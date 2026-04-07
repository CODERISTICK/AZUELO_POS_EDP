/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pointofsale.Screens;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import pointofsale.DBConnection;

public class LoginScreen extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(LoginScreen.class.getName());
    
    private char defaultEchoChar;

   
    public LoginScreen() {
        initComponents();
        defaultEchoChar = password_txt.getEchoChar();
    }

    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        showPassword_chb = new javax.swing.JCheckBox();
        btn_Login = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        Username_txt = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        password_txt = new javax.swing.JPasswordField();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(498, 276));
        setMinimumSize(new java.awt.Dimension(498, 276));
        setPreferredSize(new java.awt.Dimension(515, 319));
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(13, 52, 91));
        jPanel1.setForeground(new java.awt.Color(0, 0, 0));
        jPanel1.setMaximumSize(new java.awt.Dimension(627, 348));
        jPanel1.setMinimumSize(new java.awt.Dimension(627, 348));
        jPanel1.setPreferredSize(new java.awt.Dimension(627, 348));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        showPassword_chb.setForeground(new java.awt.Color(255, 255, 255));
        showPassword_chb.setText("Show Password");
        showPassword_chb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showPassword_chbActionPerformed(evt);
            }
        });
        jPanel1.add(showPassword_chb, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 200, 130, 30));

        btn_Login.setBackground(new java.awt.Color(7, 163, 46));
        btn_Login.setForeground(new java.awt.Color(0, 0, 0));
        btn_Login.setText("LOGIN");
        btn_Login.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_LoginActionPerformed(evt);
            }
        });
        jPanel1.add(btn_Login, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 230, 250, 40));

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/padlock (1).png"))); // NOI18N
        jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 160, 30, 40));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/user.png"))); // NOI18N
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 90, -1, 40));

        Username_txt.setBackground(new java.awt.Color(102, 102, 102));
        Username_txt.setForeground(new java.awt.Color(255, 255, 255));
        Username_txt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Username_txtActionPerformed(evt);
            }
        });
        jPanel1.add(Username_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 90, 250, 40));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Password");
        jPanel1.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 140, 70, -1));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Username");
        jPanel1.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 60, 80, 30));

        jLabel2.setBackground(new java.awt.Color(255, 255, 255));
        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Welcome To StockWise");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 10, 260, -1));

        password_txt.setBackground(new java.awt.Color(102, 102, 102));
        password_txt.setForeground(new java.awt.Color(255, 255, 255));
        jPanel1.add(password_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 160, 250, 40));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale/Screens/giphy.gif"))); // NOI18N
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btn_LoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_LoginActionPerformed
         String username = Username_txt.getText().trim();
        String password = String.valueOf(password_txt.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.");
            return;
        }

        try {
            Connection con = DBConnection.getConnection();

            String sql = "SELECT * FROM users WHERE username=?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, username);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");

                boolean matched;

                // NEW HASHED PASSWORD
                if (storedPassword != null && storedPassword.contains(":")) {
                    matched = pointofsale.PasswordUtils.verifyPassword(password, storedPassword);
                } // OLD PLAIN TEXT PASSWORD
                else {
                    matched = password.equals(storedPassword);
                }

                if (matched) {
                    String firstName = rs.getString("first_name");
                    String middleName = rs.getString("middle_name");
                    String lastName = rs.getString("last_name");
                    String role = rs.getString("role");
                    String status = rs.getString("status");

                    if (!"Active".equalsIgnoreCase(status)) {
                        JOptionPane.showMessageDialog(this, "This account is inactive.");
                        return;
                    }

                    JOptionPane.showMessageDialog(this, "Login Successfully!");

                    String welcomeName = firstName + " " + lastName;

                    String fullName;
                    if (middleName != null && !middleName.trim().isEmpty()) {
                        fullName = firstName + " " + middleName + " " + lastName;
                    } else {
                        fullName = firstName + " " + lastName;
                    }

                    MainPanel mp = new MainPanel(username, welcomeName, fullName, role);
                    mp.setVisible(true);
                    this.dispose();

                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password.");
                }

            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
        }
    }//GEN-LAST:event_btn_LoginActionPerformed

    private void Username_txtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Username_txtActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_Username_txtActionPerformed

    private void showPassword_chbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showPassword_chbActionPerformed
        // TODO add your handling code here:
        //code here
        if (showPassword_chb.isSelected()) {
            password_txt.setEchoChar((char) 0); // show
        } else {
            password_txt.setEchoChar(defaultEchoChar); // hide
        }
    }//GEN-LAST:event_showPassword_chbActionPerformed

  
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new LoginScreen().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField Username_txt;
    private javax.swing.JButton btn_Login;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPasswordField password_txt;
    private javax.swing.JCheckBox showPassword_chb;
    // End of variables declaration//GEN-END:variables
}
