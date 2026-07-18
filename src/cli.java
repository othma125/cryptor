// Author: Othmane

import Encryption.DecryptingSenario;
import Encryption.EncryptingSenario;
import Encryption.Senario;
import Tools.InputParameters;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;

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
 * password is asked once and applied to every file in the batch. A file that
 * fails (missing, wrong password, no free space) is reported and skipped, and
 * the run finishes the rest, exiting non-zero if any file failed.
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
 * shutdown hook calls {@link Senario#Cancel()} on the file currently being
 * processed and waits for the worker to stop and delete its partial output, so
 * an interrupted run never leaves a half-written file behind.
 *
 * @author Othmane
 */
public class cli {

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || (!args[0].equals("encrypt") && !args[0].equals("decrypt"))) {
            System.err.println("usage: java -cp out cli encrypt|decrypt <file> [<file> ...]");
            System.exit(2);
        }
        boolean encrypt = args[0].equals("encrypt");

        new InputParameters();
        if (InputParameters.inputParameterFileNotFound) {
            System.err.println("InputParameters file not found; run from the Cryptor directory.");
            System.exit(1);
        }

        File[] files = new File[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            files[i - 1] = new File(args[i]).getAbsoluteFile();  // absolute so getParent() is never null on a bare name
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

        // The cancel hook always targets whichever file is being processed right now.
        final Senario[] current = new Senario[1];
        Thread hook = installCancelHook(current);

        int failures = 0;
        for (File file : files) {
            if (!file.isFile()) {
                System.err.println("no such file: " + file);
                failures++;
                continue;
            }
            if (encrypt) {
                EncryptingSenario enc = new EncryptingSenario(file, pw);
                current[0] = enc;
                enc.execute();
                enc.get();
                if (enc.isCanceled())   // Ctrl+C: the hook prints the message and cleans up
                    return;
                if (enc.NoEnoughFreeSpace()) {
                    System.err.println("not enough free space for: " + file.getName());
                    failures++;
                    continue;
                }
                System.out.println("encrypted -> " + new File(file.getParent(), stripExt(file.getName()) + ".cr"));
            } else {
                DecryptingSenario dec = new DecryptingSenario(file, pw, false);
                current[0] = dec;
                dec.execute();
                dec.get();
                if (dec.isCanceled())
                    return;
                if (dec.WrongPassword()) {
                    System.err.println("wrong password or corrupted/tampered file: " + file.getName());
                    failures++;
                    continue;
                }
                System.out.println("decrypted " + file.getName());
            }
        }

        removeHook(hook);
        if (failures > 0)
            System.exit(1);
    }

    /**
     * Registers a Ctrl+C (SIGINT) handler that cancels the file currently being
     * processed exactly as the GUI's Cancel button does, then blocks until the
     * worker has stopped and removed its partial output before the JVM exits.
     */
    private static Thread installCancelHook(final Senario[] current) {
        Thread hook = new Thread(() -> {
            Senario worker = current[0];
            if (worker == null || worker.isDone())
                return;                       // between files, or finished normally: nothing to cancel
            worker.Cancel();
            try {
                worker.get();                 // wait for the worker to delete the partial output
            } catch (Exception ignored) {
            }
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
