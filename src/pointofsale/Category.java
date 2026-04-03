package pointofsale;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class Category {

    Connection con;

    public Category() {
        con = DBConnection.getConnection();
    }

    // CHECK DUPLICATE CATEGORY NAME
    public boolean isCategoryNameExists(String name) {
        if (con == null) {
            System.err.println("Database connection is null");
            return false;
        }
        
        try {
            String sql = "SELECT COUNT(*) FROM categories WHERE name = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, name);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            rs.close();
            pst.close();
        } catch (Exception e) {
            System.err.println("Error checking category name: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // CHECK DUPLICATE CATEGORY NAME (FOR UPDATE)
    public boolean isCategoryNameExists(String name, String oldName) {
        if (con == null) {
            System.err.println("Database connection is null");
            return false;
        }
        
        try {
            String sql = "SELECT COUNT(*) FROM categories WHERE name = ? AND name != ?";
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
            System.err.println("Error checking category name: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // LOAD ALL CATEGORIES
    public void loadCategories(JTable table) {

        try {

            String sql = "SELECT * FROM categories ORDER BY category_id ASC";

            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            while (rs.next()) {

                model.addRow(new Object[]{
                    rs.getInt("category_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("created_at"),
                    rs.getString("updated_at")
                });

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

          // ADD CATEGORY
    public void addCategory(String name, String description) {

        name = name.trim();
        description = description.trim();

        if (name.isEmpty() || description.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Category Name and Description cannot be empty.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {

            String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";

            PreparedStatement pst = con.prepareStatement(sql);

            pst.setString(1, name);
            pst.setString(2, description);

            int rowsInserted = pst.executeUpdate();

            if (rowsInserted > 0) {
                JOptionPane.showMessageDialog(null, "Category successfully added!");
            } else {
                JOptionPane.showMessageDialog(null, "Category was not added.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error adding category: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // UPDATE CATEGORY
    public void updateCategory(int id, String name, String description) {

        try {

            String sql = "UPDATE categories SET name=?, description=? WHERE category_id=?";

            PreparedStatement pst = con.prepareStatement(sql);

            pst.setString(1, name);
            pst.setString(2, description);
            pst.setInt(3, id);

            pst.executeUpdate();

            JOptionPane.showMessageDialog(null, "Category updated successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // DELETE CATEGORY
    // DELETE CATEGORY (SAFE DELETE)
    // DELETE CATEGORY
public void deleteCategory(int id) {

        try {
            // Check first if there are products using this category
            String checkSql = "SELECT COUNT(*) FROM products WHERE category_id = ?";
            PreparedStatement checkPst = con.prepareStatement(checkSql);
            checkPst.setInt(1, id);

            ResultSet rs = checkPst.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(
                        null,
                        "Cannot delete this category because there are existing products under it.",
                        "Delete Not Allowed",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            // Delete only if no products are using it
            String sql = "DELETE FROM categories WHERE category_id = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, id);

            int rowsDeleted = pst.executeUpdate();

            if (rowsDeleted > 0) {
                JOptionPane.showMessageDialog(null, "Category deleted successfully!");
            } else {
                JOptionPane.showMessageDialog(null, "Category not found.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Error deleting category: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
    }

    // SEARCH CATEGORY (LIVE SEARCH)
    public void searchCategory(JTable table, String keyword) {

        try {

            String sql = "SELECT * FROM categories WHERE name LIKE ? ORDER BY category_id ASC";

            PreparedStatement pst = con.prepareStatement(sql);

            pst.setString(1, "%" + keyword + "%");

            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            while (rs.next()) {

                model.addRow(new Object[]{
                    rs.getInt("category_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("created_at"),
                    rs.getString("updated_at")
                });

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}