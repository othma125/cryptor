// Author: Othmane

import Encryption.DecryptingSenario;
import Encryption.EncryptingSenario;
import Tools.InputParameters;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

/**
 * Throughput benchmark: encrypt and decrypt MB/s across a range of file sizes.
 * The cipher is O(n) with an O(1) per-byte constant, so this measures those
 * constants rather than any change in order. Decryption reads the file twice
 * (MAC pass + decode pass), so its MB/s is expected to trail encryption.
 *
 * <p>
 * Run from the {@code Cryptor} directory (so {@code InputParameters} is found):
 * <pre>javac -d out src/*.java src/Encryption/*.java src/Tools/*.java test/*.java
 * java -cp out BenchmarkTest</pre>
 */
public class BenchmarkTest {

    private static final char[] PW = "password".toCharArray();
    private static final int[] SIZES = {1 << 20, 4 << 20, 16 << 20};   // 1, 4, 16 MB

    public static void main(String[] args) throws Exception {
        new InputParameters();
        if (InputParameters.inputParameterFileNotFound)
            throw new IllegalStateException("Run from the Cryptor directory: InputParameters file not found");

        System.out.printf("%-8s %14s %14s%n", "size", "encrypt MB/s", "decrypt MB/s");
        for (int size : SIZES)
            benchmark(size);
    }

    private static void benchmark(int size) throws Exception {
        File dir = Files.createTempDirectory("cryptor-bench").toFile();
        byte[] plaintext = new byte[size];
        new Random(size).nextBytes(plaintext);
        File plain = new File(dir, "bench.dat");
        Files.write(plain.toPath(), plaintext);

        byte[] salt = new byte[InputParameters.saltLength];
        Arrays.fill(salt, (byte) 0x2A);

        // one warm-up round-trip that also serves as the correctness check
        File cr = new File(dir, "bench.dat.cr");
        double encMbps = timeEncrypt(plain, salt, size);
        double decMbps = timeDecrypt(cr, size);
        byte[] roundTripped = Files.readAllBytes(new File(dir, "bench.dat").toPath());
        assert Arrays.equals(plaintext, roundTripped) : "round-trip mismatch at size " + size;

        System.out.printf("%-8s %14.1f %14.1f%n", human(size), encMbps, decMbps);
        deleteRecursively(dir);
    }

    private static double timeEncrypt(File plain, byte[] salt, int size) throws Exception {
        long t0 = System.nanoTime();
        EncryptingSenario enc = new EncryptingSenario(plain, PW, salt);
        enc.execute();
        enc.get();
        return mbps(size, System.nanoTime() - t0);
    }

    private static double timeDecrypt(File cr, int size) throws Exception {
        long t0 = System.nanoTime();
        DecryptingSenario dec = new DecryptingSenario(cr, PW, false);
        dec.execute();
        dec.get();
        if (dec.WrongPassword())
            throw new IllegalStateException("decrypt failed at size " + size);
        return mbps(size, System.nanoTime() - t0);
    }

    private static double mbps(int bytes, long nanos) {
        return (bytes / (1024.0 * 1024.0)) / (nanos / 1e9);
    }

    private static String human(int bytes) {
        return (bytes >> 20) + "MB";
    }

    private static void deleteRecursively(File f) {
        File[] kids = f.listFiles();
        if (kids != null)
            for (File k : kids)
                deleteRecursively(k);
        f.delete();
    }
}
