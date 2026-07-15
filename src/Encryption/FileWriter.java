package Encryption;


import Tools.InputParameters;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Background writer that owns a {@link FileReader} for the input file and
 * drains produced bytes to the output {@link RandomAccessFile}. Preallocates
 * the output to the expected size, reports progress, refuses to start when free
 * space is insufficient, and can optionally open the finished file. Cancelling
 * stops both reader and writer and lets the caller delete the partial output.
 *
 * <p>
 * The encrypt/decrypt loop fills its own {@code writeBlock} and hands full
 * blocks to the writer thread through the inherited bounded queue.
 * {@link #EndWriting()} flushes the final partial block and a
 * {@link BlockIO#EOF} sentinel; the writer thread writes blocks in order until
 * it sees it. See {@link BlockIO} for the queue and cancellation plumbing shared
 * with {@link FileReader}.
 *
 * @author Othmane
 */
public class FileWriter extends BlockIO {

    private short index = 0;
    private volatile boolean openOutputFile = false;
    private long FileSize = 0;
    private FileReader FR = null;
    private final File outputFile;
    private RandomAccessFile outputRAF = null;
    private byte[] writeBlock = null;

    /**
     * Opens the input file for reading and the output file for writing, sizing
     * the output to the expected result length. If there is not enough free
     * space, the output stays unopened and {@link #NoEnoughFreeSpace()} reports
     * {@code true}.
     *
     * @param InputFile source file being read
     * @param OF destination file to write (assigned to {@code outputFile})
     * @param decrypt {@code true} for decryption sizing, {@code false} for
     * encryption
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public FileWriter(File InputFile, File OF, boolean decrypt) throws IOException, InterruptedException {
        this.outputFile = OF;
        this.FR = new FileReader(new RandomAccessFile(InputFile, "r"));
        long size = this.FR.getFileSize();
        // header = 2*nameLen + 2 bytes; a per-file salt is prepended and a MAC tag
        // appended, both in the clear.
        size += (decrypt) ? -2 * this.outputFile.getName().length() - 2 - InputParameters.saltLength - InputParameters.tagLength
                          : 2 * InputFile.getName().length() + 2 + InputParameters.saltLength + InputParameters.tagLength;
        this.FileSize = size;
        if (size <= InputFile.getFreeSpace()) {
            this.FR.execute();
            this.outputRAF = new RandomAccessFile(this.outputFile, "rw");
            this.outputRAF.setLength(size);
            this.writeBlock = new byte[InputParameters.maxLength];
            this.setProgress(0);
        } 
        else 
            this.FR.CloseFile();
    }

    /**
     * Appends one byte to the current output block, handing the full block to
     * the writer thread (and starting a fresh block) when it fills.
     *
     * @param Byte byte to write
     */
    void WriteByte(byte Byte) throws IOException, InterruptedException {
        this.writeBlock[this.index] = Byte;
        this.index++;
        if (this.index == this.writeBlock.length) {
            enqueue(this.writeBlock);
            this.writeBlock = new byte[InputParameters.maxLength];
            this.index = 0;
        }
    }

    @Override
    protected Void doInBackground() throws IOException, InterruptedException {
        long written = 0;
        while (!this.cancel) {
            byte[] block = this.queue.poll(100, TimeUnit.MILLISECONDS);
            if (block == null) 
                continue;
            if (block == EOF) 
                break;
            this.outputRAF.write(block);
            written += block.length;
            int p = (int) (written * 100 / this.FileSize);
            if (p > this.getProgress() && p < 100) 
                this.setProgress(p);
        }
        this.outputRAF.close();
        this.queue.clear();
        if (!this.cancel) {
            this.setProgress(100);
            if (this.openOutputFile) 
                Desktop.getDesktop().open(this.outputFile);
        }
        return null;
    }

    /** Also stops the reader this writer owns, so both threads wind down. */
    @Override
    void Cancel() {
        super.Cancel();
        this.FR.Cancel();
    }

    /**
     * Blocks until the writer thread has stopped and closed the output file, so
     * the caller can safely delete a cancelled, partial output.
     */
    void AwaitFinished() throws InterruptedException {
        try {
            this.get();
        } catch (java.util.concurrent.ExecutionException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    short ReadUnsignedByte() throws IOException, InterruptedException {
        return this.FR.ReadUnsignedByte();
    }

    boolean HasMoreData() throws InterruptedException {
        return this.FR.HasMoreData();
    }

    /**
     * Flushes the final partial block, signals end of stream, then blocks until
     * the writer thread has drained the queue and closed the output file, so the
     * finished file is fully on disk when this returns.
     */
    void EndWriting() throws InterruptedException {
        if (this.index > 0)
            enqueue(Arrays.copyOf(this.writeBlock, this.index));
        enqueue(EOF);
        try {
            this.get();   // wait for doInBackground to finish writing and close the file
        } catch (java.util.concurrent.ExecutionException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    /**
     * @return {@code true} if the output could not be allocated for lack of
     * free space.
     */
    boolean NoEnoughFreeSpace() {
        return this.outputRAF == null;
    }

    void OpenOutputFile() {
        this.openOutputFile = true;
    }
}
