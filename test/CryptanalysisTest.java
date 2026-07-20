// Author: Othmane

import Encryption.EncryptingSenario;
import Encryption.Order;
import Tools.InputParameters;
import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Attacks the cipher with a couple of standard "is it broken?" cryptanalysis
 * smoke tests and prints a verdict. This is the empirical counterpart to the
 * README Design analysis:
 *
 * <ol>
 *   <li><b>Avalanche / diffusion</b> — flip one <em>early</em> plaintext bit and
 *       measure how much of the ciphertext changes. A cipher with real forward
 *       diffusion changes ~50% every time; a self-heal near 0% is a fail. The
 *       flip is kept early so the test is fair to forward-only chaining.</li>
 *   <li><b>Constant-input periodicity</b> — encrypt an all-zero file under each
 *       of several fixed keys and look at entropy / exact period. A real cipher
 *       yields ~8 bits/byte and no period for every key; chaining that XORs
 *       after the substitution instead of inside it collapses to period 2 on
 *       constant input, for the fraction of keys that pack it to a constant.</li>
 *   <li><b>Randomization</b> — with a per-file salt, the same file + password
 *       must now produce different bytes each time (precomputation dies). Before
 *       the salt this failed; it should now pass.</li>
 *   <li><b>Bit avalanche</b> — [1] counts changed bytes, which saturates near
 *       97%; this counts changed <em>bits</em>, which must sit at ~50% on every
 *       trial, and catches ciphers that [1] waves through.</li>
 *   <li><b>Key avalanche</b> — a 1-bit password change must rewrite ~50% of the
 *       output bits: related keys must not give related ciphertext.</li>
 *   <li><b>Uniformity</b> — NIST-style monobit plus a byte chi-square, catching
 *       the bias and non-uniformity that entropy is too coarse to see.</li>
 *   <li><b>Serial correlation</b> — adjacent ciphertext bytes must be
 *       uncorrelated; this is where a leaky chain shows up, and it is invisible
 *       to the order-blind statistics above.</li>
 * </ol>
 *
 * A random-plaintext baseline calibrates that the metrics read ~8 bits/byte
 * when there is no structure to leak. Note the entropy ceiling is sample-size
 * bound: 8192 bytes over 256 bins tops out near 7.98, not 8.00.
 *
 * Run from the {@code Cryptor} directory (so {@code InputParameters} is found):
 * <pre>javac -d out src/*.java src/Encryption/*.java src/Tools/*.java test/*.java
 * java -ea -cp out CryptanalysisTest</pre>
 */
public class CryptanalysisTest {

    private static final char[] PW = "password".toCharArray();

    public static void main(String[] args) throws Exception {
        new InputParameters();
        if (InputParameters.inputParameterFileNotFound)
            throw new IllegalStateException("Run from the Cryptor directory: InputParameters file not found");

        boolean broken = false;
        broken |= avalanche();
        broken |= constantInput();
        broken |= determinism();
        broken |= bitAvalanche();
        broken |= keyAvalanche();
        broken |= uniformity();
        broken |= serialCorrelation();

        System.out.println();
        System.out.println(broken
                ? "VERDICT: BROKEN by standard cryptanalysis (matches README Design analysis)."
                : "VERDICT: survived these tests.");

        // The statistical battery above answers "does it look random?". These
        // three answer "what does a real attack recover, and how much does the
        // key cost to guess?" — the "learn cryptography" attacks from
        // ALGORITHM_DIRECTIONS.md. They print measurements, not a BROKEN verdict.
        System.out.println();
        System.out.println("=== Attacks & key-cost estimates (ALGORITHM_DIRECTIONS.md) ===");
        keyspace16();
        kdfCost();
        knownPlaintextGridRecovery();
    }

    /**
     * Flip one <em>early</em> plaintext bit (first {@code FLIP_ZONE} bytes) and
     * measure the fraction of the whole ciphertext that changes. Flipping early
     * keeps the test fair to a forward-chaining cipher: almost the entire file is
     * downstream of the change, so a cipher with real diffusion must alter ~50%
     * of the output. A late flip would legitimately change little even in AES-CBC
     * (chaining only propagates forward), so we do not test that.
     */
    private static boolean avalanche() throws Exception {
        int trials = 30, size = 2048, flipZone = 32;
        Random rnd = new Random(1);
        byte[] salt = new byte[InputParameters.saltLength];   // fixed salt: measure plaintext diffusion under one key
        Arrays.fill(salt, (byte) 0x2A);
        double sum = 0, min = 1, max = 0;
        int local = 0, phaseShift = 0;   // local heal (<5% changed) vs length-changing shift
        for (int t = 0; t < trials; t++) {
            byte[] base = new byte[size];
            rnd.nextBytes(base);
            byte[] variant = base.clone();
            int bit = rnd.nextInt(flipZone * 8);   // early flip: whole file is downstream
            variant[bit >> 3] ^= (1 << (bit & 7));

            byte[] a = encrypt(base, salt), b = encrypt(variant, salt);
            double frac = diffFraction(a, b);
            sum += frac;
            min = Math.min(min, frac);
            max = Math.max(max, frac);
            if (frac < 0.05)
                local++;
            if (a.length != b.length)
                phaseShift++;
        }
        double mean = sum / trials;
        System.out.println("[1] Avalanche (1-bit flip in first " + flipZone + "B, " + trials + " trials, " + size + "B each)");
        System.out.printf ("    ciphertext changed: mean %.1f%%  min %.1f%%  max %.1f%%%n", mean * 100, min * 100, max * 100);
        System.out.println("    trials that self-healed (<5% changed): " + local + "/" + trials + "   |   trials that shifted length: " + phaseShift + "/" + trials);
        // A secure cipher never self-heals on an early flip: it avalanches to ~50%.
        boolean broken = local > 0 || min < 0.40;
        System.out.println("    => " + (broken ? "BROKEN: diffusion is bounded/self-healing, not total." : "ok: every flip avalanched."));
        return broken;
    }

    /**
     * Encrypt a large constant file; a periodic / low-entropy result leaks structure.
     *
     * <p>
     * This sweeps {@code KEYS} <em>fixed</em> salts rather than encrypting once
     * under a random one, because this leak is key-dependent: the pre-counter
     * chaining collapsed to a 2-byte period for ~38% of keys and looked perfectly
     * healthy for the rest. Sampling one random key per run therefore turned a
     * consistent break into a coin-flip verdict, and a single lucky run is how
     * "~7.95 bits/byte, no period" was once recorded in the README for a cipher
     * that was in fact broken. Fixed salts also make the verdict reproducible.
     */
    private static boolean constantInput() throws Exception {
        int keys = 8, size = 8192;

        byte[] rndPlain = new byte[size];
        new Random(2).nextBytes(rndPlain);
        double baseline = entropy(encrypt(rndPlain));  // calibration: no structure to leak

        int collapsed = 0, worstPeriod = -1;
        double worst = 9;
        for (int k = 0; k < keys; k++) {
            byte[] salt = new byte[InputParameters.saltLength];
            Arrays.fill(salt, (byte) k);               // one distinct, fixed key per iteration
            byte[] c = encrypt(new byte[size], salt);  // all 0x00
            double e = entropy(c);
            int p = period(c, 128);                    // skip header transient
            if (p > 0 || e < 7.0) {
                collapsed++;
                if (p > 0 && worstPeriod < 0)
                    worstPeriod = p;
            }
            worst = Math.min(worst, e);
        }
        System.out.println();
        System.out.println("[2] Constant input (" + size + " bytes of 0x00, swept over " + keys + " fixed keys)");
        System.out.printf ("    worst-case entropy: %.2f bits/byte  (random-plaintext baseline: %.2f)%n", worst, baseline);
        System.out.println("    keys that leaked (period or entropy < 7.0): " + collapsed + "/" + keys
                + (worstPeriod > 0 ? "   |   exact period found: " + worstPeriod + " bytes" : "   |   no period at any key"));
        boolean broken = collapsed > 0;
        System.out.println("    => " + (broken ? "BROKEN: constant plaintext produces periodic/low-entropy ciphertext for some keys." : "ok: no periodicity, full entropy, every key."));
        return broken;
    }

    /** Same file + same password twice → identical ciphertext means precomputation is possible. */
    private static boolean determinism() throws Exception {
        byte[] p = new byte[4096];
        new Random(3).nextBytes(p);
        boolean deterministic = Arrays.equals(encrypt(p), encrypt(p));
        System.out.println();
        System.out.println("[3] Randomization (per-file salt in header)");
        System.out.println("    same file + password → identical ciphertext: " + deterministic);
        System.out.println("    => " + (deterministic ? "BROKEN: precomputation possible; two encryptions of a file are bit-identical." : "ok: randomized per encryption."));
        return deterministic;
    }

    /**
     * Strict-avalanche-style check at <em>bit</em> granularity. Test [1] counts
     * changed <em>bytes</em>, which saturates: flipping ~50% of bits changes
     * ~99.6% of bytes, so a byte count near 97% cannot distinguish a good cipher
     * from a mediocre one. The real criterion is that each output bit flips with
     * probability ~1/2, i.e. ~50% of bits differ — significantly more or less
     * both indicate structure.
     */
    private static boolean bitAvalanche() throws Exception {
        int trials = 30, size = 2048, flipZone = 32;
        Random rnd = new Random(11);
        byte[] salt = new byte[InputParameters.saltLength];
        Arrays.fill(salt, (byte) 0x2A);
        double sum = 0, min = 1, max = 0;
        for (int t = 0; t < trials; t++) {
            byte[] base = new byte[size];
            rnd.nextBytes(base);
            byte[] variant = base.clone();
            int bit = rnd.nextInt(flipZone * 8);
            variant[bit >> 3] ^= (1 << (bit & 7));
            double frac = bitDiffFraction(encrypt(base, salt), encrypt(variant, salt));
            sum += frac;
            min = Math.min(min, frac);
            max = Math.max(max, frac);
        }
        double mean = sum / trials;
        System.out.println();
        System.out.println("[4] Bit avalanche (1-bit plaintext flip, " + trials + " trials, ideal 50%)");
        System.out.printf ("    ciphertext bits changed: mean %.1f%%  min %.1f%%  max %.1f%%%n", mean * 100, min * 100, max * 100);
        // Judge per trial, not just on the mean: a cipher that swings between 25%
        // and 85% averages to a healthy-looking 50% while being plainly broken.
        // For ~16k ciphertext bits the per-trial sd is ~0.4%, so a spread this
        // wide is structure, not sampling noise.
        boolean broken = mean < 0.45 || mean > 0.55 || min < 0.45 || max > 0.55;
        System.out.println("    => " + (broken ? "BROKEN: bit diffusion is not consistently ~50%; some flips barely move the output." : "ok: ~half the output bits flip, every trial."));
        return broken;
    }

    /**
     * Key avalanche: same plaintext and salt, passwords differing by one bit.
     * A cipher must decorrelate its key just as thoroughly as its plaintext —
     * related-key structure is what lets an attacker walk the keyspace instead
     * of searching it. PBKDF2 should make the two master keys unrelated, so this
     * mostly checks that the derived permutations really depend on the whole key.
     */
    private static boolean keyAvalanche() throws Exception {
        int trials = 16, size = 2048;
        Random rnd = new Random(12);
        byte[] salt = new byte[InputParameters.saltLength];
        Arrays.fill(salt, (byte) 0x2A);   // fixed salt: isolate the password's effect
        double sum = 0, min = 1, max = 0;
        for (int t = 0; t < trials; t++) {
            byte[] p = new byte[size];
            rnd.nextBytes(p);
            char[] pw = "password".toCharArray();
            char[] pw2 = pw.clone();
            int bit = rnd.nextInt(7);              // flip one bit of one character
            pw2[rnd.nextInt(pw2.length)] ^= (1 << bit);
            double frac = bitDiffFraction(encrypt(p, salt, pw), encrypt(p, salt, pw2));
            sum += frac;
            min = Math.min(min, frac);
            max = Math.max(max, frac);
        }
        double mean = sum / trials;
        System.out.println();
        System.out.println("[5] Key avalanche (1-bit password flip, same plaintext+salt, " + trials + " trials, ideal 50%)");
        System.out.printf ("    ciphertext bits changed: mean %.1f%%  min %.1f%%  max %.1f%%%n", mean * 100, min * 100, max * 100);
        boolean broken = mean < 0.45 || mean > 0.55;
        System.out.println("    => " + (broken ? "BROKEN: related keys produce related ciphertext." : "ok: a 1-bit key change rewrites the output."));
        return broken;
    }

    /**
     * NIST-style monobit plus a byte-distribution chi-square on the ciphertext of
     * random plaintext. Entropy (test [2]) is a coarse statistic — it is blind to
     * ordering and forgiving of mild bias — so these check the two things it
     * misses cheaply: that 0 and 1 bits are balanced, and that the 256 byte
     * values are uniform rather than merely varied. For 256 bins, chi-square is
     * ~255 ± 22.6 when uniform; the bound below is ~4 sigma.
     */
    private static boolean uniformity() throws Exception {
        int size = 1 << 16;
        byte[] p = new byte[size];
        new Random(13).nextBytes(p);
        byte[] c = encrypt(p);

        long ones = 0;
        for (byte x : c)
            ones += Integer.bitCount(x & 0xff);
        long bits = (long) c.length * 8;
        double prop = (double) ones / bits;
        double sd = 0.5 / Math.sqrt(bits);          // sd of the proportion under H0
        double sigma = Math.abs(prop - 0.5) / sd;

        int[] f = new int[256];
        for (byte x : c) f[x & 0xff]++;
        double expected = (double) c.length / 256, chi2 = 0;
        for (int o : f)
            chi2 += (o - expected) * (o - expected) / expected;

        System.out.println();
        System.out.println("[6] Uniformity of ciphertext (" + size + "B random plaintext)");
        System.out.printf ("    monobit: %.4f ones (ideal 0.5000, %.1f sigma)%n", prop, sigma);
        System.out.printf ("    byte chi-square: %.1f over 255 df (uniform ~255 +/- 22.6)%n", chi2);
        boolean broken = sigma > 4 || chi2 > 350 || chi2 < 160;
        System.out.println("    => " + (broken ? "BROKEN: ciphertext is measurably non-uniform." : "ok: bits balanced and bytes uniform."));
        return broken;
    }

    /**
     * Serial correlation between consecutive ciphertext bytes. Chaining schemes
     * fail exactly here when the chained value leaks: c_i built from c_(i-1) by a
     * weak map leaves a linear trace even when the byte histogram looks flat, so
     * this catches what entropy and chi-square (both order-blind) cannot.
     */
    private static boolean serialCorrelation() throws Exception {
        int size = 1 << 16;
        byte[] p = new byte[size];
        new Random(14).nextBytes(p);
        byte[] c = encrypt(p);

        double r = correlation(c);
        System.out.println();
        System.out.println("[7] Serial correlation of adjacent ciphertext bytes (" + size + "B random plaintext)");
        System.out.printf ("    coefficient: %+.4f (ideal 0.0000)%n", r);
        boolean broken = Math.abs(r) > 0.02;
        System.out.println("    => " + (broken ? "BROKEN: adjacent bytes are correlated; the chain leaks." : "ok: adjacent bytes are uncorrelated."));
        return broken;
    }

    // --- attacks & key-cost estimates ---

    // The grid transform is a fixed, compiled-in constant: an attacker knows it
    // exactly (README "the grid stage contributes no key-dependent mixing"). So
    // these two tables are not secrets — they are public knowledge reused to
    // enumerate the 16-step keyspace and to drive the known-plaintext attack.
    private static final short[] DEFAULT16 = {0, 4, 5, 12, 1, 2, 9, 13, 8, 6, 15, 14, 3, 7, 11, 10};
    private static final int[][] SECRET_MATRIX = {null,
            {1, 4}, {2, 3, 5, 8, 12}, {3, 5, 8, 12}, null, {5, 8, 12}, {6, 7, 9, 13},
            {7, 9, 13}, {8, 12}, {9, 13}, {10, 11, 14, 15}, {11, 14, 15}, null, null, {14, 15}};

    /**
     * Effective keyspace of the 16-step grid permutation. Each password picks,
     * independently, one option from each non-null {@code secretMatrix} row, so
     * the reachable orders number the product of the row sizes — not 16!. This is
     * a structural ceiling of the cipher: a stronger KDF cannot lift it, because
     * it caps how many distinct grid orders exist at all.
     */
    private static boolean keyspace16() {
        long space = 1;
        for (int[] row : SECRET_MATRIX)
            if (row != null)
                space *= row.length;
        System.out.println();
        System.out.println("[A] 16-step grid keyspace (structural, KDF cannot lift it)");
        System.out.printf ("    reachable grid orders: %,d  (~2^%.1f, vs 16! = 2^44.3)%n",
                space, Math.log(space) / Math.log(2));
        System.out.println("    => a whole secret layer lives in a ~17-bit space; brute-forceable in the [C] attack.");
        return false;
    }

    /**
     * Cost of guessing the password through the KDF. Times real
     * {@link Order#getPassword} derivations and extrapolates to a couple of
     * guessing budgets. Single-core, one implementation: a GPU/ASIC rig is orders
     * of magnitude faster, so treat these as generous upper bounds on the
     * attacker's cost, i.e. the floor of what a *weak* password buys.
     */
    private static boolean kdfCost() {
        int trials = 40;
        byte[] salt = new byte[InputParameters.saltLength];
        Arrays.fill(salt, (byte) 0x2A);
        char[] pw = "password".toCharArray();
        Order.getPassword(pw, salt);   // warm up JIT / provider init
        long t0 = System.nanoTime();
        for (int i = 0; i < trials; i++)
            Order.getPassword(pw, salt);
        double perGuess = (System.nanoTime() - t0) / 1e9 / trials;
        double rate = 1 / perGuess;

        double lower8 = Math.pow(26, 8);          // 8-char lowercase
        double dict = 1e7;                          // a modest wordlist
        System.out.println();
        System.out.println("[B] KDF guessing cost (PBKDF2-HMAC-SHA256, 100k iters, single core)");
        System.out.printf ("    %.1f ms/guess  ->  %,.0f guesses/sec on this core%n", perGuess * 1000, rate);
        System.out.printf ("    8-char lowercase (26^8): %s   |   10M-word dictionary: %s%n",
                duration(lower8 / rate), duration(dict / rate));
        System.out.println("    => only sets the cost for a *weak* password; a GPU rig cuts these by ~10^3-10^6.");
        return false;
    }

    /**
     * Known-plaintext recovery of the 16-step grid order — the attack sketched in
     * the README "small effective keyspace" note, run for real. The attacker knows
     * a plaintext and its ciphertext but not the key. The grid packing depends only
     * on the (public) fixed tables and the ~2^17 grid order, so every candidate
     * order is enumerated and each is tested for internal consistency: within a
     * 64-byte rekey block the map {@code idx = V ^ prevCipher ^ ctr} to the observed
     * ciphertext byte must be a partial permutation (injective both ways, since the
     * per-block substitution is a bijection). A wrong grid order produces the wrong
     * packed bytes {@code V} and collides within a block; the true order never does.
     *
     * <p>
     * This peels off a whole secret layer without touching the master key or the
     * per-block 256-tables (each is fresh every 64 bytes, so 64 samples is too thin
     * to invert) — exactly the limited-but-real break the README predicts. Its own
     * success is the correctness check: if the packing model diverged from the
     * cipher, the true order would fail its own consistency test and the run would
     * report "true order recovered: NO".
     */
    private static boolean knownPlaintextGridRecovery() throws Exception {
        int nblocks = 5, testLen = nblocks * 64;

        // Known (plaintext, ciphertext) pair under a fixed salt.
        byte[] salt = new byte[InputParameters.saltLength];
        Arrays.fill(salt, (byte) 0x37);
        byte[] fileBytes = new byte[1024];
        new Random(99).nextBytes(fileBytes);

        File dir = Files.createTempDirectory("cryptor-kpa").toFile();
        File plain = new File(dir, "kpa.dat");
        Files.write(plain.toPath(), fileBytes);
        EncryptingSenario enc = new EncryptingSenario(plain, PW, salt);
        enc.execute();
        enc.get();
        byte[] cr = Files.readAllBytes(new File(dir, "kpa.dat.cr").toPath());
        deleteRecursively(dir);
        int[] body = new int[cr.length - InputParameters.saltLength - InputParameters.tagLength];
        for (int i = 0; i < body.length; i++)
            body[i] = cr[InputParameters.saltLength + i] & 0xff;

        // The bytes the packer consumes: header (name length, name chars as
        // little-endian pairs, terminator) then the file bytes — all known to an
        // attacker who knows the file name and content. Mirrors EncryptingSenario.
        List<Integer> streamList = new ArrayList<>();
        String name = plain.getName();
        streamList.add(name.length());
        for (int k = 0; k < name.length(); k++) {
            streamList.add(name.charAt(k) % 256);
            streamList.add(name.charAt(k) / 256);
        }
        streamList.add((int) InputParameters.endFileNameCharacter);
        for (byte b : fileBytes)
            streamList.add(b & 0xff);
        int[] stream = streamList.stream().mapToInt(Integer::intValue).toArray();

        short[] trueOrder = new Order(InputParameters._16, Order.getPassword(PW, salt)).getOrder();

        // Enumerate every reachable grid order (odometer over the secretMatrix rows).
        List<Integer> nonNull = new ArrayList<>();
        for (int i = 0; i < SECRET_MATRIX.length; i++)
            if (SECRET_MATRIX[i] != null)
                nonNull.add(i);
        int[] choice = new int[nonNull.size()];
        GridSim sim = new GridSim(body, testLen, nblocks);
        long total = 0, survivors = 0;
        boolean trueSurvived = false;
        do {
            short[] order = buildOrder(nonNull, choice);
            total++;
            if (sim.survives(order, stream)) {
                survivors++;
                if (Arrays.equals(order, trueOrder))
                    trueSurvived = true;
            }
        } while (advance(choice, nonNull));

        System.out.println();
        System.out.println("[C] Known-plaintext grid-order recovery (" + testLen + " known ciphertext bytes)");
        System.out.printf ("    %,d candidate grid orders  ->  %,d consistent with the known plaintext%n", total, survivors);
        System.out.println("    true order recovered: " + (trueSurvived ? "YES" : "NO")
                + "   (survivors narrow further with more known text)");
        System.out.println("    => the grid layer is not key-strength; only the master key + per-block tables are.");
        assert trueSurvived : "KPA model diverged from the cipher: true grid order failed its own consistency test";
        return false;
    }

    /** Builds a length-16 order from one odometer position, mirroring Order.getOrder. */
    private static short[] buildOrder(List<Integer> nonNull, int[] choice) {
        short[] aux = new short[15];
        for (int c = 0; c < nonNull.size(); c++) {
            int i = nonNull.get(c);
            aux[i] = (short) (SECRET_MATRIX[i][choice[c]] - i);
        }
        short[] order = DEFAULT16.clone();
        for (int i = 0; i < aux.length; i++) {
            int j = i + aux[i];
            if (j < order.length && i != j) {
                short t = order[i]; order[i] = order[j]; order[j] = t;
            }
        }
        return order;
    }

    /** Advances the mixed-radix odometer; returns false once it wraps (all combos seen). */
    private static boolean advance(int[] choice, List<Integer> nonNull) {
        for (int c = choice.length - 1; c >= 0; c--) {
            if (++choice[c] < SECRET_MATRIX[nonNull.get(c)].length)
                return true;
            choice[c] = 0;
        }
        return false;
    }

    /**
     * Re-runs the cipher's grid packing for a candidate order and tests each
     * emitted byte for chaining consistency against the known ciphertext. Reused
     * across all candidates (reset per run) to avoid per-candidate allocation.
     */
    private static final class GridSim {
        private final int[] body, fwd, rev;   // fwd/rev: per-block idx<->ciphertext maps
        private final int testLen, nblocks;
        private short[] order;
        private int previous, zeroCounter, index, outputByte, emitted;
        private boolean broken;

        GridSim(int[] body, int testLen, int nblocks) {
            this.body = body;
            this.testLen = testLen;
            this.nblocks = nblocks;
            this.fwd = new int[nblocks * 256];
            this.rev = new int[nblocks * 256];
        }

        boolean survives(short[] order, int[] stream) {
            this.order = order;
            previous = InputParameters.n; zeroCounter = 0; index = 0; outputByte = 0; emitted = 0; broken = false;
            Arrays.fill(fwd, -1);
            Arrays.fill(rev, -1);
            for (int v : stream) {
                decodeByte(v);
                if (broken)
                    return false;
                if (emitted >= testLen)
                    return true;
            }
            return emitted >= testLen;
        }

        private void decodeByte(int value) {
            for (int i = 0; i < 8 && !broken && emitted < testLen; i++) {
                if (((value >> (7 - i)) & 1) == 1) {
                    if (previous < InputParameters.n) { emit(previous * 4 + zeroCounter); previous = InputParameters.n; }
                    else previous = zeroCounter;
                    zeroCounter = 0;
                } else {
                    zeroCounter++;
                    if (zeroCounter == InputParameters.m) {
                        if (previous < InputParameters.n) { emit(previous * 4 + zeroCounter); previous = InputParameters.n; }
                        else previous = zeroCounter;
                        zeroCounter = 0;
                    }
                }
            }
        }

        private void emit(int matrixIndex) {
            int k = order[matrixIndex], X = k / 4, Y = k % 4;   // pairs[order[matrixIndex]]
            for (int j = 0; j < X; j++) packBit(false);
            if (X < InputParameters.m) packBit(true);
            for (int j = 0; j < Y; j++) packBit(false);
            if (Y < InputParameters.m) packBit(true);
        }

        private void packBit(boolean setBit) {
            if (setBit) outputByte += (1 << (7 - index));
            index++;
            if (index == 8) { checkByte(outputByte); index = 0; outputByte = 0; }
        }

        private void checkByte(int V) {
            int i = emitted;
            if (i < testLen) {
                int prevC = (i == 0) ? 0 : body[i - 1];
                int idx = (V ^ prevC ^ (i & 0xff)) & 0xff;
                int c = body[i];
                int base = (i / 64) * 256;
                if (fwd[base + idx] == -1 && rev[base + c] == -1) {
                    fwd[base + idx] = c; rev[base + c] = idx;
                } else if (fwd[base + idx] != c || rev[base + c] != idx) {
                    broken = true;
                }
            }
            emitted++;
        }
    }

    /** Rough human-readable duration for a number of seconds. */
    private static String duration(double seconds) {
        double[] cuts = {1, 60, 3600, 86400, 31557600.0};
        String[] units = {"sec", "min", "hours", "days", "years"};
        int u = 0;
        for (int i = 0; i < cuts.length; i++)
            if (seconds >= cuts[i])
                u = i;
        return String.format("%,.1f %s", seconds / cuts[u], units[u]);
    }

    // --- metrics ---

    /** Pearson correlation of c[i] against c[i+1]. */
    private static double correlation(byte[] d) {
        int n = d.length - 1;
        double sx = 0, sy = 0, sxy = 0, sxx = 0, syy = 0;
        for (int i = 0; i < n; i++) {
            double x = d[i] & 0xff, y = d[i + 1] & 0xff;
            sx += x; sy += y; sxy += x * y; sxx += x * x; syy += y * y;
        }
        double cov = n * sxy - sx * sy;
        double den = Math.sqrt(n * sxx - sx * sx) * Math.sqrt(n * syy - sy * sy);
        return den == 0 ? 0 : cov / den;
    }

    /** Fraction of differing bits over the aligned region (length diffs counted as all-differing). */
    private static double bitDiffFraction(byte[] a, byte[] b) {
        int m = Math.min(a.length, b.length);
        long diff = 0;
        for (int i = 0; i < m; i++)
            diff += Integer.bitCount((a[i] ^ b[i]) & 0xff);
        diff += (long) Math.abs(a.length - b.length) * 8;
        return (double) diff / (Math.max(a.length, b.length) * 8L);
    }

    /** Differing bytes over the aligned region plus any length difference, / max length. */
    private static double diffFraction(byte[] a, byte[] b) {
        int m = Math.min(a.length, b.length), d = Math.abs(a.length - b.length);
        for (int i = 0; i < m; i++)
            if (a[i] != b[i])
                d++;
        return (double) d / Math.max(a.length, b.length);
    }

    private static double entropy(byte[] d) {
        int[] f = new int[256];
        for (byte x : d) f[x & 0xff]++;
        double e = 0;
        for (int c : f)
            if (c > 0)  {
                double p = (double) c / d.length;
                e -= p * (Math.log(p) / Math.log(2));
            }
        return e;
    }

    /** Smallest period 1..1024 that matches >98% of the tail (after skip), or -1. */
    private static int period(byte[] d, int skip) {
        int n = d.length;
        for (int p = 1; p <= 1024 && skip + p < n; p++) {
            int match = 0, total = 0;
            for (int i = skip; i + p < n; i++) {
                total++;
                if (d[i] == d[i + p])
                    match++;
            }
            if (total > 0 && (double) match / total > 0.98)
                return p;
        }
        return -1;
    }

    // --- cipher driver (same pattern as RoundTripTest) ---

    /** Encrypt with a fresh random salt (the normal path). */
    private static byte[] encrypt(byte[] plaintext) throws Exception {
        return run(plaintext, new EncryptingSenarioFactory() {
            public EncryptingSenario make(File f) { return new EncryptingSenario(f, PW); }
        });
    }

    /** Encrypt with a fixed salt, so two calls share one key (for diffusion measurement). */
    private static byte[] encrypt(byte[] plaintext, final byte[] salt) throws Exception {
        return run(plaintext, new EncryptingSenarioFactory() {
            public EncryptingSenario make(File f) { return new EncryptingSenario(f, PW, salt); }
        });
    }

    /** Encrypt with a fixed salt and an explicit password (for key-avalanche measurement). */
    private static byte[] encrypt(byte[] plaintext, final byte[] salt, final char[] pw) throws Exception {
        return run(plaintext, new EncryptingSenarioFactory() {
            public EncryptingSenario make(File f) { return new EncryptingSenario(f, pw, salt); }
        });
    }

    private interface EncryptingSenarioFactory { EncryptingSenario make(File plain); }

    private static byte[] run(byte[] plaintext, EncryptingSenarioFactory factory) throws Exception {
        File dir = Files.createTempDirectory("cryptor-ca").toFile();
        File plain = new File(dir, "plain.dat");
        Files.write(plain.toPath(), plaintext);
        EncryptingSenario enc = factory.make(plain);
        enc.execute();
        enc.get();
        byte[] cipher = Files.readAllBytes(new File(dir, "plain.dat.cr").toPath());
        deleteRecursively(dir);
        return cipher;
    }

    private static void deleteRecursively(File f) {
        File[] kids = f.listFiles();
        if (kids != null)
            for (File k : kids)
                deleteRecursively(k);
        f.delete();
    }
}
