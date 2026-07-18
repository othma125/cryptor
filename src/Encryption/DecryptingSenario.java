package Encryption;


import Tools.InputParameters;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import javax.crypto.Mac;

/**
 * Runs the decryption of one {@code .cr} file on a background thread. Verifies
 * the file's MAC tag first ({@link #tagMatches}), then uses the inverse password
 * orders to recover the original file name from the header, then streams the body
 * back through the inverse substitution to reconstruct the plaintext via a
 * {@link FileWriter}. A wrong password is caught by the tag mismatch, and the
 * header consistency check is kept behind it as a second line; both surface
 * through {@link #WrongPassword()}. Reports progress, supports cancel, and can
 * open the result.
 *
 * @author Othmane
 */
public class DecryptingSenario extends Senario {

    public boolean openDecryptedFile;

    // Header-decode state, used only by getOutputFile and its helpers. The
    // header and body form one continuous bit stream, so index/outputByte/
    // previous/zeroCounter are left as-is for the body loop to pick up.
    private String outputPath;
    private boolean nameLengthKnown = false;
    private short nameLength;
    private final ArrayList<Short> nameArray = new ArrayList<>();
    private File decodedOutputFile = null;
    private boolean headerInvalid = false;

    /**
     * @param file {@code .cr} file to decrypt (assigned to {@code inputFile})
     * @param password password whose inverse derived orders undo the
     * substitution
     * @param OpenFile {@code true} to open the recovered file when finished (assigned to {@code openDecryptedFile})
     */
    private final char[] password;
    private BigInteger masterPW;   // seeds per-block substitution rekeying

    public DecryptingSenario(File file, char[] password, boolean OpenFile) {
        super(file);
        this.openDecryptedFile = OpenFile;
        this.password = password;   // orders are derived in doInBackground, once the salt is read
    }

    /**
     * Derives the inverse substitution orders from the password and the per-file
     * salt read from the {@code .cr} header. Must run before the header/body are
     * decoded.
     *
     * @param salt the salt bytes read from the start of the file
     */
    private void deriveOrders(byte[] salt) {
        this.masterPW = Order.getPassword(this.password, salt);
        this.firstLayer = Order.Inverse(new Order(InputParameters._256, this.masterPW).getOrder());
        this.secondLayer = Order.Inverse(new Order(InputParameters._16, this.masterPW).getOrder());
    }

    @Override
    protected void rekeyByteLayer(long block) {   // byte substitution is firstLayer (inverse 256-order) on decrypt
        // ponytail: O(256^2) Inverse rebuilt per block; fine at 64-byte blocks, revisit if profiling flags it
        this.firstLayer = Order.Inverse(new Order(InputParameters._256, Order.subKey(this.masterPW, block)).getOrder());
    }

    @Override
    protected Void doInBackground() throws FileNotFoundException, IOException, InterruptedException {
        File OutputFile;
        long pointer, bodyBytes;
        try (RandomAccessFile InputRAF = new RandomAccessFile(this.inputFile, "r")) {
            if (InputRAF.length() < InputParameters.saltLength + InputParameters.tagLength) {   // too short to hold salt + tag: not our file
                this.setProgress(100);
                return null;
            }
            byte[] salt = new byte[InputParameters.saltLength];
            InputRAF.readFully(salt);
            this.deriveOrders(salt);
            if (!this.tagMatches(InputRAF)) {   // wrong password or tampered file: interpret nothing
                this.setProgress(100);
                return null;
            }
            InputRAF.seek(InputParameters.saltLength);
            OutputFile = this.getOutputFile(InputRAF, this.inputFile.getParentFile().getAbsolutePath());
            if (OutputFile == null) {
                this.setProgress(100);
                return null;
            }
            pointer = InputRAF.getFilePointer();   // salt + encrypted header bytes; the body starts here
            bodyBytes = InputRAF.length() - InputParameters.tagLength - pointer;   // the trailing tag is not ciphertext
        }
        this.FW = new FileWriter(this.inputFile, OutputFile, true);
        if (this.NoEnoughFreeSpace()) {
            this.setProgress(100);
            return null;
        }
        this.FW.addPropertyChangeListener(this);
        this.FW.execute();
        while (pointer > 0) {
            pointer--;
            this.FW.ReadUnsignedByte();
        }
        if (this.index == InputParameters._8) {
            this.FW.WriteByte(this.transformOutput(this.outputByte));
            this.index = 0;
            this.outputByte = 0;
        }
        while (bodyBytes > 0 && this.FW.HasMoreData()) {   // stops short of the tag, which must not be decoded
            if (this.cancel) {
                this.FW.Cancel();
                break;
            }
            this.decodeByte(this.unchain(this.FW.ReadUnsignedByte()));
            bodyBytes--;
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
        this.OpenDecryptedFile();
        this.FW.EndWriting();
        return null;
    }

    @Override
    protected InputParameters.Pair gridPair(byte matrixIndex) {
        return InputParameters.pairs[this.secondLayer[matrixIndex]];
    }

    @Override
    protected byte transformOutput(short outputByte) {
        return (byte) outputByte;
    }

    /**
     * Undoes the CBC chaining from encryption. Encryption computes
     * {@code c = S[V ^ prev ^ ctr]}, so this inverts in the mirror order:
     * de-substitute first, then XOR out the previous ciphertext byte and the
     * position counter ({@code V = S⁻¹[c] ^ prev ^ ctr}). Both the header and
     * body read paths funnel through here so {@link #prevCipher} and
     * {@code gridByteCount} stay continuous across them.
     *
     * @param rawByte a byte read straight from the {@code .cr} file (0..255)
     * @return the de-substituted grid byte to feed the bit decoder
     */
    private short unchain(int rawByte) {
        short unchained = (short) ((this.firstLayer[rawByte] ^ this.prevCipher ^ (int) (this.gridByteCount & 0xff)) & 0xff);
        this.prevCipher = (short) rawByte;
        this.countGridByte();   // rekeys firstLayer for the next block, mirroring encryption
        return unchained;
    }

    /**
     * Encrypt-then-MAC verification (direction 5): recomputes HMAC-SHA256 over
     * everything but the trailing tag (i.e. salt + ciphertext) and compares it
     * against the stored tag. A wrong password derives a different MAC key, so
     * this doubles as an exact password check: unlike the header sniff, a
     * mismatch is conclusive rather than probabilistic.
     *
     * <p>
     * This costs one extra read pass over the file, and is worth it: verifying
     * before decoding means a forged or corrupt {@code .cr} never reaches the
     * header state machine, which resolves and deletes a path built from the
     * bytes it decodes.
     *
     * @param InputRAF open handle to the {@code .cr} file (repositioned by this
     * call; the caller must seek before decoding)
     * @return {@code true} if the tag matches, i.e. right password and untampered
     */
    private boolean tagMatches(RandomAccessFile InputRAF) throws IOException {
        Mac mac = Order.mac(this.masterPW);
        long remaining = InputRAF.length() - InputParameters.tagLength;
        InputRAF.seek(0);
        byte[] buffer = new byte[InputParameters.maxLength];
        while (remaining > 0) {
            int read = InputRAF.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (read < 0)
                return false;
            mac.update(buffer, 0, read);
            remaining -= read;
        }
        byte[] stored = new byte[InputParameters.tagLength];
        InputRAF.readFully(stored);
        return MessageDigest.isEqual(mac.doFinal(), stored);   // constant-time: Arrays.equals would leak via timing
    }

    /**
     * Reads and decodes the header to recover the original file name, returning
     * the target {@link File}. Returns {@code null} on a wrong password (header
     * inconsistent) or an invalid recovered path. On return, {@code InputRAF} is
     * positioned at the start of the encrypted body.
     *
     * <p>
     * The per-bit decode mirrors {@link #decodeByte(short)}: each run of
     * zeros/ones resolves to a matrix index, whose grid move is emitted bit by
     * bit through {@link #emitHeaderMove()} into the header state machine
     * (instead of {@link #emitMove(byte)}, which would write it to the output).
     *
     * @param InputRAF open handle to the {@code .cr} file, positioned at the
     * header
     * @param OutputPath directory in which to place the recovered file
     * @return the output file, or {@code null} if the password/path is invalid
     */
    private File getOutputFile(RandomAccessFile InputRAF, String OutputPath) throws IOException {
        this.outputPath = OutputPath;
        while (this.decodedOutputFile == null && InputRAF.getFilePointer() < InputRAF.length()) {
            short x = this.unchain(InputRAF.readUnsignedByte());
            for (byte y = 0; y < InputParameters._8; y++) {
                if (InputParameters.binaryCoding[x][y]) {
                    if (this.previous < InputParameters.n) {
                        if (!this.emitHeaderMove())
                            return null;
                        this.previous = InputParameters.n;
                    }
                    else
                        this.previous = this.zeroCounter;
                    this.zeroCounter = 0;
                }
                else {
                    this.zeroCounter++;
                    if (this.zeroCounter == InputParameters.m) {
                        if (this.previous < InputParameters.n) {
                            if (!this.emitHeaderMove())
                                return null;
                            this.previous = InputParameters.n;
                        }
                        else
                            this.previous = this.zeroCounter;
                        this.zeroCounter = 0;
                    }
                }
            }
        }
        return this.decodedOutputFile;
    }

    /**
     * Emits the grid move (X then Y steps) for the current matrix index into the
     * header decoder, packing bits into whole bytes.
     *
     * @return {@code false} if the header turned out inconsistent (wrong
     * password / invalid path), in which case decoding must stop
     */
    private boolean emitHeaderMove() {
        InputParameters.Pair pair = this.gridPair(InputParameters.indexMatrix[this.previous][this.zeroCounter]);
        for (byte j = 0; j < pair.X; j++)
            if (!this.bumpHeaderIndex())
                return false;
        if (pair.X < InputParameters.m) {
            this.outputByte += InputParameters.Power_2[this.index];
            if (!this.bumpHeaderIndex())
                return false;
        }
        for (byte j = 0; j < pair.Y; j++)
            if (!this.bumpHeaderIndex())
                return false;
        if (pair.Y < InputParameters.m) {
            this.outputByte += InputParameters.Power_2[this.index];
            if (!this.bumpHeaderIndex())
                return false;
        }
        return true;
    }

    /**
     * Advances the output bit index; when a whole byte completes, hands it to
     * {@link #processHeaderByte()} and resets the accumulator (unless the output
     * file was already resolved, matching the original streaming behaviour).
     *
     * @return {@code false} if the header became invalid and decoding must stop
     */
    private boolean bumpHeaderIndex() {
        this.index++;
        if (this.index == InputParameters._8) {
            boolean fileWasResolved = this.decodedOutputFile != null;
            this.processHeaderByte();
            if (this.headerInvalid)
                return false;
            if (!fileWasResolved) {
                this.index = 0;
                this.outputByte = 0;
            }
        }
        return true;
    }

    /**
     * Consumes one fully decoded header byte: the first byte is the file-name
     * length, subsequent bytes are name characters up to the
     * {@link InputParameters#endFileNameCharacter} terminator, at which point the
     * output {@link File} is resolved. Sets {@link #headerInvalid} if the name
     * overruns its declared length or the recovered path is invalid.
     */
    private void processHeaderByte() {
        if (!this.nameLengthKnown) {
            this.nameLength = this.outputByte;
            this.nameLengthKnown = true;
        }
        else if (this.decodedOutputFile == null) {
            if (this.outputByte == InputParameters.endFileNameCharacter && this.nameArray.size() == 2 * this.nameLength) {
                try {
                    this.decodedOutputFile = Paths.get(this.outputPath + File.separator + this.getOutputFileName(this.nameArray)).toFile();
                } catch (InvalidPathException e) {
                    this.headerInvalid = true;
                    return;
                }
                this.decodedOutputFile.delete();
                this.nameArray.clear();
            }
            else if (this.outputByte == InputParameters.endFileNameCharacter)
                this.nameArray.add(this.outputByte);
            else {
                this.nameArray.add(this.outputByte);
                if (this.nameArray.size() > 2 * this.nameLength)
                    this.headerInvalid = true;
            }
        }
    }

    /**
     * @return {@code true} if the file was rejected before any plaintext was
     * written: the MAC tag did not match (wrong password or a tampered file — the
     * two are deliberately indistinguishable, both mean "do not proceed"), or the
     * header could not be decoded.
     */
    public boolean WrongPassword() {
        return this.FW == null;
    }

    void OpenDecryptedFile() {
        if (this.openDecryptedFile)
            this.FW.OpenOutputFile();
    }

    /**
     * Rebuilds the file name from its decoded header bytes, pairing them as
     * little-endian base-256 character codes.
     *
     * @param v decoded header bytes (two per character)
     * @return the recovered file name
     */
    private String getOutputFileName(ArrayList<Short> v) {
        String name = "";
        short c = 0;
        for (int i = 0; i < v.size(); i++) {
            if (i % 2 == 0)
                c = v.get(i);
            else {
                c += InputParameters._256 * v.get(i);
                name += (char) c;
            }
        }
        return name;
    }
}
