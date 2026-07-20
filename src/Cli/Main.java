// Author: Othmane
package Cli;

import Encryption.DecryptingSenario;
import Encryption.EncryptingSenario;
import Encryption.Senario;
import Tools.InputParameters;
import java.io.File;
import java.util.Arrays;

/**
 * Headless command-line front end, so the cipher is scriptable and fuzz/bench
 * friendly without the Swing UI. Drives the same {@link EncryptingSenario}/
 * {@link DecryptingSenario} workers the GUI uses (they run fine without a
 * display), so there is one cipher, not two.
 *
 * <pre>
 * java -cp out Cli.Main encrypt [-r] [-d [-y]] &lt;file|dir&gt; [more ...]
 * java -cp out Cli.Main decrypt [-r] &lt;file.cr|dir&gt; [more ...]
 * </pre>
 *
 * This class is wiring only — parse, resolve, confirm, ask, run, report — with
 * the work in {@link Options}, {@link Batch}, {@link FileTask} and
 * {@link Prompt}. It is also the one place that calls {@code System.exit}, so
 * every exit code the CLI can produce is visible in a single method.
 *
 * <p>
 * Multiple files may be given (e.g. a drag-and-drop of a whole selection); the
 * password is asked once and applied to every file in the batch. A file that
 * fails (missing, wrong password, no free space) is reported and skipped, and
 * the run finishes the rest, exiting non-zero if any file failed.
 *
 * <p>
 * Ctrl+C during a run is the same operation as the GUI's Cancel button: a
 * shutdown hook calls {@link Senario#Cancel()} on every file being processed and
 * waits for the workers to stop and delete their partial output, so an
 * interrupted run never leaves a half-written file behind.
 *
 * @author Othmane
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Options options = null;
        try {
            options = Options.parse(args);
        } catch (UsageException bad) {
            if (!bad.getMessage().isEmpty())
                System.err.println(bad.getMessage());
            System.err.println(Options.USAGE);
            System.exit(2);
        }

        new InputParameters();
        if (InputParameters.inputParameterFileNotFound) {
            System.err.println("InputParameters file not found; run from the Cryptor directory.");
            System.exit(1);
        }

        File[] files = options.resolveFiles();
        if (files.length == 0) {
            System.err.println("no files to " + options.verb() + ".");
            System.exit(1);
        }

        // Destructive: warn once for the whole batch before anything is touched,
        // unless -y was given. A no at the prompt aborts before the password.
        if (options.deleteOriginal && !options.assumeYes) {
            System.err.println("WARNING: -d will permanently delete " + files.length
                    + " original file(s) after encrypting them (securely overwritten, not"
                    + " recoverable by undelete tools). This cannot be undone.");
            if (!new Prompt("Proceed? [y/N] ").confirm()) {
                System.err.println("aborted.");
                System.exit(1);
            }
        }

        char[] password = askPassword(options);
        FileTask[] done = new Batch(files, options, password).run();

        int failures = 0;
        for (FileTask task : done) {
            System.out.println(task.result());
            if (!task.succeeded())
                failures++;
        }
        if (failures > 0)
            System.exit(1);
    }

    /**
     * Reads the one password for the whole batch, before any file is touched.
     * Encryption asks twice and requires a match, and refuses an empty password
     * — the same policy the GUI enforces. Cancelling at the prompt exits 130,
     * the conventional code for "killed by SIGINT", with nothing written.
     */
    private static char[] askPassword(Options options) {
        char[] password = read("Enter password: ");
        if (options.encrypting()) {
            if (password.length == 0) {
                System.err.println("refusing to encrypt with an empty password.");
                System.exit(1);
            }
            if (!Arrays.equals(password, read("Enter password again: "))) {
                System.err.println("passwords do not match.");
                System.exit(1);
            }
        }
        return password;
    }

    private static char[] read(String prompt) {
        char[] password = new Prompt(prompt).readPassword();
        if (password == null) {
            System.err.println("\ncancelled.");
            System.exit(130);
        }
        return password;
    }
}
