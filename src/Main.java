
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Application entry point. Sets the system look-and-feel and shows the main
 * window.
 *
 * @author Othmane
 */
public class Main {

    /**
     * Launches the Cryptor GUI.
     *
     * @param args command-line arguments (unused)
     * @throws UnsupportedLookAndFeelException if the system look-and-feel
     * cannot be applied
     * @throws IllegalAccessException if the look-and-feel class cannot be
     * accessed
     * @throws ClassNotFoundException if the look-and-feel class is not found
     * @throws InstantiationException if the look-and-feel class cannot be
     * instantiated
     * @throws InterruptedException if interrupted while building the frame
     */
    public static void main(String[] args) throws UnsupportedLookAndFeelException, IllegalAccessException, ClassNotFoundException, InstantiationException, InterruptedException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new MainFrame().setVisible(true);
    }
}
