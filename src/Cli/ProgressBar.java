// Author: Othmane
package Cli;

/**
 * Draws the batch's aggregate progress as one bar on stderr, so concurrent files
 * show as a single advancing line rather than several tearing ones.
 *
 * <p>
 * Rendering only: the mean across files is computed by {@code Tools.BatchRun},
 * which the GUI shares, and handed here already reduced.
 *
 * @author Othmane
 */
final class ProgressBar {

    private static final int WIDTH = 30;

    /** Called from worker threads, hence synchronized: one writer at a time. */
    synchronized void render(int overall, int done, int total) {
        int filled = overall * WIDTH / 100;
        StringBuilder sb = new StringBuilder("\r[");
        for (int k = 0; k < WIDTH; k++)
            sb.append(k < filled ? '#' : '-');
        sb.append("] ");
        sb.append(overall);
        sb.append("%  (");
        sb.append(done);
        sb.append('/');
        sb.append(total);
        sb.append(")   ");
        System.err.print(sb);
        System.err.flush();
    }
}
