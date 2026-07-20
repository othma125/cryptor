// Author: Othmane
package Cli;

/**
 * A shared, thread-safe progress bar for the whole batch: it draws the mean of
 * every file's 0..100 progress as one bar on stderr, so concurrent files show
 * as a single advancing line rather than several tearing ones.
 *
 * @author Othmane
 */
final class ProgressBar {

    private static final int WIDTH = 30;

    private final int[] pct;

    ProgressBar(int files) {
        this.pct = new int[files];
    }

    synchronized void set(int file, int value) {
        // Monotonic: the worker thread marks a slot 100 while a late 99 event
        // for it may still be queued on the EDT, so ignore any regression.
        if (value <= this.pct[file])
            return;
        this.pct[file] = value;
        this.render();
    }

    synchronized void render() {
        long sum = 0;
        int done = 0;
        for (int p : this.pct) {
            sum += p;
            if (p >= 100)
                done++;
        }
        int overall = (int) (sum / this.pct.length);
        int filled = overall * WIDTH / 100;
        StringBuilder sb = new StringBuilder("\r[");
        for (int k = 0; k < WIDTH; k++)
            sb.append(k < filled ? '#' : '-');
        sb.append("] ");
        sb.append(overall);
        sb.append("%  (");
        sb.append(done);
        sb.append('/');
        sb.append(this.pct.length);
        sb.append(")   ");
        System.err.print(sb);
        System.err.flush();
    }
}
