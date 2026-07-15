package Encryption;

// Author: Othmane

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingWorker;

/**
 * Shared plumbing for the two block-I/O workers, {@link FileReader} and
 * {@link FileWriter}. Both move fixed-size byte blocks between the
 * encrypt/decrypt loop and a background thread over a bounded
 * {@link LinkedBlockingQueue}, and differ only in direction: the reader produces
 * blocks for the loop to consume, the writer consumes blocks the loop produces.
 * The queue bound provides backpressure for free — whichever side runs ahead
 * blocks until the other catches up — and end of stream is marked with the
 * {@link #EOF} sentinel.
 *
 * <p>
 * What lives here is only what both directions already had in common; the file
 * handles, sizing, progress and end-of-stream handling stay in the subclasses,
 * because they genuinely differ.
 *
 * @author Othmane
 */
public abstract class BlockIO extends SwingWorker {

    /**
     * Sentinel enqueued once to mark end of stream (identity-compared, never its
     * contents). A single shared instance is safe: each worker owns its own
     * queue, so the sentinel can never cross between them.
     */
    protected static final byte[] EOF = new byte[0];
    /**
     * Maximum buffered blocks; bounds memory and throttles the producer.
     */
    private static final int CAPACITY = 10;

    protected final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>(CAPACITY);
    protected volatile boolean cancel = false;

    /**
     * Hands a block to the other thread, honouring cancellation instead of
     * blocking forever: offer-with-timeout re-checks {@link #cancel} on every
     * pass, where a plain {@code put} on a full queue would wait for a consumer
     * that has already stopped.
     *
     * @param block block to hand over
     */
    protected void enqueue(byte[] block) throws InterruptedException {
        while (!this.cancel && !this.queue.offer(block, 100, TimeUnit.MILLISECONDS));
    }

    void Cancel() {
        this.cancel = true;
    }
}
