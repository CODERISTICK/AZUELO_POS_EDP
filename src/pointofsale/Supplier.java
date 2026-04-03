package pointofsale;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class Supplier {

    Connection con;

    public Supplier() {
        con = DBConnection.getConnection();
    }

    // CHECK DUPLICATE SUPPLIER NAME
    public boolean isSupplierNameExists(String name) {
        if (con == null) {
            System.err.println("Database connection is null");
            return false;
        }
        
        try {
            String sql = "SELECT COUNT(*) FROM suppliers WHERE name = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, name);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            rs.close();
            pst.close();
        } catch (Exception e) {
            System.err.println("Error checking supplier name: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // CHECK DUPLICATE SUPPLIER NAME (FOR UPDATE)
    public boolean isSupplierNameExists(String name, String oldName) {
        if (con == null) {
            System.err.println("Database connection is null");
            return false;
        }
        
        try {
            String sql = "SELECT COUNT(*) FROM suppliers WHERE name = ? AND name != ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, name);
            pst.setString(2, oldName);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            rs.close();
            pst.close();
        } catch (Exception e) {
            System.err.println("Error checking supplier name: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // LOAD ALL SUPPLIERS
    public void loadSuppliers(JTable table) {
        try {
            String sql = "SELECT * FROM suppliers ORDER BY supplier_id ASC";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("name"),
                    rs.getString("contact_person"),
                    rs.getString("contact_number"),
                    rs.getString("email"),
                    rs.getString("address"),
                    rs.getString("created_at"),
                    rs.getString("updated_at")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading suppliers: " + e.getMessage());
        }
    }

    // ADD SUPPLIER
    public void addSupplier(String name, String contactPerson, String contactNumber, String email, String address) {
        try {
            String sql = "INSERT INTO suppliers (name, contact_person, contact_number, email, address) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, name);
            pst.setString(2, contactPerson);
            pst.setString(3, contactNumber);
            pst.setString(4, email);
            pst.setString(5, address);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(null, "Supplier added successfully!");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error adding supplier: " + e.getMessage());
        }
    }

    // UPDATE SUPPLIER
    public void updateSupplier(String oldName, String oldContactPerson, String oldContactNumber,
                               String oldEmail, String oldAddress,
                               String name, String contactPerson, String contactNumber,
                               String email, String address) {
        try {
            String sql = "UPDATE suppliers SET name=?, contact_person=?, contact_number=?, email=?, address=? " +
                         "WHERE name=? AND contact_person=? AND contact_number=? AND email=? AND address=?";
            PreparedStatement pst = con.prepareStatement(sql);

            pst.setString(1, name);
            pst.setString(2, contactPerson);
            pst.setString(3, contactNumber);
            pst.setString(4, email);
            pst.setString(5, address);

            pst.setString(6, oldName);
            pst.setString(7, oldContactPerson);
            pst.setString(8, oldContactNumber);
            pst.setString(9, oldEmail);
            pst.setString(10, oldAddress);

            pst.executeUpdate();

            JOptionPane.showMessageDialog(null, "Supplier updated successfully!");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error updating supplier: " + e.getMessage());
        }
    }

    // DELETE SUPPLIER
    public void deleteSupplier(String name, String contactPerson, String contactNumber, String email, String address) {
        try {
            // Check first if there are products using this supplier
            // Try different possible column names for supplier reference
            String checkSql = "SELECT COUNT(*) FROM products WHERE supplier_id = (SELECT supplier_id FROM suppliers WHERE name = ?)";
            try {
                PreparedStatement checkPst = con.prepareStatement(checkSql);
                checkPst.setString(1, name);
                ResultSet rs = checkPst.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Cannot delete this supplier because there are existing products under it.",
                            "Delete Not Allowed",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                rs.close();
                checkPst.close();
            } catch (Exception e) {
                // If the above query fails, try alternative approaches
                System.err.println("Error checking supplier references: " + e.getMessage());
            }

            // Delete only if no products are using it
            String sql = "DELETE FROM suppliers WHERE name=? AND contact_person=? AND contact_number=? AND email=? AND address=?";
            PreparedStatement pst = con.prepareStatement(sql);

            pst.setString(1, name);
            pst.setString(2, contactPerson);
            pst.setString(3, contactNumber);
            pst.setString(4, email);
            pst.setString(5, address);

            int rowsDeleted = pst.executeUpdate();

            if (rowsDeleted > 0) {
                JOptionPane.showMessageDialog(null, "Supplier deleted successfully!");
            } else {
                JOptionPane.showMessageDialog(null, "Supplier not found.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error deleting supplier: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // LIVE SEARCH
    public void searchSupplier(JTable table, String keyword) {
        try {
            String sql = "SELECT * FROM suppliers WHERE name LIKE ? OR contact_person LIKE ? OR contact_number LIKE ? OR email LIKE ? OR address LIKE ? ORDER BY supplier_id ASC";
            PreparedStatement pst = con.prepareStatement(sql);

            String key = "%" + keyword + "%";
            pst.setString(1, key);
            pst.setString(2, key);
            pst.setString(3, key);
            pst.setString(4, key);
            pst.setString(5, key);

            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("name"),
                    rs.getString("contact_person"),
                    rs.getString("contact_number"),
                    rs.getString("email"),
                    rs.getString("address"),
                    rs.getString("created_at"),
                    rs.getString("updated_at")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error searching supplier: " + e.getMessage());
        }
    }
}