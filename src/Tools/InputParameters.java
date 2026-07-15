package Tools;

import Encryption.Order;
import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.Scanner;
import java.util.stream.IntStream;

/**
 * Global cipher configuration and precomputed lookup tables, loaded once at
 * startup from the {@code InputParameters} file in the working directory. Holds
 * the bit-coding table, powers of two, division scales, the 16 grid points and
 * the default order used to seed permutations. If the file is missing,
 * {@link #inputParameterFileNotFound} is set and the GUI blocks
 * encryption/decryption.
 *
 * <p>
 * ponytail: these are public mutable static fields (global state). Fine for a
 * single-window app; would need encapsulation only if this ever ran
 * multi-instance.
 *
 * @author Othmane
 */
public class InputParameters {

    public static final byte n = 4;
    public static final byte m = 3;
    public static final byte _8 = 8;
    public static final byte _16 = 16;
    public static final short _256 = 256;
    public static final short maxLength = 1024;
    public static final int saltLength = 16;
    /** Length of the HMAC-SHA256 tag appended to a {@code .cr} file (direction 5). */
    public static final int tagLength = 32;
    public static final byte[][] indexMatrix = {{0, 1, 2, 3},
                                                {4, 5, 6, 7},
                                                {8, 9, 10, 11},
                                                {12, 13, 14, 15}};
    public static boolean inputParameterFileNotFound = false;
    public static short endFileNameCharacter = (short) '\n';
    public static boolean[][] binaryCoding = new boolean[InputParameters._256][InputParameters._8];
    public static int[] Power_2;
    public static BigInteger[][] Scales;
    public static BigInteger[] maxScales = new BigInteger[2];
    public static Pair[] pairs;
    public static BigInteger defaultCode;
    public static Order defaultOrder;

    /**
     * Grid coordinate for index {@code k} on the {@code n}-wide grid
     * ({@code X=k/n}, {@code Y=k%n}).
     */
    public class Pair {

        public byte X, Y;

        public Pair(int k) {
            this.X = (byte) (k / InputParameters.n);
            this.Y = (byte) (k % InputParameters.n);
        }
    }

    /**
     * Loads the parameter file and builds all lookup tables. On a missing file,
     * sets {@link #inputParameterFileNotFound} and returns without throwing.
     *
     * @throws InterruptedException if interrupted while waiting for the
     * default-order thread
     */
    public InputParameters() throws InterruptedException {
        InputParameters.pairs = IntStream.range(0, InputParameters._16)
                                            .mapToObj(Pair::new)
                                            .toArray(Pair[]::new);
        InputParameters.Power_2 = IntStream.range(0, InputParameters._8)
                                            .map(i -> (int) Math.pow(2, InputParameters._8 - 1 - i))
                                            .toArray();
        InputParameters.Scales = new BigInteger[2][];
        InputParameters.Scales[0] = new BigInteger[11];
        InputParameters.Scales[1] = new BigInteger[255];
        InputParameters.Scales[0][0] = InputParameters.Scales[1][0] = BigInteger.ONE;
        Scanner scanner;
        try {
            scanner = new Scanner(new File("InputParameters"));
        } catch (FileNotFoundException e) {
            InputParameters.inputParameterFileNotFound = true;
            return;
        }
        InputParameters.endFileNameCharacter = Short.valueOf(scanner.nextLine());
        IntStream.range(1, InputParameters.Scales[0].length)
                .forEach(j -> InputParameters.Scales[0][j] = new BigInteger(scanner.nextLine().trim()));
        InputParameters.maxScales[0] = new BigInteger(scanner.nextLine().trim());
        IntStream.range(1, InputParameters.Scales[1].length)
                .forEach(j -> InputParameters.Scales[1][j] = new BigInteger(scanner.nextLine().trim()));
        InputParameters.defaultCode = new BigInteger(scanner.nextLine().trim());
        Thread thread = new Thread(() -> InputParameters.defaultOrder = new Order());
        thread.start();
        for (short i = 0; i < InputParameters._256; i++) {
            String s = Integer.toBinaryString(i);
            int k = s.length() - 1;
            for (byte j = InputParameters._8 - 1;; j--) {
                InputParameters.binaryCoding[i][j] = s.charAt(k) == '1';
                if (--k == -1) 
                    break;
            }
        }
        InputParameters.maxScales[1] = new BigInteger(scanner.nextLine().trim());
        scanner.close();
        thread.join();
    }
}
