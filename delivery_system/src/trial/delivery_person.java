/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package trial;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDate;

public class delivery_person extends JFrame {
    private Connection con;
    private PreparedStatement ps;
    private ResultSet rs;
    public String restaurantName;
    private JTextArea textArea;
    private JButton confirmButton;
    public String username;
    public String uid;
    public String orderId;
    public String deliveryPartnerId;
    public String dpId;

    public delivery_person(String restaurantName, String username, String orderId) {
        this.restaurantName = restaurantName;
        this.username = username;
        this.orderId = orderId;
        initComponents(); // Your existing method
        customInitComponents(); // New method to add additional functionality
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:XE", "system", "kushal");
            allocateDeliveryPerson();
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(delivery_person.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void customInitComponents() {
        setTitle("Delivery Person Allocation");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel label = new JLabel("Delivery Person Allocation", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 18));
        add(label, BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel confirmationPanel = new JPanel();
        confirmationPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        JLabel confirmationLabel = new JLabel("Food is being delivered...");
        confirmationPanel.add(confirmationLabel);

        confirmButton = new JButton("Press to confirm");
        confirmButton.setVisible(false); // Initially invisible
        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmDelivery();
            }
        });
        confirmationPanel.add(confirmButton);

        add(confirmationPanel, BorderLayout.SOUTH);
    }

    private void allocateDeliveryPerson() {
        LocalDate currentDate = LocalDate.now();
        java.sql.Date sqlDate = java.sql.Date.valueOf(currentDate);

        try {
            // Retrieve the delivery partner ID using the restaurant name
            String sql = "SELECT DELV_PART_ID FROM restaurant WHERE REST_NAME = ?";
            ps = con.prepareStatement(sql);
            ps.setString(1, restaurantName);
            rs = ps.executeQuery();

            if (rs.next()) {
                deliveryPartnerId = rs.getString("DELV_PART_ID");
            }

            if (deliveryPartnerId != null) {
                // Check for an available delivery person
                sql = "SELECT DP_ID, DP_NAME, DP_PHONE, DP_VEHICLE_NO FROM delivery_person " +
                        "WHERE DP_PART_ID = ? AND DP_AVAILABILITY = 'yes' ORDER BY DP_NAME";
                ps = con.prepareStatement(sql);
                ps.setString(1, deliveryPartnerId);
                rs = ps.executeQuery();

                if (rs.next()) {
                    dpId = rs.getString("DP_ID");
                    String dpName = rs.getString("DP_NAME");
                    String dpPhone = rs.getString("DP_PHONE");
                    String dpVehicleNo = rs.getString("DP_VEHICLE_NO");

                    // Update the availability of the selected delivery person
                    sql = "UPDATE Delivery_Person SET DP_AVAILABILITY = 'no' WHERE DP_ID = ?";
                    ps = con.prepareStatement(sql);
                    ps.setString(1, dpId);
                    ps.executeUpdate();

                    // Display the delivery person's details in the text area
                    textArea.setText(
                            "Delivery Person Assigned:\n" +
                                    "Name: " + dpName + "\n" +
                                    "Phone: " + dpPhone + "\n" +
                                    "Vehicle No: " + dpVehicleNo
                    );

                    // Show the confirmation message and button after 30 seconds
                    Timer timer = new Timer(10000, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            showConfirmationMessage();
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();

                    // Retrieve user ID and insert order details
                    insertOrderDetails(deliveryPartnerId, dpId, sqlDate);
                } else {
                    textArea.setText("No available delivery person found.");
                }
            } else {
                textArea.setText("No delivery partner found for the specified restaurant.");
            }
        } catch (SQLException ex) {
            Logger.getLogger(delivery_person.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void insertOrderDetails(String deliveryPartnerId, String dpId, java.sql.Date sqlDate) {
        try {
            // Retrieve user ID
            String sql = "SELECT USER_ID FROM users WHERE USER_NAME = ?";
            ps = con.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();
            if (rs.next()) {
                uid = rs.getString("USER_ID");
            } else {
                textArea.setText("User ID not found for the specified username.");
                return;
            }

            // Insert order details
            if (deliveryPartnerId != null && dpId != null && uid != null) {
                String sql2 = "INSERT INTO orderss VALUES (?, ?, ?, ?, ?, ?)";
                ps = con.prepareStatement(sql2);
                ps.setString(1, orderId);
                ps.setString(2, deliveryPartnerId);
                ps.setDate(3, sqlDate);
                ps.setDate(4, sqlDate);
                ps.setString(5, uid);
                ps.setString(6, dpId);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(delivery_person.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void showConfirmationMessage() {
        textArea.setText("Food is delivered.");
        confirmButton.setVisible(true); // Show the confirm button
    }

    private void confirmDelivery() {
        try {
            // Execute trigger to update DP_AVAILABILITY from 'no' to 'yes'
            String sql = "UPDATE Delivery_Person SET DP_AVAILABILITY = 'yes' WHERE DP_AVAILABILITY = 'no'";
            ps = con.prepareStatement(sql);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Delivery confirmed successfully!");
            dispose(); // Close the JFrame after confirmation
        } catch (SQLException ex) {
            Logger.getLogger(delivery_person.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } finally {
            // Close resources
            try {
                if (ps != null) ps.close();
                if (con != null) con.close();
            } catch (SQLException ex) {
                Logger.getLogger(delivery_person.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public void main(String args[]) {
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
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(delivery_person.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(delivery_person.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(delivery_person.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(delivery_person.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new delivery_person(restaurantName,username,orderId).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
