// Author: Othmane
package Cli;

import Encryption.Senario;
import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

/**
 * Runs a whole batch of files: owns the thread pool, the shared progress bar,
 * and the Ctrl+C hook that cancels whatever is still in flight.
 *
 * @author Othmane
 */
final class Batch {

    /**
     * One worker thread per CPU core, but never more than 3 files at once: each
     * file drives 3 SwingWorker threads (worker + reader + writer) and that pool
     * caps at 10, so a 4th concurrent file could starve a reader and deadlock.
     */
    private static final int MAX_CONCURRENT_FILES = 3;

    private final File[] files;
    private final Options options;
    private final char[] password;

    Batch(File[] files, Options options, char[] password) {
        this.files = files;
        this.options = options;
        this.password = password;
    }

    /**
     * Encrypts or decrypts every file, drawing one aggregate progress bar, and
     * returns the finished tasks in the order they were given so the caller can
     * print results that don't depend on which thread won.
     */
    FileTask[] run() throws Exception {
        Set<Senario> active = ConcurrentHashMap.newKeySet();   // the cancel hook's targets
        Thread hook = this.installCancelHook(active);

        ProgressBar bar = new ProgressBar(this.files.length);
        FileTask[] tasks = new FileTask[this.files.length];
        for (int i = 0; i < this.files.length; i++)
            tasks[i] = new FileTask(this.files[i], this.options, this.password, active, bar, i);

        int parallelism = Math.max(1, Math.min(this.files.length,
                Math.min(Runtime.getRuntime().availableProcessors(), MAX_CONCURRENT_FILES)));
        ForkJoinPool pool = new ForkJoinPool(parallelism);
        Future<?>[] running = new Future<?>[tasks.length];
        for (int i = 0; i < tasks.length; i++)
            running[i] = pool.submit(tasks[i]);
        for (Future<?> t : running)
            t.get();
        pool.shutdown();

        bar.render();
        System.err.println();   // close the bar line before the caller prints results
        this.removeHook(hook);
        return tasks;
    }

    /**
     * Registers a Ctrl+C (SIGINT) handler that cancels every file currently being
     * processed exactly as the GUI's Cancel button does, then blocks until the
     * workers have stopped and removed their partial output before the JVM exits.
     */
    private Thread installCancelHook(final Set<Senario> active) {
        Thread hook = new Thread(() -> {
            boolean cancelledAny = false;
            for (Senario worker : active) {
                if (worker.isDone())
                    continue;             // finished normally: nothing to cancel
                worker.Cancel();
                cancelledAny = true;
                try {
                    worker.get();         // wait for the worker to delete the partial output
                } catch (InterruptedException | ExecutionException ignored) {}
            }
            if (cancelledAny)
                System.err.println("\ncancelled; partial output removed.");
        });
        Runtime.getRuntime().addShutdownHook(hook);
        return hook;
    }

    /** Drops the cancel hook once the run has finished normally. */
    private void removeHook(Thread hook) {
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException alreadyShuttingDown) {}
    }
}
