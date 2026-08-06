// Author: Othmane

import Tools.BatchRun;
import Tools.InputParameters;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Checks the batch engine the GUI and the CLI now share: that a folder's worth of
 * files survives an encrypt → decrypt round trip run in parallel, that each file's
 * outcome comes back in input order, and that cancelling leaves nothing behind.
 *
 * <p>
 * Run from the {@code Cryptor} directory (so {@code InputParameters} is found):
 * <pre>./test.sh BatchRunTest</pre>
 */
public class BatchRunTest {

    private static final char[] PW = "batch-pw".toCharArray();

    public static void main(String[] args) throws Exception {
        new InputParameters();
        if (InputParameters.inputParameterFileNotFound)
            throw new IllegalStateException("Run from the Cryptor directory: InputParameters file not found");

        int failures = 0;
        failures += run("roundTrip", BatchRunTest::roundTrip);
        failures += run("wrongPassword", BatchRunTest::wrongPassword);
        failures += run("missingFile", BatchRunTest::missingFile);
        failures += run("cancelLeavesNothing", BatchRunTest::cancelLeavesNothing);
        System.out.println(failures == 0 ? "OK: 4 batch cases." : "FAILURES: " + failures + "/4");
    }

    private interface Case {
        void run() throws Exception;
    }

    private static int run(String name, Case c) {
        try {
            c.run();
            return 0;
        } catch (AssertionError e) {
            System.out.println("FAIL " + name + ": " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.out.println("ERROR " + name + ": " + e);
            return 1;
        }
    }

    /**
     * Six files through one batch and back. Also pins the two things the progress
     * contract promises: the aggregate reaches 100% with every file counted done,
     * and results come back in the order the files were given.
     */
    private static void roundTrip() throws Exception {
        File dir = Files.createTempDirectory("batch-rt").toFile();
        int n = 6;
        File[] plain = new File[n];
        byte[][] original = new byte[n][];
        for (int i = 0; i < n; i++) {
            original[i] = new byte[500 + i * 4000];
            new Random(i).nextBytes(original[i]);
            plain[i] = new File(dir, "f" + i + ".dat");
            Files.write(plain[i].toPath(), original[i]);
        }

        int[] last = new int[3];
        BatchRun.Result[] enc = new BatchRun(plain, true, false, false, PW,
                (overall, done, total) -> { last[0] = overall; last[1] = done; last[2] = total; }).run();

        assertTrue(enc.length == n, "expected " + n + " results, got " + enc.length);
        for (int i = 0; i < n; i++) {
            assertTrue(enc[i].succeeded(), "file " + i + " reported " + enc[i].status);
            assertTrue(enc[i].file.equals(plain[i]), "results came back out of order at " + i);
            assertTrue(enc[i].output != null && enc[i].output.isFile(), "no .cr written for file " + i);
        }
        assertTrue(last[0] == 100 && last[1] == n && last[2] == n,
                "progress ended at " + last[0] + "% (" + last[1] + "/" + last[2] + ")");

        File[] crs = new File[n];
        for (int i = 0; i < n; i++) {
            crs[i] = enc[i].output;
            assertTrue(plain[i].delete(), "could not clear plaintext " + i);
        }
        BatchRun.Result[] dec = new BatchRun(crs, false, false, false, PW, (o, d, t) -> {}).run();
        for (int i = 0; i < n; i++) {
            assertTrue(dec[i].succeeded(), "decrypt of file " + i + " reported " + dec[i].status);
            assertTrue(Arrays.equals(original[i], Files.readAllBytes(plain[i].toPath())),
                    "MISMATCH on file " + i);
        }
        deleteRecursively(dir);
    }

    /** A wrong password must be reported per file, not thrown, and write nothing. */
    private static void wrongPassword() throws Exception {
        File dir = Files.createTempDirectory("batch-pw").toFile();
        File[] plain = {new File(dir, "a.dat"), new File(dir, "b.dat")};
        for (File f : plain)
            Files.write(f.toPath(), new byte[3000]);

        BatchRun.Result[] enc = new BatchRun(plain, true, false, false, PW, (o, d, t) -> {}).run();
        File[] crs = {enc[0].output, enc[1].output};
        for (File f : plain)
            assertTrue(f.delete(), "could not clear " + f);

        BatchRun.Result[] dec = new BatchRun(crs, false, false, false,
                "not-the-password".toCharArray(), (o, d, t) -> {}).run();
        for (int i = 0; i < 2; i++) {
            assertTrue(dec[i].status == BatchRun.Status.WRONG_PASSWORD,
                    "expected WRONG_PASSWORD, got " + dec[i].status);
            assertTrue(!plain[i].isFile(), "a rejected file still produced output");
        }
        deleteRecursively(dir);
    }

    /** A file that vanished between the pick and the run is reported, not fatal. */
    private static void missingFile() throws Exception {
        File dir = Files.createTempDirectory("batch-missing").toFile();
        File real = new File(dir, "real.dat");
        Files.write(real.toPath(), new byte[2000]);
        File[] files = {new File(dir, "ghost.dat"), real};

        BatchRun.Result[] r = new BatchRun(files, true, false, false, PW, (o, d, t) -> {}).run();
        assertTrue(r[0].status == BatchRun.Status.MISSING, "expected MISSING, got " + r[0].status);
        assertTrue(r[1].succeeded(), "the real file should still have been encrypted");
        deleteRecursively(dir);
    }

    /**
     * Cancelling mid-batch must stop the files in flight, skip the ones not yet
     * started, and leave no partial {@code .cr} behind — the guarantee the GUI's
     * Cancel button and the CLI's Ctrl+C both rest on.
     */
    private static void cancelLeavesNothing() throws Exception {
        File dir = Files.createTempDirectory("batch-cancel").toFile();
        int n = 6;
        File[] plain = new File[n];
        for (int i = 0; i < n; i++) {
            plain[i] = new File(dir, "big" + i + ".dat");
            byte[] bytes = new byte[4 * 1024 * 1024];
            new Random(i).nextBytes(bytes);
            Files.write(plain[i].toPath(), bytes);
        }

        AtomicInteger seen = new AtomicInteger();
        BatchRun[] holder = new BatchRun[1];
        holder[0] = new BatchRun(plain, true, false, false, PW, (overall, done, total) -> {
            // cancel as soon as the batch is demonstrably under way
            if (seen.incrementAndGet() == 1)
                new Thread(holder[0]::cancel).start();
        });
        BatchRun.Result[] r = holder[0].run();

        int cancelled = 0;
        for (BatchRun.Result one : r)
            if (one.status == BatchRun.Status.CANCELLED)
                cancelled++;
        assertTrue(cancelled > 0, "nothing was cancelled; the batch outran the cancel");
        for (BatchRun.Result one : r)
            if (one.status == BatchRun.Status.CANCELLED)
                assertTrue(!new File(dir, one.file.getName().replace(".dat", ".cr")).exists(),
                        "cancelled file left a partial .cr: " + one.file.getName());
        deleteRecursively(dir);
    }

    private static void deleteRecursively(File f) {
        File[] kids = f.listFiles();
        if (kids != null)
            for (File k : kids)
                deleteRecursively(k);
        f.delete();
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond)
            throw new AssertionError(msg);
    }
}
