import Encryption.DecryptingSenario;
import Encryption.EncryptingSenario;
import Tools.InputParameters;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

/**
 * Standalone encrypt -&gt; decrypt round-trip check after the {@code BigInteger} swap.
 * Run from the {@code Cryptor} directory (so the {@code InputParameters} file is found):
 *
 * <pre>javac -d out $(find src test -name '*.java') &amp;&amp; java -ea -cp out RoundTripTest</pre>
 *
 * Encrypts a random file, decrypts the result, and asserts the bytes match the original.
 * Covers several sizes across the 1024-byte block boundary and a few passwords, including
 * the long password that would overflow {@code long} but not {@code BigInteger}.
 */
public class RoundTripTest {
    public static void main(String[] args) throws Exception {
        new InputParameters();
        if (InputParameters.inputParameterFileNotFound)
            throw new IllegalStateException("Run from the Cryptor directory: InputParameters file not found");

        int[] sizes = {1, 15, 100, 1023, 1024, 1025, 5000, 40000};
        char[][] passwords = {
            "password".toCharArray(),
            "a".toCharArray(),
            "correct horse battery staple 0123456789 correct horse battery staple".toCharArray(),
            new char[0]
        };

        int cases = 0, failures = 0;
        for (char[] pw : passwords)
            for (int size : sizes) {
                cases++;
                try {
                    roundTrip(size, pw);
                } catch (AssertionError e) {
                    failures++;
                    System.out.println("FAIL " + e.getMessage());
                }
            }

        cases++;
        try {
            cancelMidFlight();
        } catch (AssertionError e) {
            failures++;
            System.out.println("FAIL " + e.getMessage());
        }

        cases++;
        try {
            encryptionIsRandomized();
        } catch (AssertionError e) {
            failures++;
            System.out.println("FAIL " + e.getMessage());
        }

        cases++;
        try {
            tamperIsDetected();
        } catch (AssertionError e) {
            failures++;
            System.out.println("FAIL " + e.getMessage());
        }
        System.out.println((failures == 0 ? "OK: " : "FAILURES: " + failures + "/")
                + cases + " round-trips.");
    }

    private static void roundTrip(int size, char[] password) throws Exception {
        File dir = Files.createTempDirectory("cryptor-rt").toFile();
        File plain = new File(dir, "plain.dat");
        byte[] original = new byte[size];
        new Random(size * 31L + password.length).nextBytes(original);
        Files.write(plain.toPath(), original);

        EncryptingSenario enc = new EncryptingSenario(plain, password);
        enc.execute();
        enc.get();
        File encrypted = new File(dir, "plain.cr");
        assertTrue(encrypted.isFile(), "encrypted file missing for size " + size);

        // wrong password must be detected, never reproduce the original
        char[] wrong = "ZZZQq9-wrong".toCharArray();
        DecryptingSenario bad = new DecryptingSenario(encrypted, wrong, false);
        bad.execute();
        bad.get();
        assertTrue(bad.WrongPassword() || !plainMatches(plain, original),
                "WRONG PASSWORD NOT DETECTED size=" + size + " pwlen=" + password.length);

        // right password must reproduce the original byte for byte
        DecryptingSenario dec = new DecryptingSenario(encrypted, password, false);
        dec.execute();
        dec.get();
        byte[] decrypted = Files.readAllBytes(plain.toPath());
        assertTrue(Arrays.equals(original, decrypted),
                "MISMATCH size=" + size + " pwlen=" + password.length
                        + " (got " + decrypted.length + " bytes)");

        deleteRecursively(dir);
    }

    /**
     * Cancels a large encryption mid-flight and asserts the worker returns
     * cleanly and the partial output file is deleted (no locked/leftover file).
     */
    private static void cancelMidFlight() throws Exception {
        File dir = Files.createTempDirectory("cryptor-cancel").toFile();
        File plain = new File(dir, "big.dat");
        byte[] original = new byte[20 * 1024 * 1024];
        new Random(7).nextBytes(original);
        Files.write(plain.toPath(), original);

        EncryptingSenario enc = new EncryptingSenario(plain, "pw".toCharArray());
        enc.execute();
        enc.Cancel();       // volatile flag: the worker loop must observe it and stop
        enc.get();          // must return without throwing
        assertTrue(!new File(dir, "big.cr").exists(), "cancelled output file not deleted");

        deleteRecursively(dir);
    }

    /**
     * Encryption must be randomized: the per-file salt means the same plaintext
     * under the same password produces different ciphertext every time (which is
     * what kills the old deterministic-output weakness). Decryption still
     * recovers the original because the salt is stored in the header — the
     * round-trip cases above already prove that.
     */
    private static void encryptionIsRandomized() throws Exception {
        byte[] plaintext = new byte[4096];
        new Random(1234).nextBytes(plaintext);

        byte[] a1 = encryptWith(plaintext, "alpha".toCharArray());
        byte[] a2 = encryptWith(plaintext, "alpha".toCharArray());

        assertTrue(!Arrays.equals(a1, a2), "same file+password produced identical ciphertext (salt not applied)");
    }

    /**
     * The MAC must reject a tampered file (direction 5). Flips one bit in the
     * body of a .cr under the *right* password: without a MAC this decodes to
     * wrong plaintext silently, since the old header check only ever looked at
     * the first few bytes. Nothing may be written, because verification happens
     * before the header is decoded at all.
     */
    private static void tamperIsDetected() throws Exception {
        File dir = Files.createTempDirectory("cryptor-tamper").toFile();
        File plain = new File(dir, "plain.dat");
        byte[] original = new byte[2000];
        new Random(99).nextBytes(original);
        Files.write(plain.toPath(), original);

        char[] pw = "pw".toCharArray();
        EncryptingSenario enc = new EncryptingSenario(plain, pw);
        enc.execute();
        enc.get();

        File encrypted = new File(dir, "plain.cr");
        byte[] cr = Files.readAllBytes(encrypted.toPath());
        cr[cr.length / 2] ^= 0x01;
        Files.write(encrypted.toPath(), cr);

        assertTrue(plain.delete(), "could not clear the plaintext before decrypting");
        DecryptingSenario dec = new DecryptingSenario(encrypted, pw, false);
        dec.execute();
        dec.get();
        assertTrue(dec.WrongPassword(), "TAMPERED FILE NOT DETECTED (right password, one flipped bit)");
        assertTrue(!plain.isFile(), "tampered file still produced output");

        deleteRecursively(dir);
    }

    /** Encrypts {@code plaintext} under {@code password} and returns the .cr bytes. */
    private static byte[] encryptWith(byte[] plaintext, char[] password) throws Exception {
        File dir = Files.createTempDirectory("cryptor-pw").toFile();
        File plain = new File(dir, "plain.dat");
        Files.write(plain.toPath(), plaintext);

        EncryptingSenario enc = new EncryptingSenario(plain, password);
        enc.execute();
        enc.get();
        byte[] cipher = Files.readAllBytes(new File(dir, "plain.cr").toPath());

        deleteRecursively(dir);
        return cipher;
    }

    private static boolean plainMatches(File plain, byte[] original) throws Exception {
        return plain.isFile() && Arrays.equals(Files.readAllBytes(plain.toPath()), original);
    }

    private static void deleteRecursively(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRecursively(k);
        f.delete();
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }
}
