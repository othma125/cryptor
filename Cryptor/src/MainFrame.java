
import Tools.InputParameters;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import javax.swing.JFileChooser;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Othmane
 */
public class MainFrame extends javax.swing.JFrame implements PropertyChangeListener{
    final char[] Password="password".toCharArray();
    final String Name="Cryptor";
    final String DesktopPath=System.getProperty("user.home")+"\\Desktop";
    JFileChooser fc;
    File SelectedFile;
    EncryptingSenario EncSen=null;
    DecryptingSenario DecSen=null;
    Image Logo=Toolkit.getDefaultToolkit().getImage("Icon.gif");
    public MainFrame(){
        new InputParameters();
        this.initComponents();
        this.setTitle(this.Name);
        this.setLocationRelativeTo(null);
        this.setIconImage(this.Logo);
//        this.jProgressBar.setStringPainted(true);
        this.EncryptingButton.setEnabled(false);
        this.DecryptingButton.setEnabled(false);
        this.EncryptingPassword1.setText("");
        this.EncryptingPassword2.setText("");
        this.DecryptingPassword.setText("");
        this.jDialog_PasswordsDoNotMatch.setTitle(this.Name);
        this.jDialog_PasswordsDoNotMatch.pack();
        this.jDialog_PasswordsDoNotMatch.setLocationRelativeTo(this);
        this.jDialog_Done.setTitle(this.Name);
        this.jDialog_Done.pack();
        this.jDialog_Done.setLocationRelativeTo(this);
        this.jDialog_ChosenFileEmty.setTitle(this.Name);
        this.jDialog_ChosenFileEmty.pack();
        this.jDialog_ChosenFileEmty.setLocationRelativeTo(this);
        this.jDialog_WrongPassword.setTitle(this.Name);
        this.jDialog_WrongPassword.pack();
        this.jDialog_WrongPassword.setLocationRelativeTo(this);
        this.jDialog_InputParameterFileNotFound.setTitle(this.Name);
        this.jDialog_InputParameterFileNotFound.pack();
        this.jDialog_InputParameterFileNotFound.setLocationRelativeTo(this);
        this.jDialog_NoEnoughFreeSpace.setTitle(this.Name);
        this.jDialog_NoEnoughFreeSpace.pack();
        this.jDialog_NoEnoughFreeSpace.setLocationRelativeTo(this);
        if(InputParameters.InputParameterFileNotFound){
            this.setEnabled(false);
            this.jDialog_InputParameterFileNotFound.setVisible(true);
        }
    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jDialog_PasswordsDoNotMatch = new javax.swing.JDialog();
        jButton_PasswordsDoNotMatch = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jDialog_Done = new javax.swing.JDialog();
        jButton_Done = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jDialog_WrongPassword = new javax.swing.JDialog();
        jLabel7 = new javax.swing.JLabel();
        jButton_WrongPassword = new javax.swing.JButton();
        jDialog_InputParameterFileNotFound = new javax.swing.JDialog();
        jLabel9 = new javax.swing.JLabel();
        jButton_InputParameterFileNotFound = new javax.swing.JButton();
        jDialog_NoEnoughFreeSpace = new javax.swing.JDialog();
        jLabel8 = new javax.swing.JLabel();
        jButton_NoEnoughFreeSpace = new javax.swing.JButton();
        jDialog_ChosenFileEmty = new javax.swing.JDialog();
        jLabel10 = new javax.swing.JLabel();
        jButton__ChosenFileEmty = new javax.swing.JButton();
        jProgressBar = new javax.swing.JProgressBar();
        jTabbedPane = new javax.swing.JTabbedPane();
        EncryptingPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        EncryptingPassword1 = new javax.swing.JPasswordField();
        EncryptingPassword2 = new javax.swing.JPasswordField();
        EncryptingBrowserButton = new javax.swing.JButton();
        EncryptingButton = new javax.swing.JButton();
        DecryptingPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        DecryptingPassword = new javax.swing.JPasswordField();
        DecryptingBrowserButton = new javax.swing.JButton();
        DecryptingButton = new javax.swing.JButton();
        jCheckBox_OpenFile = new javax.swing.JCheckBox();
        AboutPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jButton_Cancel = new javax.swing.JButton();

        jDialog_PasswordsDoNotMatch.setAlwaysOnTop(true);
        jDialog_PasswordsDoNotMatch.setResizable(false);
        jDialog_PasswordsDoNotMatch.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                jDialog_PasswordsDoNotMatchWindowClosing(evt);
            }
        });

        jButton_PasswordsDoNotMatch.setText("OK");
        jButton_PasswordsDoNotMatch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_PasswordsDoNotMatchActionPerformed(evt);
            }
        });

        jLabel3.setText("The entered passwords do not match");

        javax.swing.GroupLayout jDialog_PasswordsDoNotMatchLayout = new javax.swing.GroupLayout(jDialog_PasswordsDoNotMatch.getContentPane());
        jDialog_PasswordsDoNotMatch.getContentPane().setLayout(jDialog_PasswordsDoNotMatchLayout);
        jDialog_PasswordsDoNotMatchLayout.setHorizontalGroup(
            jDialog_PasswordsDoNotMatchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog_PasswordsDoNotMatchLayout.createSequentialGroup()
                .addGroup(jDialog_PasswordsDoNotMatchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jDialog_PasswordsDoNotMatchLayout.createSequentialGroup()
                        .addGap(48, 48, 48)
                        .addComponent(jLabel3))
                    .addGroup(jDialog_PasswordsDoNotMatchLayout.createSequentialGroup()
                        .addGap(125, 125, 125)
                        .addComponent(jButton_PasswordsDoNotMatch)))
                .addContainerGap(51, Short.MAX_VALUE))
        );
        jDialog_PasswordsDoNotMatchLayout.setVerticalGroup(
            jDialog_PasswordsDoNotMatchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog_PasswordsDoNotMatchLayout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addComponent(jLabel3)
                .addGap(18, 18, 18)
                .addComponent(jButton_PasswordsDoNotMatch)
                .addContainerGap(35, Short.MAX_VALUE))
        );

        jDialog_Done.setAlwaysOnTop(true);
        jDialog_Done.setResizable(false);
        jDialog_Done.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                jDialog_DoneWindowClosing(evt);
            }
        });

        jButton_Done.setText("OK");
        jButton_Done.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_DoneActionPerformed(evt);
            }
        });

        jLabel4.setText("  Done !");

        javax.swing.GroupLayout jDialog_DoneLayout = new javax.swing.GroupLayout(jDialog_Done.getContentPane());
        jDialog_Done.getContentPane().setLayout(jDialog_DoneLayout);
        jDialog_DoneLayout.setHorizontalGroup(
            jDialog_DoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog_DoneLayout.createSequentialGroup()
                .addGap(124, 124, 124)
                .addGroup(jDialog_DoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton_Done)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(135, Short.MAX_VALUE))
        );
        jDialog_DoneLayout.setVerticalGroup(
            jDialog_DoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog_DoneLayout.createSequentialGroup()
                .addContainerGap(38, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addGap(28, 28, 28)
                .addComponent(jButton_Done)
                .addGap(21, 21, 21))
        );

        jDialog_WrongPassword.setAlwaysOnTop(true);
        jDialog_WrongPassword.setResizable(false);
        jDialog_WrongPassword.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                jDialog_WrongPasswordWindowClosing(evt);
            }
        });

        jLabel7.setText("Wrong password !");

        jButton_WrongPassword.setText("OK");
        jButton_WrongPassword.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_WrongPasswordActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jDialog_WrongPasswordLayout = new javax.swing.GroupLayout(jDialog_WrongPassword.getContentPane());
        jDialog_WrongPassword.getContentPane().setLayout(jDialog_WrongPasswordLayout);
        jDialog_WrongPasswordLayout.setHorizontalGroup(
            jDialog_WrongPasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog_WrongPasswordLayout.createSequentialGroup()
                .addGroup(jDialog_WrongPasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jDialog_WrongPasswordLayout.createSequentialGroup()
                        .addGap(129, 129, 129)
                        .addComponent(jButton_WrongPassword))
                    .addGroup(jDialog_WrongPasswordLayout.createSequentialGroup()
                        .addGap(100, 100, 100)
                        .addComponent(jLabel7)))
                .addContainerGap(104, Short.MAX_VALUE))
        );
        jDialog_WrongPasswordLayout.setVerticalGroup(
            jDialog_WrongPasswordLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog_WrongPasswordLayout.createSequentialGroup()
                .addContainerGap(29, Short.MAX_VALUE)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton_WrongPassword)
                .addGap(22, 22, 22))
        );

        jDialog_InputParameterFileNotFound.setAlwaysOnTop(true);
        jDialog_InputParameterFileNotFound.setResizable(false);
        jDialog_InputParameterFileNotFound.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                jDialog_InputParameterFileNotFoundWindowClosing(evt);
            }
        });

        jLabel9.setText("The file \"InputParameters\" is not found");

        jButton_InputParameterFileNotFound.setText("Exit");
        jButton_InputParameterFileNotFound.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_InputParameterFileNotFoundActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jDialog_InputParameterFileNotFoundLayout = new javax.swing.GroupLayout(jDialog_InputParameterFileNotFound.getContentPane());
        jDialog_InputParameterFileNotFound.getContentPane().setLayout(jDialog_InputParameterFileNotFoundLayout);
        jDialog_InputParameterFileNotFoundLayout.setHorizontalGroup(
            jDialog_InputParameterFileNotFoundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog_InputParameterFileNotFoundLayout.createSequentialGroup()
                .addGroup(jDialog_InputParameterFileNotFoundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jDialog_InputParameterFileNotFoundLayout.createSequentialGroup()
                        .addGap(56, 56, 56)
                        .addComponent(jLabel9))
                    .addGroup(jDialog_InputParameterFileNotFoundLayout.createSequentialGroup()
                        .addGap(140, 140, 140)
                        .addComponent(jButton_InputParameterFileNotFound)))
                .addContainerGap(70, Short.MAX_VALUE))
        );
        jDialog_InputParameterFileNotFoundLayout.setVerticalGroup(
            jDialog_InputParameterFileNotFoundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog_InputParameterFileNotFoundLayout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                .addComponent(jButton_InputParameterFileNotFound)
                .addGap(25, 25, 25))
        );

        jDialog_NoEnoughFreeSpace.setResizable(false);
        jDialog_NoEnoughFreeSpace.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                jDialog_NoEnoughFreeSpaceWindowClosing(evt);
            }
        });

        jLabel8.setText("There is no enough free space in this partition");

        jButton_NoEnoughFreeSpace.setText("OK");
        jButton_NoEnoughFreeSpace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_NoEnoughFreeSpaceActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jDialog_NoEnoughFreeSpaceLayout = new javax.swing.GroupLayout(jDialog_NoEnoughFreeSpace.getContentPane());
        jDialog_NoEnoughFreeSpace.getContentPane().setLayout(jDialog_NoEnoughFreeSpaceLayout);
        jDialog_NoEnoughFreeSpaceLayout.setHorizontalGroup(
            jDialog_NoEnoughFreeSpaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog_NoEnoughFreeSpaceLayout.createSequentialGroup()
                .addContainerGap(29, Short.MAX_VALUE)
                .addComponent(jLabel8)
                .addGap(26, 26, 26))
            .addGroup(jDialog_NoEnoughFreeSpaceLayout.createSequentialGroup()
                .addGap(125, 125, 125)
                .addComponent(jButton_NoEnoughFreeSpace)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jDialog_NoEnoughFreeSpaceLayout.setVerticalGroup(
            jDialog_NoEnoughFreeSpaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog_NoEnoughFreeSpaceLayout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                .addComponent(jButton_NoEnoughFreeSpace)
                .addGap(26, 26, 26))
        );

        jLabel10.setText("The chosen file is empty");

        jButton__ChosenFileEmty.setText("OK");
        jButton__ChosenFileEmty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton__ChosenFileEmtyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jDialog_ChosenFileEmtyLayout = new javax.swing.GroupLayout(jDialog_ChosenFileEmty.getContentPane());
        jDialog_ChosenFileEmty.getContentPane().setLayout(jDialog_ChosenFileEmtyLayout);
        jDialog_ChosenFileEmtyLayout.setHorizontalGroup(
            jDialog_ChosenFileEmtyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog_ChosenFileEmtyLayout.createSequentialGroup()
                .addGroup(jDialog_ChosenFileEmtyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jDialog_ChosenFileEmtyLayout.createSequentialGroup()
                        .addGap(124, 124, 124)
                        .addComponent(jButton__ChosenFileEmty))
                    .addGroup(jDialog_ChosenFileEmtyLayout.createSequentialGroup()
                        .addGap(82, 82, 82)
                        .addComponent(jLabel10)))
                .addContainerGap(89, Short.MAX_VALUE))
        );
        jDialog_ChosenFileEmtyLayout.setVerticalGroup(
            jDialog_ChosenFileEmtyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog_ChosenFileEmtyLayout.createSequentialGroup()
                .addContainerGap(35, Short.MAX_VALUE)
                .addComponent(jLabel10)
                .addGap(29, 29, 29)
                .addComponent(jButton__ChosenFileEmty)
                .addGap(23, 23, 23))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jLabel1.setText("Encrypting password");

        EncryptingPassword1.setText("jPasswordField1");

        EncryptingPassword2.setText("jPasswordField2");

        EncryptingBrowserButton.setText("Browser");
        EncryptingBrowserButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EncryptingBrowserButtonActionPerformed(evt);
            }
        });

        EncryptingButton.setText("Encrypt");
        EncryptingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EncryptingButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout EncryptingPanelLayout = new javax.swing.GroupLayout(EncryptingPanel);
        EncryptingPanel.setLayout(EncryptingPanelLayout);
        EncryptingPanelLayout.setHorizontalGroup(
            EncryptingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(EncryptingPanelLayout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addGroup(EncryptingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(EncryptingPassword1, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(EncryptingPassword2, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 145, Short.MAX_VALUE)
                .addGroup(EncryptingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(EncryptingBrowserButton)
                    .addComponent(EncryptingButton, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30))
        );
        EncryptingPanelLayout.setVerticalGroup(
            EncryptingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(EncryptingPanelLayout.createSequentialGroup()
                .addGroup(EncryptingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(EncryptingPanelLayout.createSequentialGroup()
                        .addGap(48, 48, 48)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(EncryptingPassword1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(EncryptingPassword2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(EncryptingPanelLayout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addComponent(EncryptingBrowserButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(EncryptingButton)))
                .addContainerGap(30, Short.MAX_VALUE))
        );

        jTabbedPane.addTab("Encrypting", EncryptingPanel);

        jLabel2.setText("Decrypting password");

        DecryptingPassword.setText("jPasswordField3");

        DecryptingBrowserButton.setText("Browser");
        DecryptingBrowserButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DecryptingBrowserButtonActionPerformed(evt);
            }
        });

        DecryptingButton.setText("Decrypt");
        DecryptingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DecryptingButtonActionPerformed(evt);
            }
        });

        jCheckBox_OpenFile.setText("Open the decrypted file");

        javax.swing.GroupLayout DecryptingPanelLayout = new javax.swing.GroupLayout(DecryptingPanel);
        DecryptingPanel.setLayout(DecryptingPanelLayout);
        DecryptingPanelLayout.setHorizontalGroup(
            DecryptingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DecryptingPanelLayout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addGroup(DecryptingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(DecryptingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(DecryptingPassword, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING))
                    .addComponent(jCheckBox_OpenFile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 110, Short.MAX_VALUE)
                .addGroup(DecryptingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(DecryptingBrowserButton, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(DecryptingButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30))
        );
        DecryptingPanelLayout.setVerticalGroup(
            DecryptingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DecryptingPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(DecryptingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(DecryptingPanelLayout.createSequentialGroup()
                        .addComponent(DecryptingBrowserButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(DecryptingButton))
                    .addGroup(DecryptingPanelLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(DecryptingPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox_OpenFile)
                .addContainerGap(26, Short.MAX_VALUE))
        );

        jTabbedPane.addTab("Decrypting", DecryptingPanel);

        jLabel5.setText("Made by Othmane EL YAAKOUBI");

        jLabel6.setText("Email : othma125@hotmail.com");

        javax.swing.GroupLayout AboutPanelLayout = new javax.swing.GroupLayout(AboutPanel);
        AboutPanel.setLayout(AboutPanelLayout);
        AboutPanelLayout.setHorizontalGroup(
            AboutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, AboutPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(AboutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel5))
                .addGap(107, 107, 107))
        );
        AboutPanelLayout.setVerticalGroup(
            AboutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AboutPanelLayout.createSequentialGroup()
                .addGap(56, 56, 56)
                .addComponent(jLabel5)
                .addGap(18, 18, 18)
                .addComponent(jLabel6)
                .addContainerGap(56, Short.MAX_VALUE))
        );

        jTabbedPane.addTab("About", AboutPanel);

        jButton_Cancel.setText("Cancel");
        jButton_Cancel.setEnabled(false);
        jButton_Cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_CancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 283, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton_Cancel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(35, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jTabbedPane)
                    .addContainerGap()))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(199, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButton_Cancel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(14, 14, 14))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(43, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    private void jButton_PasswordsDoNotMatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_PasswordsDoNotMatchActionPerformed
        // TODO add your handling code here:
        this.jTabbedPane.setEnabled(true);
        this.setEnabled(true);
        this.EncryptingButton.setEnabled(true);
        this.EncryptingPassword1.setText("");
        this.EncryptingPassword2.setText("");
        this.jDialog_PasswordsDoNotMatch.setVisible(false);
    }//GEN-LAST:event_jButton_PasswordsDoNotMatchActionPerformed
    private void jButton_DoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_DoneActionPerformed
        // TODO add your handling code here:
        this.jTabbedPane.setEnabled(true);
        this.setEnabled(true);
        if(this.jTabbedPane.getSelectedIndex()==0){
            this.DecryptingButton.setEnabled(false);
            this.EncSen=null;
        }
        else{
            this.EncryptingButton.setEnabled(false);
            this.DecSen=null;
        }
        this.jDialog_Done.setVisible(false);
    }//GEN-LAST:event_jButton_DoneActionPerformed
    private void jButton_WrongPasswordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_WrongPasswordActionPerformed
        // TODO add your handling code here:
        this.jTabbedPane.setEnabled(true);
        this.setEnabled(true);
        this.DecryptingPassword.setText("");
        this.jDialog_WrongPassword.setVisible(false);
    }//GEN-LAST:event_jButton_WrongPasswordActionPerformed
    private void DecryptingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DecryptingButtonActionPerformed
        // TODO add your handling code here:
        if(this.DecSen!=null)
            return;
        this.jTabbedPane.setEnabled(false);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        char[] password=this.DecryptingPassword.getPassword();
        if(password.length==0)
            this.DecSen=new DecryptingSenario(this.SelectedFile,this.Password,this.jCheckBox_OpenFile.isSelected());
        else
            this.DecSen=new DecryptingSenario(this.SelectedFile,password,this.jCheckBox_OpenFile.isSelected());
        this.DecSen.addPropertyChangeListener(this);
        this.DecSen.execute();
    }//GEN-LAST:event_DecryptingButtonActionPerformed
    private void DecryptingBrowserButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DecryptingBrowserButtonActionPerformed
        // TODO add your handling code here:
        if(this.DecSen!=null)
            return;
        this.DecryptingButton.setEnabled(false);
        this.DecryptingPassword.setText("");
        this.fc=new JFileChooser();
        this.fc.setCurrentDirectory(new File(this.DesktopPath));
        javax.swing.filechooser.FileFilter ff=new javax.swing.filechooser.FileFilter(){
            @Override
            public boolean accept(File f){
                return f.isDirectory() || f.getName().endsWith(".cr");
            }
            @Override
            public String getDescription(){
                return "File .cr";
            }
        };
        this.fc.removeChoosableFileFilter(fc.getAcceptAllFileFilter());
        this.fc.setFileFilter(ff);
        int returnVal=this.fc.showOpenDialog(this);
        if(returnVal==JFileChooser.APPROVE_OPTION){
            this.SelectedFile=this.fc.getSelectedFile();
            try(RandomAccessFile RAF=new RandomAccessFile(this.SelectedFile,"r")){
                if(RAF.length()>0){
                    this.DecryptingButton.setEnabled(true);
                    this.EncryptingButton.setEnabled(false);
                }
                else
                    this.jDialog_ChosenFileEmty.setVisible(true);
                RAF.close();
            }catch(FileNotFoundException ex){
            }catch(IOException ex){}
        }
    }//GEN-LAST:event_DecryptingBrowserButtonActionPerformed
    private void EncryptingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EncryptingButtonActionPerformed
        // TODO add your handling code here:
        if(this.EncSen!=null)
            return;
        this.jTabbedPane.setEnabled(false);
        char[] password1=this.EncryptingPassword1.getPassword();
        char[] password2=this.EncryptingPassword2.getPassword();
        if(password1.length==0 && password2.length==0){
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            this.EncSen=new EncryptingSenario(this.SelectedFile,this.Password);
            this.EncSen.addPropertyChangeListener(this);
            this.EncSen.execute();
        }
        else{
            if(MainFrame.CompairInputPasswords(password1,password2)){
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                this.EncSen=new EncryptingSenario(this.SelectedFile,password1);
                this.EncSen.addPropertyChangeListener(this);
                this.EncSen.execute();
            }
            else{
                this.setEnabled(false);
                this.jDialog_PasswordsDoNotMatch.setVisible(true);
            }
        }
    }//GEN-LAST:event_EncryptingButtonActionPerformed
    private void EncryptingBrowserButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EncryptingBrowserButtonActionPerformed
        // TODO add your handling code here:
        if(this.EncSen!=null)
            return;
        this.EncryptingButton.setEnabled(false);
        this.EncryptingPassword1.setText("");
        this.EncryptingPassword2.setText("");
        this.fc=new JFileChooser();
        this.fc.setCurrentDirectory(new File(this.DesktopPath));
        int returnVal=this.fc.showOpenDialog(this);
        if(returnVal==JFileChooser.APPROVE_OPTION){
            this.SelectedFile=this.fc.getSelectedFile();
            try(RandomAccessFile RAF=new RandomAccessFile(this.SelectedFile,"r")){
                if(RAF.length()>0){
                    this.EncryptingButton.setEnabled(true);
                    this.DecryptingButton.setEnabled(false);
                }
                else
                    this.jDialog_ChosenFileEmty.setVisible(true);
                RAF.close();
            }catch(FileNotFoundException ex){
            }catch(IOException ex){}
        }
    }//GEN-LAST:event_EncryptingBrowserButtonActionPerformed
    private void jButton_InputParameterFileNotFoundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_InputParameterFileNotFoundActionPerformed
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_jButton_InputParameterFileNotFoundActionPerformed
    private void jDialog_InputParameterFileNotFoundWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_jDialog_InputParameterFileNotFoundWindowClosing
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_jDialog_InputParameterFileNotFoundWindowClosing
    private void jDialog_WrongPasswordWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_jDialog_WrongPasswordWindowClosing
        // TODO add your handling code here:
        this.jTabbedPane.setEnabled(true);
        this.setEnabled(true);
        this.DecryptingPassword.setText("");
        this.jDialog_WrongPassword.setVisible(false);
    }//GEN-LAST:event_jDialog_WrongPasswordWindowClosing
    private void jDialog_DoneWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_jDialog_DoneWindowClosing
        // TODO add your handling code here:
        this.jTabbedPane.setEnabled(true);
        this.setEnabled(true);
        if(this.jTabbedPane.getSelectedIndex()==0){
            this.DecryptingButton.setEnabled(false);
            this.EncSen=null;
        }
        else{
            this.EncryptingButton.setEnabled(false);
            this.DecSen=null;
        }
    }//GEN-LAST:event_jDialog_DoneWindowClosing
    private void jDialog_PasswordsDoNotMatchWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_jDialog_PasswordsDoNotMatchWindowClosing
        // TODO add your handling code here:
        this.jTabbedPane.setEnabled(true);
        this.setEnabled(true);
        this.EncryptingButton.setEnabled(true);
        this.EncryptingPassword1.setText("");
        this.EncryptingPassword2.setText("");
        this.jDialog_PasswordsDoNotMatch.setVisible(false);
    }//GEN-LAST:event_jDialog_PasswordsDoNotMatchWindowClosing

    private void jButton_CancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_CancelActionPerformed
        // TODO add your handling code here:
        if(this.jTabbedPane.getSelectedIndex()==0)
            this.EncSen.Cancel();
        else
            this.DecSen.Cancel();
    }//GEN-LAST:event_jButton_CancelActionPerformed

    private void jButton_NoEnoughFreeSpaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_NoEnoughFreeSpaceActionPerformed
        // TODO add your handling code here:
        this.jTabbedPane.setEnabled(true);
        this.setEnabled(true);
        this.jDialog_NoEnoughFreeSpace.setVisible(false);
    }//GEN-LAST:event_jButton_NoEnoughFreeSpaceActionPerformed

    private void jDialog_NoEnoughFreeSpaceWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_jDialog_NoEnoughFreeSpaceWindowClosing
        // TODO add your handling code here:
        this.jTabbedPane.setEnabled(true);
        this.setEnabled(true);
        this.jDialog_NoEnoughFreeSpace.setVisible(false);
    }//GEN-LAST:event_jDialog_NoEnoughFreeSpaceWindowClosing

    private void jButton__ChosenFileEmtyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton__ChosenFileEmtyActionPerformed
        // TODO add your handling code here:
        this.jDialog_ChosenFileEmty.setVisible(false);
    }//GEN-LAST:event_jButton__ChosenFileEmtyActionPerformed
    private static boolean CompairInputPasswords(char[] password1,char[] password2){
        if(password1.length!=password2.length)
            return false;
        for(int i=0;i<password1.length;i++)
            if(password1[i]!=password2[i])
                return false;
        return true;
    }
    @Override
    public void propertyChange(PropertyChangeEvent evt){
        if("progress".matches(evt.getPropertyName())){
            int progress=(int)evt.getNewValue();
            this.jProgressBar.setValue(progress);
            if(progress==100){
                this.setCursor(null);
                this.jButton_Cancel.setEnabled(false);
                this.jProgressBar.setValue(0);
                if(this.jTabbedPane.getSelectedIndex()==0){
                    if(this.EncSen.isCanceled()){
                        this.jTabbedPane.setEnabled(true);
                        this.DecryptingButton.setEnabled(false);
                        this.EncSen=null;
                    }
                    else if(this.EncSen.NoEnoughFreeSpace()){
                        this.setEnabled(false);
                        this.jDialog_NoEnoughFreeSpace.setVisible(true);
                        this.EncSen=null;
                    }
                    else{
                        this.setEnabled(false);
                        this.jDialog_Done.setVisible(true);
                    }
                }
                else{
                    if(this.DecSen.isCanceled()){
                        this.jTabbedPane.setEnabled(true);
                        this.EncryptingButton.setEnabled(false);
                        if(this.jCheckBox_OpenFile.isSelected())
                            this.jCheckBox_OpenFile.setSelected(false);
                        this.DecSen=null;
                    }
                    else if(this.DecSen.WrongPassword()){
                        this.setEnabled(false);
                        this.jDialog_WrongPassword.setVisible(true);
                        if(this.jCheckBox_OpenFile.isSelected())
                            this.jCheckBox_OpenFile.setSelected(false);
                        this.DecSen=null;
                    }
                    else if(this.DecSen.NoEnoughFreeSpace()){
                        this.setEnabled(false);
                        this.jDialog_NoEnoughFreeSpace.setVisible(true);
                        if(this.jCheckBox_OpenFile.isSelected())
                            this.jCheckBox_OpenFile.setSelected(false);
                        this.DecSen=null;
                    }
                    else{
                        this.setEnabled(false);
                        if(this.jCheckBox_OpenFile.isSelected()){
                            this.jCheckBox_OpenFile.setSelected(false);
                            this.EncryptingButton.setEnabled(false);
                            this.jTabbedPane.setEnabled(true);
                            this.setEnabled(true);
                            this.DecSen=null;
                        }
                        else
                            this.jDialog_Done.setVisible(true);
                    }
                }
            }
            else if(progress>0 && !this.jButton_Cancel.isEnabled())
                this.jButton_Cancel.setEnabled(true);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel AboutPanel;
    private javax.swing.JButton DecryptingBrowserButton;
    private javax.swing.JButton DecryptingButton;
    private javax.swing.JPanel DecryptingPanel;
    private javax.swing.JPasswordField DecryptingPassword;
    private javax.swing.JButton EncryptingBrowserButton;
    private javax.swing.JButton EncryptingButton;
    private javax.swing.JPanel EncryptingPanel;
    private javax.swing.JPasswordField EncryptingPassword1;
    private javax.swing.JPasswordField EncryptingPassword2;
    private javax.swing.JButton jButton_Cancel;
    private javax.swing.JButton jButton_Done;
    private javax.swing.JButton jButton_InputParameterFileNotFound;
    private javax.swing.JButton jButton_NoEnoughFreeSpace;
    private javax.swing.JButton jButton_PasswordsDoNotMatch;
    private javax.swing.JButton jButton_WrongPassword;
    private javax.swing.JButton jButton__ChosenFileEmty;
    private javax.swing.JCheckBox jCheckBox_OpenFile;
    private javax.swing.JDialog jDialog_ChosenFileEmty;
    private javax.swing.JDialog jDialog_Done;
    private javax.swing.JDialog jDialog_InputParameterFileNotFound;
    private javax.swing.JDialog jDialog_NoEnoughFreeSpace;
    private javax.swing.JDialog jDialog_PasswordsDoNotMatch;
    private javax.swing.JDialog jDialog_WrongPassword;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JProgressBar jProgressBar;
    private javax.swing.JTabbedPane jTabbedPane;
    // End of variables declaration//GEN-END:variables
}