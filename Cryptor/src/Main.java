
import java.io.IOException;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Othmane
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    // TODO code application logic here
    public static void main(String[] args) throws UnsupportedLookAndFeelException, IllegalAccessException, ClassNotFoundException, InstantiationException{
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new MainFrame().setVisible(true);
//        javax.swing.SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                new MainFrame().setVisible(true);
//            }
//        });
    }
}
