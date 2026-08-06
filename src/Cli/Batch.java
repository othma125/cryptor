// Author: Othmane
package Cli;

import Tools.BatchRun;
import java.io.File;

/**
 * The CLI's front end onto {@link BatchRun}: draws the shared progress bar on
 * stderr, installs the Ctrl+C hook that cancels whatever is still in flight, and
 * turns each outcome into the one line the CLI prints for it.
 *
 * <p>
 * The pool, the progress aggregation and the cancellation all live in
 * {@link BatchRun}, which the GUI drives too; what stays here is only the part
 * that is specific to a terminal.
 *
 * @author Othmane
 */
final class Batch {

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
     * returns the outcomes in the order the files were given.
     */
    BatchRun.Result[] run() throws Exception {
        ProgressBar bar = new ProgressBar();
        BatchRun batch = new BatchRun(this.files, this.options.encrypting(),
                this.options.deleteOriginal, false, this.password, bar::render);
        Thread hook = this.installCancelHook(batch);

        BatchRun.Result[] results = batch.run();

        System.err.println();   // close the bar line before the caller prints results
        this.removeHook(hook);
        return results;
    }

    /**
     * The one line the CLI prints for a finished file.
     *
     * @param r one file's outcome
     * @param options the run's options, which decide whether a wipe was asked for
     * @return the report line, without a trailing newline
     */
    static String describe(BatchRun.Result r, Options options) {
        switch (r.status) {
            case MISSING:
                return "no such file: " + r.file;
            case CANCELLED:
                return "cancelled: " + r.file.getName();
            case NO_SPACE:
                return "not enough free space for: " + r.file.getName();
            case WRONG_PASSWORD:
                return "wrong password or corrupted/tampered file: " + r.file.getName();
            default:
                break;
        }
        if (!options.encrypting())
            return "decrypted " + r.file.getName();
        String line = "encrypted -> " + r.output;
        if (options.deleteOriginal)   // the worker wiped the plaintext once the .cr was safely written
            line += r.deleteError == null ? "  (original deleted)"
                    : "  (could not delete original: " + r.deleteError.getMessage() + ")";
        return line;
    }

    /**
     * Registers a Ctrl+C (SIGINT) handler that cancels the batch exactly as the
     * GUI's Cancel button does, then blocks until the workers have stopped and
     * removed their partial output before the JVM exits.
     */
    private Thread installCancelHook(final BatchRun batch) {
        Thread hook = new Thread(() -> {
            if (batch.cancel())
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
