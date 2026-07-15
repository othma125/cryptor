package Encryption;

// Author: Othmane

import Tools.InputParameters;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 * Shared base for the encryption and decryption workers. Holds the common
 * bit-packing state and the parts of the bit-level substitution that are
 * identical in both directions: the per-byte run decomposition
 * ({@link #decodeByte(short)}) and the grid-move packing ({@link #emitMove(byte)}).
 * Subclasses supply the direction-specific pieces through {@link #gridPair(byte)}
 * and {@link #transformOutput(short)}, plus their own {@code doInBackground}.
 *
 * @author Othmane
 */
public abstract class Senario extends SwingWorker implements PropertyChangeListener {

    public volatile boolean cancel = false;
    /**
     * Previous ciphertext byte, for CBC-style chaining (direction 1). Encrypt
     * XORs it into each output byte after substitution; decrypt XORs it out of
     * each raw disk byte before the inverse substitution. Seeded to 0 (fixed IV);
     * a per-file random IV (direction 2) would seed this instead.
     */
    protected short prevCipher = 0;
    protected byte zeroCounter = 0;
    protected byte previous = InputParameters.n;
    protected byte index = 0;
    protected short outputByte = 0;
    protected short[] firstLayer;
    protected short[] secondLayer;
    protected final File inputFile;
    protected FileWriter FW = null;

    /**
     * Byte-substitution rekeying (direction 4). Every {@link #REKEY_BLOCK}
     * ciphertext bytes the 256-element substitution is rebuilt from a fresh
     * sub-key, so constant/periodic plaintext no longer maps through one fixed
     * table for the whole file. Unlike the grid order, the substitution is
     * applied to whole bytes at the byte-level hook ({@code transformOutput} /
     * {@code unchain}), so it rekeys with no move-straddle: both directions
     * count the same ciphertext bytes and switch at the same points.
     */
    protected static final int REKEY_BLOCK = 64;

    /**
     * Counts ciphertext bytes emitted so far, and doubles as the chaining
     * counter. Read before {@link #countGridByte()} bumps it, it is the 0-based
     * index of the byte being transformed, and both directions reach that read
     * at the same point for the same byte — the same property that keeps
     * {@link #REKEY_BLOCK} rekeying in sync.
     */
    protected long gridByteCount = 0;
    private long blockIndex = 0;

    /** Rebuilds this direction's byte substitution from block {@code block}'s sub-key. */
    protected abstract void rekeyByteLayer(long block);

    /** Counts one ciphertext byte and rekeys the substitution at each block boundary. */
    protected void countGridByte() {
        this.gridByteCount++;
        if (this.gridByteCount % REKEY_BLOCK == 0)
            this.rekeyByteLayer(++this.blockIndex);
    }

    /**
     * @param file file this worker reads (assigned to {@code inputFile})
     */
    protected Senario(File file) {
        this.inputFile = file;
    }

    /**
     * Decomposes one byte into runs of zeros/ones, mapping each run through the
     * secret index matrix and handing the resolved index to
     * {@link #emitMove(byte)}.
     *
     * @param value byte value {@code 0..255} to decode
     */
    protected void decodeByte(short value) {
        for (byte i = 0; i < InputParameters._8; i++) {
            if (InputParameters.binaryCoding[value][i]) {
                if (this.previous < InputParameters.n) {
                    this.emitMove(InputParameters.indexMatrix[this.previous][this.zeroCounter]);
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
                        this.emitMove(InputParameters.indexMatrix[this.previous][this.zeroCounter]);
                        this.previous = InputParameters.n;
                    }
                    else
                        this.previous = this.zeroCounter;
                    this.zeroCounter = 0;
                }
            }
        }
    }

    /**
     * Emits the grid move (X then Y steps) for a matrix index, packing bits into
     * whole output bytes handed to the {@link FileWriter}.
     *
     * @param matrixIndex matrix index to encode
     */
    protected void emitMove(byte matrixIndex) {
        InputParameters.Pair pair = this.gridPair(matrixIndex);
        for (byte j = 0; j < pair.X; j++)
            this.packBit();
        if (pair.X < InputParameters.m) {
            this.outputByte += InputParameters.Power_2[this.index];
            this.packBit();
        }
        for (byte j = 0; j < pair.Y; j++)
            this.packBit();
        if (pair.Y < InputParameters.m) {
            this.outputByte += InputParameters.Power_2[this.index];
            this.packBit();
        }
    }

    /**
     * Advances the output bit index; when a whole byte completes, writes it
     * (after {@link #transformOutput(short)}) and resets the accumulator.
     */
    private void packBit() {
        this.index++;
        if (this.index == InputParameters._8) {
            try {
                this.FW.WriteByte(this.transformOutput(this.outputByte));
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(Senario.class.getName()).log(Level.SEVERE, null, ex);
            }
            this.index = 0;
            this.outputByte = 0;
        }
    }

    /**
     * @param matrixIndex resolved matrix index
     * @return the grid coordinate pair for {@code matrixIndex} under this
     * direction's order (first-step order when encrypting, its inverse when
     * decrypting)
     */
    protected abstract InputParameters.Pair gridPair(byte matrixIndex);

    /**
     * @param outputByte a fully packed output byte
     * @return the byte actually written to disk: substituted through the
     * second-step order when encrypting, raw when decrypting
     */
    protected abstract byte transformOutput(short outputByte);

    public void Cancel() {
        this.cancel = true;
    }

    public boolean isCanceled() {
        return this.cancel;
    }

    public boolean NoEnoughFreeSpace() {
        return this.FW.NoEnoughFreeSpace();
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if ("progress".matches(e.getPropertyName()))
            this.setProgress((int) e.getNewValue());
    }
}
