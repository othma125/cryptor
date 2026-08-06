// Author: Othmane

package Tools;

import Encryption.DecryptingSenario;
import Encryption.EncryptingSenario;
import Encryption.Senario;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

/**
 * Runs a batch of files through the encryption workers in parallel, for whichever
 * front end asked for it. Owns the thread pool, the aggregate progress and the
 * cancellation; it renders nothing and pops no dialogs, so the CLI can draw a bar
 * on stderr and the GUI can drive a {@code JProgressBar} from the same run.
 *
 * <p>
 * The split matters: deciding <em>what happened</em> to a file (wrong password,
 * no space, cancelled) is identical in both front ends, while deciding <em>what
 * to say about it</em> is not. This class does the first and hands back a
 * {@link Result} per file, in input order, for the caller to phrase.
 *
 * @author Othmane
 */
public final class BatchRun {

    /**
     * One worker thread per CPU core, but never more than 3 files at once: each
     * file drives 3 SwingWorker threads (worker + reader + writer) and that pool
     * caps at 10, so a 4th concurrent file could starve a reader and deadlock.
     */
    private static final int MAX_CONCURRENT_FILES = 3;

    /**
     * Aggregate progress sink. Called from worker threads, so a UI implementation
     * must hop to its own event thread.
     */
    public interface Progress {
        void update(int overallPercent, int filesDone, int filesTotal);
    }

    /** What became of one file. */
    public enum Status { OK, WRONG_PASSWORD, NO_SPACE, CANCELLED, MISSING }

    /** One file's outcome, carrying the extras only encryption produces. */
    public static final class Result {

        public final File file;
        public final Status status;
        /** The {@code .cr} actually written; null unless encryption succeeded. */
        public final File output;
        /** Set when encryption succeeded but the requested wipe of the original did not. */
        public final IOException deleteError;

        Result(File file, Status status, File output, IOException deleteError) {
            this.file = file;
            this.status = status;
            this.output = output;
            this.deleteError = deleteError;
        }

        public boolean succeeded() {
            return this.status == Status.OK;
        }
    }

    private final File[] files;
    private final boolean encrypting;
    private final boolean deleteOriginal;
    private final boolean openFile;
    private final char[] password;
    private final Progress progress;

    private final Set<Senario> active = ConcurrentHashMap.newKeySet();
    private final int[] percent;
    private volatile boolean cancelled = false;

    /**
     * @param files files to work on, in the order results come back
     * @param encrypting {@code true} to encrypt, {@code false} to decrypt
     * @param deleteOriginal securely wipe each source once its {@code .cr} is written (encryption only)
     * @param openFile open each file after decrypting it (decryption only)
     * @param password the password for the whole batch
     * @param progress aggregate progress sink, called from worker threads
     */
    public BatchRun(File[] files, boolean encrypting, boolean deleteOriginal, boolean openFile,
            char[] password, Progress progress) {
        this.files = files;
        this.encrypting = encrypting;
        this.deleteOriginal = deleteOriginal;
        this.openFile = openFile;
        this.password = password;
        this.progress = progress;
        this.percent = new int[files.length];
    }

    /**
     * Works through every file and returns the outcomes in the order the files
     * were given, so a caller can report results that don't depend on which
     * thread won.
     *
     * <p>
     * Blocks until the batch is done. A UI caller must therefore run this off its
     * event thread — and specifically <em>not</em> on a {@code SwingWorker}: three
     * concurrent files already hold nine of that pool's ten threads, so borrowing
     * the tenth to wait on them invites the very starvation
     * {@link #MAX_CONCURRENT_FILES} exists to prevent.
     *
     * @return one {@link Result} per input file, in input order
     * @throws Exception if a worker failed outright rather than reporting an outcome
     */
    public Result[] run() throws Exception {
        Result[] results = new Result[this.files.length];
        int parallelism = Math.max(1, Math.min(this.files.length,
                Math.min(Runtime.getRuntime().availableProcessors(), MAX_CONCURRENT_FILES)));
        ForkJoinPool pool = new ForkJoinPool(parallelism);
        Future<?>[] running = new Future<?>[this.files.length];
        for (int i = 0; i < this.files.length; i++) {
            final int slot = i;
            running[i] = pool.submit((Callable<Void>) () -> {
                results[slot] = this.process(slot);
                return null;
            });
        }
        for (Future<?> f : running)
            f.get();
        pool.shutdown();
        return results;
    }

    /**
     * Cancels the batch: every file in flight stops and removes its partial
     * output, and every file not yet started is skipped. Blocks until the running
     * workers have finished cleaning up, so the caller can trust that no partial
     * {@code .cr} outlives the call.
     *
     * @return {@code true} if anything was actually still running
     */
    public boolean cancel() {
        this.cancelled = true;   // stops files that have not been picked up yet
        boolean cancelledAny = false;
        for (Senario worker : this.active) {
            if (worker.isDone())
                continue;        // finished normally: nothing to cancel
            worker.Cancel();
            cancelledAny = true;
            try {
                worker.get();    // wait for the worker to delete the partial output
            } catch (InterruptedException | ExecutionException ignored) {}
        }
        return cancelledAny;
    }

    private Result process(int slot) throws Exception {
        File file = this.files[slot];
        if (this.cancelled)
            return this.finish(slot, new Result(file, Status.CANCELLED, null, null));
        if (!file.isFile())
            return this.finish(slot, new Result(file, Status.MISSING, null, null));

        Senario sen = this.encrypting
                ? new EncryptingSenario(file, this.password)
                : new DecryptingSenario(file, this.password, this.openFile);
        if (this.encrypting && this.deleteOriginal)
            ((EncryptingSenario) sen).deleteOriginal = true;   // wiped by the worker once the .cr is written
        sen.addPropertyChangeListener(e -> {
            if ("progress".equals(e.getPropertyName()))
                this.set(slot, (int) e.getNewValue());
        });
        this.active.add(sen);
        try {
            sen.execute();
            sen.get();
            if (sen.isCanceled())
                return this.finish(slot, new Result(file, Status.CANCELLED, null, null));
            if (this.encrypting) {
                EncryptingSenario enc = (EncryptingSenario) sen;
                if (enc.NoEnoughFreeSpace())
                    return this.finish(slot, new Result(file, Status.NO_SPACE, null, null));
                // ask the run for the name: it may carry a (n) collision suffix
                return this.finish(slot, new Result(file, Status.OK, enc.OutputFile(), enc.deleteError));
            }
            DecryptingSenario dec = (DecryptingSenario) sen;
            // Before the space check, not after: a rejected file never got as far
            // as opening an output, so there is no writer to ask about space.
            if (dec.WrongPassword())
                return this.finish(slot, new Result(file, Status.WRONG_PASSWORD, null, null));
            if (dec.NoEnoughFreeSpace())
                return this.finish(slot, new Result(file, Status.NO_SPACE, null, null));
            return this.finish(slot, new Result(file, Status.OK, null, null));
        } finally {
            this.active.remove(sen);
        }
    }

    /**
     * Marks a slot finished and returns its result. The writer stops at 99, and a
     * skipped file never reports at all, so the slot is forced to 100 either way —
     * otherwise the batch mean could never reach it.
     */
    private Result finish(int slot, Result result) {
        this.set(slot, 100);
        return result;
    }

    /**
     * Records one file's progress and reports the batch mean. Monotonic per slot:
     * a worker thread marks its slot 100 while a late 99 event for it may still be
     * queued on the EDT, so any regression is ignored.
     */
    private synchronized void set(int slot, int value) {
        if (value <= this.percent[slot])
            return;
        this.percent[slot] = value;
        long sum = 0;
        int done = 0;
        for (int p : this.percent) {
            sum += p;
            if (p >= 100)
                done++;
        }
        this.progress.update((int) (sum / this.percent.length), done, this.percent.length);
    }
}
