package Encryption;

import Tools.ExchangeMove;
import Tools.InputParameters;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.stream.IntStream;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A password-derived permutation of {@code 0..length-1}, used as one of the two
 * substitution steps of the cipher. The password number selects, via successive
 * division against precomputed scales, a sequence of {@link ExchangeMove}
 * swaps applied to a base order. An empty password leaves the order undefined
 * (null).
 *
 * @author Othmane
 */
public class Order {

    private static final short[] defaultOrder = {0, 4, 5, 12, 1, 2, 9, 13, 8, 6, 15, 14, 3, 7, 11, 10};
    private static final byte[][] secretMatrix = {null,
                                                    {1, 4},
                                                    {2, 3, 5, 8, 12},
                                                    {3, 5, 8, 12},
                                                    null,
                                                    {5, 8, 12},
                                                    {6, 7, 9, 13},
                                                    {7, 9, 13},
                                                    {8, 12},
                                                    {9, 13},
                                                    {10, 11, 14, 15},
                                                    {11, 14, 15},
                                                    null,
                                                    null,
                                                    {14, 15}};
    /**
     * Per-position swap offsets derived from the password; null for an empty
     * password.
     */
    public short[] auxilaryArray = null;
    /**
     * Size of the permutation (16 or 256).
     */
    public short length;

    /**
     * Builds a permutation of length {@code length} (16 or 256) from the given
     * password.
     *
     * @param length permutation size, {@code 16} or {@code 256}
     * @param passwordCode password encoded as a number
     */
    public Order(short length, BigInteger passwordCode) {
        this.length = length;
        this.auxilaryArray = new short[this.length - 1];
        if (passwordCode.signum() != 0) {
            if (this.length == InputParameters._16) {
                BigInteger x = passwordCode.mod(InputParameters.maxScales[0]);
                byte l = (byte) (InputParameters.Scales[0].length - 1);
                for (short i = (short) (this.auxilaryArray.length - 1); i > -1; i--) {
                    if (Order.secretMatrix[i] == null) 
                        continue;
                    BigInteger[] qr = x.divideAndRemainder(InputParameters.Scales[0][l]);
                    this.auxilaryArray[i] = (short) (Order.secretMatrix[i][qr[0].intValueExact()] - i);
                    x = qr[1];
                    l--;
                }
            }
            else {
                BigInteger x = passwordCode.mod(InputParameters.maxScales[1]);
                for (int i = this.auxilaryArray.length - 1; i > -1; i--) {
                    BigInteger[] qr = x.divideAndRemainder(InputParameters.Scales[1][i]);
                    this.auxilaryArray[i] = (short) qr[0].intValueExact();
                    x = qr[1];
                }
            }
        }
    }

    /**
     * Builds the default length-256 permutation from
     * {@link InputParameters#defaultCode}, used to seed the 256-order before
     * the password order is applied.
     */
    public Order() {
        this.length = InputParameters._256;
        if (InputParameters.defaultCode.signum() != 0) {
            this.auxilaryArray = new short[this.length - 1];
            BigInteger x = InputParameters.defaultCode;
            for (int i = this.auxilaryArray.length - 1; i > -1; i--) {
                BigInteger[] qr = x.divideAndRemainder(InputParameters.Scales[1][i]);
                this.auxilaryArray[i] = (short) qr[0].intValueExact();
                x = qr[1];
            }
        }
    }

    /**
     * Derives the key number from the password and a per-file salt using
     * PBKDF2 (HMAC-SHA256, 100k iterations). Replaces the old raw base-256
     * encoding: stretching makes brute-force/dictionary attacks expensive, the
     * salt makes the key per-file (killing precomputation), and the uniform
     * 256-bit digest removes the "different passwords collide mod maxScales"
     * bias. The result seeds both the 16- and 256-element orders.
     *
     * @param password password characters (an empty password is stretched too)
     * @param salt per-file random salt read from / written to the {@code .cr} header
     * @return a uniform 256-bit key as a {@link BigInteger}
     */
    public static BigInteger getPassword(char[] password, byte[] salt) {
        // PBEKeySpec rejects an empty password; a placeholder keeps blank
        // passwords working (still per-file unique via the salt).
        char[] pw = (password.length == 0) ? new char[]{'\0'} : password;
        try {
            PBEKeySpec spec = new PBEKeySpec(pw, salt, 100_000, 256);
            byte[] dk = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            return new BigInteger(1, dk);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Derives a per-block sub-key from the master key number for rekeying the
     * byte substitution (direction 4): {@code SHA-256(master || block)}. Cheap
     * (one hash), so it runs per block without re-stretching the password. Both
     * directions derive the same value from the same master + block index.
     *
     * @param master the master key number from {@link #getPassword}
     * @param block the block index (epoch) to derive a key for
     * @return a fresh key number for that block
     */
    public static BigInteger subKey(BigInteger master, long block) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(master.toByteArray());
            for (int i = 56; i >= 0; i -= 8)
                md.update((byte) (block >>> i));
            return new BigInteger(1, md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Builds the HMAC-SHA256 instance used for encrypt-then-MAC (direction 5),
     * keyed by a MAC key derived from the master: {@code SHA-256(master || "mac")}.
     * Hashing the master (already a uniform 256-bit secret) separates the MAC key
     * from the substitution keys without a second PBKDF2 stretch. The {@code "mac"}
     * suffix is 3 bytes where {@link #subKey} always appends 8, so the two
     * derivations can never collide on an input.
     *
     * @param master the master key number from {@link #getPassword}
     * @return an initialised {@code HmacSHA256}, ready to {@code update}
     */
    public static Mac mac(BigInteger master) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(master.toByteArray());
            md.update("mac".getBytes(StandardCharsets.US_ASCII));
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(md.digest(), "HmacSHA256"));
            return m;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Materialises the permutation array by applying the password swaps to the
     * base order.
     *
     * @return the permutation, or {@code null} if the password was empty
     */
    public short[] getOrder() {
        short[] order = null;
        if (this.auxilaryArray != null) {
            if (this.length == InputParameters._16) 
                order = Order.defaultOrder.clone();
            else {
                order = new short[InputParameters._256];
                for (short i = 0; i < order.length; i++) 
                    order[i] = i;
                for (short i = 0; i < InputParameters.defaultOrder.auxilaryArray.length; i++) 
                    new ExchangeMove(i, i + InputParameters.defaultOrder.auxilaryArray[i]).perform(order);
            }
            for (short i = 0; i < this.auxilaryArray.length; i++) 
                new ExchangeMove(i, i + this.auxilaryArray[i]).perform(order);
        }
        return order;
    }

    /**
     * Returns the inverse permutation, i.e. {@code result[array[i]] == i}. Used
     * on the decrypting side to undo the two substitution steps.
     *
     * @param array a permutation of {@code 0..n-1}
     * @return its inverse permutation
     */
    public static short[] Inverse(short[] array) {
        short[] arr = new short[array.length];
        IntStream.range(0, array.length)
                .forEach(i -> arr[i] = (short) IntStream.range(0, array.length).reduce(-1, (k, l) -> (k == -1 && array[l] == i) ? l : k));
        return arr;
    }
}
