// Author: Othmane

import Encryption.DecryptingSenario;
import Encryption.EncryptingSenario;
import Encryption.Senario;
import Tools.InputParameters;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Headless command-line front end, so the cipher is scriptable and fuzz/bench
 * friendly without the Swing UI. Drives the same {@link EncryptingSenario}/
 * {@link DecryptingSenario} workers the GUI uses (they run fine without a
 * display), so there is one cipher, not two.
 *
 * <pre>
 * java -cp out cli encrypt &lt;file&gt; [&lt;file&gt; ...]
 * java -cp out cli decrypt &lt;file.cr&gt; [&lt;file.cr&gt; ...]
 * </pre>
 *
 * Multiple files may be given (e.g. a drag-and-drop of a whole selection); the
 * password is asked once and applied to every file in the batch. The batch runs
 * on a {@link ForkJoinPool} sized to the CPU, so several files encrypt at once
 * on a multi-core machine. A file that fails (missing, wrong password, no free
 * space) is reported and skipped, and the run finishes the rest, exiting
 * non-zero if any file failed.
 *
 * <p>
 * A single aggregate progress bar (the mean of every file's progress) is drawn
 * to stderr while the batch runs, the same 0..100 the GUI's progress bar shows
 * for one file. Per-file results are printed together once the bar is full, so
 * they never tear the bar mid-draw.
 *
 * <p>
 * The password is always read interactively, never taken as an argument (a
 * command-line password leaks into shell history and the process list). It is
 * read without echo from the console, or from stdin when piped. Encryption asks
 * twice and requires a match, and refuses an empty password — the same policy
 * the GUI enforces.
 *
 * <p>
 * Ctrl+C during a run is the same operation as the GUI's Cancel button: a
 * shutdown hook calls {@link Senario#Cancel()} on every file being processed and
 * waits for the workers to stop and delete their partial output, so an
 * interrupted run never leaves a half-written file behind.
 *
 * @author Othmane
 */
public class cli {

    public static void main(String[] args) throws Exception {
        String usage = "usage: java -cp out cli encrypt|decrypt [-r] <file|dir> [<file|dir> ...]";
        if (args.length < 2 || (!args[0].equals("encrypt") && !args[0].equals("decrypt"))) {
            System.err.println(usage);
            System.exit(2);
        }
        boolean encrypt = args[0].equals("encrypt");
        // Optional -r/--recursive walks directory arguments into their subfolders.
        boolean recursive = args[1].equals("-r") || args[1].equals("--recursive");
        int start = recursive ? 2 : 1;
        if (start >= args.length) {
            System.err.println(usage);
            System.exit(2);
        }

        new InputParameters();
        if (InputParameters.inputParameterFileNotFound) {
            System.err.println("InputParameters file not found; run from the Cryptor directory.");
            System.exit(1);
        }

        // A directory argument expands to the files inside it (top-level only,
        // or every subfolder with -r). Encrypt takes every file but the .cr ones
        // — re-encrypting foo.cr would strip and re-add .cr, overwriting the
        // source — and decrypt takes only .cr files, skipping the rest.
        java.util.List<File> fileList = new java.util.ArrayList<>();
        for (int i = start; i < args.length; i++) {
            File f = new File(args[i]).getAbsoluteFile();  // absolute so getParent() is never null on a bare name
            if (f.isDirectory()) {
                if (recursive) {
                    try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(f.toPath())) {
                        walk.filter(java.nio.file.Files::isRegularFile)
                                .map(java.nio.file.Path::toFile)
                                .filter(c -> c.getName().endsWith(".cr") != encrypt)
                                .forEach(fileList::add);
                    }
                } else {
                    File[] inside = f.listFiles(c -> c.isFile() && c.getName().endsWith(".cr") != encrypt);
                    if (inside != null)
                        java.util.Collections.addAll(fileList, inside);
                }
            } else
                fileList.add(f);   // an explicit file is processed as given, whatever its name
        }
        File[] files = fileList.toArray(new File[0]);
        if (files.length == 0) {
            System.err.println("no files to " + args[0] + ".");
            System.exit(1);
        }

        // One password for the whole batch, read before any file is touched.
        char[] pw = readPassword("Enter password: ");
        if (encrypt) {
            if (pw.length == 0) {
                System.err.println("refusing to encrypt with an empty password.");
                System.exit(1);
            }
            char[] confirm = readPassword("Enter password again: ");
            if (!Arrays.equals(pw, confirm)) {
                System.err.println("passwords do not match.");
                System.exit(1);
            }
        }

        // The cancel hook targets every file currently being processed.
        final Set<Senario> active = ConcurrentHashMap.newKeySet();
        Thread hook = installCancelHook(active);

        Batch bar = new Batch(files.length);
        String[] results = new String[files.length];   // filled per file, printed in order at the end

        // One worker thread per CPU core, but never more than 3 files at once: each
        // file drives 3 SwingWorker threads (worker + reader + writer) and that pool
        // caps at 10, so a 4th concurrent file could starve a reader and deadlock.
        int parallelism = Math.max(1, Math.min(files.length,
                Math.min(Runtime.getRuntime().availableProcessors(), 3)));
        ForkJoinPool pool = new ForkJoinPool(parallelism);
        AtomicInteger failures = new AtomicInteger();
        Future<?>[] tasks = new Future<?>[files.length];
        for (int i = 0; i < files.length; i++) {
            final int idx = i;
            tasks[i] = pool.submit((java.util.concurrent.Callable<Void>) () -> {
                if (!processFile(files[idx], encrypt, pw, active, bar, idx, results))
                    failures.incrementAndGet();
                return null;
            });
        }
        for (Future<?> t : tasks)
            t.get();
        pool.shutdown();

        bar.render();
        System.err.println();               // close the bar line before printing results
        for (String line : results)
            System.out.println(line);

        removeHook(hook);
        if (failures.get() > 0)
            System.exit(1);
    }

    /**
     * Encrypts or decrypts one file, feeding its progress into the shared bar and
     * recording a one-line result into {@code results[idx]}.
     *
     * @return {@code true} on success, {@code false} if the file was missing,
     * had the wrong password, or ran out of free space
     */
    private static boolean processFile(File file, boolean encrypt, char[] pw, Set<Senario> active,
                                       Batch bar, int idx, String[] results) throws Exception {
        if (!file.isFile()) {
            results[idx] = "no such file: " + file;
            bar.set(idx, 100);   // count this slot as finished so the mean can still reach 100
            return false;
        }
        Senario sen = encrypt ? new EncryptingSenario(file, pw) : new DecryptingSenario(file, pw, false);
        sen.addPropertyChangeListener(e -> {
            if ("progress".equals(e.getPropertyName()))
                bar.set(idx, (int) e.getNewValue());
        });
        active.add(sen);
        try {
            sen.execute();
            sen.get();
            bar.set(idx, 100);   // the writer stops at 99; mark the slot done once the worker returns
            if (sen.isCanceled())   // Ctrl+C: the hook prints the message and cleans up
                return false;
            if (encrypt) {
                if (((EncryptingSenario) sen).NoEnoughFreeSpace()) {
                    results[idx] = "not enough free space for: " + file.getName();
                    return false;
                }
                results[idx] = "encrypted -> " + new File(file.getParent(), stripExt(file.getName()) + ".cr");
            } else {
                if (((DecryptingSenario) sen).WrongPassword()) {
                    results[idx] = "wrong password or corrupted/tampered file: " + file.getName();
                    return false;
                }
                results[idx] = "decrypted " + file.getName();
            }
            return true;
        } finally {
            active.remove(sen);
        }
    }

    /**
     * A shared, thread-safe progress bar for the whole batch: it draws the mean
     * of every file's 0..100 progress as one bar on stderr, so concurrent files
     * show as a single advancing line rather than several tearing ones.
     */
    private static final class Batch {

        private final int[] pct;

        Batch(int files) {
            this.pct = new int[files];
        }

        synchronized void set(int file, int value) {
            // Monotonic: the worker thread marks a slot 100 while a late 99 event
            // for it may still be queued on the EDT, so ignore any regression.
            if (value <= this.pct[file])
                return;
            this.pct[file] = value;
            this.render();
        }

        synchronized void render() {
            long sum = 0;
            for (int p : this.pct)
                sum += p;
            int overall = (int) (sum / this.pct.length);
            int done = 0;
            for (int p : this.pct)
                if (p >= 100)
                    done++;
            final int width = 30;
            int filled = overall * width / 100;
            StringBuilder b = new StringBuilder("\r[");
            for (int k = 0; k < width; k++)
                b.append(k < filled ? '#' : '-');
            b.append("] ").append(overall).append("%  (").append(done).append('/').append(this.pct.length).append(")   ");
            System.err.print(b);
            System.err.flush();
        }
    }

    /**
     * Registers a Ctrl+C (SIGINT) handler that cancels every file currently being
     * processed exactly as the GUI's Cancel button does, then blocks until the
     * workers have stopped and removed their partial output before the JVM exits.
     */
    private static Thread installCancelHook(final Set<Senario> active) {
        Thread hook = new Thread(() -> {
            boolean cancelledAny = false;
            for (Senario worker : active) {
                if (worker.isDone())
                    continue;             // finished normally: nothing to cancel
                worker.Cancel();
                cancelledAny = true;
                try {
                    worker.get();         // wait for the worker to delete the partial output
                } catch (Exception ignored) {
                }
            }
            if (cancelledAny)
                System.err.println("\ncancelled; partial output removed.");
        });
        Runtime.getRuntime().addShutdownHook(hook);
        return hook;
    }

    /** Drops the cancel hook once the run has finished normally. */
    private static void removeHook(Thread hook) {
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException alreadyShuttingDown) {
        }
    }

    /**
     * Reads a password from the console (no echo) or, when piped, from stdin.
     * Ctrl+C or EOF at the prompt makes the read return null (or throw); since
     * nothing has been written yet, that is a clean cancel — exit quietly rather
     * than letting a NullPointerException or stack trace reach the user.
     */
    private static char[] readPassword(String prompt) {
        char[] pw = null;
        try {
            if (System.console() != null) {
                pw = System.console().readPassword(prompt);
            } else {
                System.out.print(prompt);
                String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
                pw = (line == null) ? null : line.toCharArray();
            }
        } catch (Exception cancelledAtPrompt) {
            pw = null;
        }
        if (pw == null) {
            System.err.println("\ncancelled.");
            System.exit(130);
        }
        return pw;
    }

    /** Base name without its final extension, matching the .cr naming the worker uses. */
    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return (dot < 0) ? name : name.substring(0, dot);
    }
}
