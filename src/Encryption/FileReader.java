package Encryption;


import Tools.InputParameters;
import Tools.SecureDelete;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Background reader that streams a {@link RandomAccessFile} into the inherited
 * bounded queue of fixed-size byte blocks, so the encrypt/decrypt loop can pull
 * unsigned bytes without blocking on disk I/O. End of stream is signalled with
 * the {@link BlockIO#EOF} sentinel block. See {@link BlockIO} for the queue and
 * cancellation plumbing shared with {@link FileWriter}.
 *
 * @author Othmane
 */
public class FileReader extends BlockIO {

    private final File sourceFile;
    private final RandomAccessFile inputRAF;
    // consumer-thread state only:
    private byte[] current = null;
    private int index = 0;
    private boolean eof = false;

    /**
     * @param source file to read; kept so its plaintext can be securely wiped
     * once encryption has finished with it (see {@link #wipeSource()})
     */
    public FileReader(File source) throws FileNotFoundException {
        this.sourceFile = source;
        this.inputRAF = new RandomAccessFile(source, "r");
    }

    /**
     * Securely overwrites and unlinks the source file this reader consumed. Must
     * be called only once the encrypted output is safely on disk, never on a
     * decrypt run: the read handle is already closed by then (this reader's
     * {@code doInBackground} closes it at end of stream), so the wipe can reopen
     * the file for the overwrite. See {@link SecureDelete}.
     */
    void wipeSource() throws IOException {
        SecureDelete.wipe(this.sourceFile);
    }

    @Override
    protected Void doInBackground() throws IOException, InterruptedException {
        try {
            while (!this.cancel && this.inputRAF.length() > this.inputRAF.getFilePointer()) {
                int remaining = (int) Math.min(InputParameters.maxLength,
                        this.inputRAF.length() - this.inputRAF.getFilePointer());
                byte[] tab = new byte[remaining];
                this.inputRAF.read(tab);
                this.enqueue(tab);
            }
        } finally {
            this.inputRAF.close();
            if (!this.cancel) 
                this.queue.put(EOF);   // consumer is still draining, so this cannot deadlock
        }
        return null;
    }

    /**
     * Ensures {@link #current} holds an unread byte, taking the next block
     * (blocking) when needed.
     *
     * @return {@code false} once the end-of-stream sentinel is reached
     */
    private boolean ensureAvailable() throws InterruptedException {
        while (this.current == null || this.index >= this.current.length) {
            if (this.eof) 
                return false;
            byte[] next = this.queue.take();
            if (next == EOF) {
                this.eof = true;
                return false;
            }
            this.current = next;
            this.index = 0;
        }
        return true;
    }

    /**
     * Returns the next buffered byte as an unsigned value {@code 0..255},
     * blocking until a block is available.
     *
     * @return next unsigned byte
     */
    short ReadUnsignedByte() throws InterruptedException {
        if (!ensureAvailable()) 
            throw new IllegalStateException("read past end of stream");
        short Byte = this.current[this.index++];
        if (Byte < 0) 
            Byte += InputParameters._256;
        return Byte;
    }

    /**
     * @return {@code true} if a byte is available now or still to come (blocks
     * to decide).
     */
    boolean HasMoreData() throws InterruptedException {
        return ensureAvailable();
    }

    void CloseFile() throws IOException {
        this.inputRAF.close();
    }

    long getFileSize() throws IOException {
        return this.inputRAF.length();
    }
}
