package Encryption;


import Tools.InputParameters;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.StringTokenizer;
import java.util.stream.IntStream;
import javax.crypto.Mac;

/**
 * Runs the encryption of one file on a background thread. Derives two password
 * orders (16- and 256-element), writes a header with the original file name,
 * then streams the file through the bit-level substitution, emitting the result
 * via a {@link FileWriter} to a {@code <name>.cr} file next to the source.
 * Reports progress and supports cancel.
 *
 * @author Othmane
 */
public class EncryptingSenario extends Senario {

    /**
     * @param file plaintext file to encrypt (assigned to {@code inputFile})
     * @param password password whose derived orders drive the substitution
     * steps
     */
    private final byte[] salt;
    private final BigInteger masterPW;   // seeds per-block substitution rekeying
    /**
     * Encrypt-then-MAC (direction 5): accumulates HMAC-SHA256 over the salt and
     * every ciphertext byte, i.e. the whole {@code .cr} file except the tag
     * itself. The tag is appended last, so decryption can verify the file before
     * interpreting any of it.
     */
    private final Mac mac;

    public EncryptingSenario(File file, char[] password) {
        this(file, password, randomSalt());
    }

    /**
     * Encrypts with a caller-supplied salt instead of a random one. Normal use
     * should prefer the two-argument constructor; reusing a salt across files
     * defeats its purpose. Exposed for reproducible encryption (test vectors,
     * measuring plaintext diffusion under a fixed key).
     *
     * @param file plaintext file to encrypt
     * @param password password whose derived orders drive the substitution
     * @param salt per-file salt to store in the header and feed the key schedule
     */
    public EncryptingSenario(File file, char[] password, byte[] salt) {
        super(file);
        this.salt = salt.clone();
        this.masterPW = Order.getPassword(password, this.salt);
        this.mac = Order.mac(this.masterPW);
        this.firstLayer = new Order(InputParameters._16, this.masterPW).getOrder();
        this.secondLayer = new Order(InputParameters._256, this.masterPW).getOrder();
    }

    @Override
    protected void rekeyByteLayer(long block) {
        this.secondLayer = new Order(InputParameters._256, Order.subKey(this.masterPW, block)).getOrder();
    }

    private static byte[] randomSalt() {
        byte[] s = new byte[InputParameters.saltLength];
        new SecureRandom().nextBytes(s);
        return s;
    }

    @Override
    protected Void doInBackground() throws FileNotFoundException, IOException, InterruptedException {
        String InputFileName = this.inputFile.getName();
        File OutputFile = new File(this.inputFile.getParent() + File.separator + this.getEncryptingFileName(InputFileName) + "cr");
        OutputFile.delete();
        this.FW = new FileWriter(this.inputFile, OutputFile, false);
        if (this.NoEnoughFreeSpace()) {
            this.setProgress(100);
            return null;
        }
        this.FW.addPropertyChangeListener(this);
        this.FW.execute();
        this.mac.update(this.salt);   // the tag covers the salt too, so it cannot be swapped
        for (byte b : this.salt)   // salt written in the clear ahead of the encrypted header
            this.FW.WriteByte(b);
        this.decodeByte((short) InputFileName.length());
        IntStream.range(0, InputFileName.length())
                .map(InputFileName::charAt)
                .forEach((int c) -> {
                    this.decodeByte((short) ((short) c % InputParameters._256));
                    this.decodeByte((short) ((short) c / InputParameters._256));
                });
        this.decodeByte(InputParameters.endFileNameCharacter);
        while (this.FW.HasMoreData()) {
            if (this.cancel) {
                this.FW.Cancel();
                break;
            }
            this.decodeByte(this.FW.ReadUnsignedByte());
        }
        if (this.cancel) {
            this.FW.AwaitFinished();   // wait for the writer to close the file before deleting it
            OutputFile.delete();
            this.setProgress(100);
            return null;
        }
        if (this.previous < InputParameters.m) {
            for (byte j = 0; j < this.previous; j++)
                this.index++;
            this.outputByte += InputParameters.Power_2[this.index];
        }
        if (this.index > 0)
            this.FW.WriteByte(this.transformOutput(this.outputByte));
        for (byte b : this.mac.doFinal())   // tag last: written raw, never chained or substituted
            this.FW.WriteByte(b);
        this.FW.EndWriting();
        return null;
    }

    @Override
    protected InputParameters.Pair gridPair(byte matrixIndex) {
        return InputParameters.pairs[this.firstLayer[matrixIndex]];
    }

    @Override
    protected byte transformOutput(short outputByte) {
        // CBC chaining with a position counter: substitute the *chained* value
        // (c = S[V ^ prev ^ ctr]), not the bare one. Two reasons the XOR sits
        // inside the table index rather than after it:
        //  - S[V] ^ prev collapses on constant plaintext, where S[V] is a fixed
        //    s within a block: c_i = s ^ c_(i-1) just toggles, giving period 2.
        //  - S[V ^ prev] alone is a permutation of prev, so iterating it walks a
        //    cycle that can be short by chance; the counter varies the map per
        //    byte, so no fixed cycle exists to land in.
        short c = (short) this.secondLayer[(outputByte ^ this.prevCipher ^ (int) (this.gridByteCount & 0xff)) & 0xff];
        this.prevCipher = c;
        this.countGridByte();   // rekeys secondLayer for the next block once this one fills
        this.mac.update((byte) c);   // every ciphertext byte reaches disk through here
        return (byte) c;
    }

    /**
     * Strips the extension from the source name so the output becomes
     * {@code name.cr} (a name without an extension keeps a trailing dot).
     *
     * @param InputFileName original file name
     * @return base name with a trailing dot, ready for the {@code cr} suffix
     */
    private String getEncryptingFileName(String InputFileName) {
        StringTokenizer st = new StringTokenizer(InputFileName, ".");
        if (st.countTokens() == 1)
            return st.nextToken() + ".";
        String name = "", extension = "", NToken;
        while (st.hasMoreTokens())
            extension = st.nextToken();
        st = new StringTokenizer(InputFileName, ".");
        while (st.hasMoreTokens()) {
            NToken = st.nextToken();
            if (extension.equals(NToken))
                break;
            name += NToken + ".";
        }
        return name;
    }
}
