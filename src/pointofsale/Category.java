package pointofsale;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class Category {

    Connection con;

    public Category() {
        con = DBConnection.getConnection();
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
    public void deleteCategory(int id) {

        try {

            String sql = "DELETE FROM categories WHERE category_id=?";

            PreparedStatement pst = con.prepareStatement(sql);

            pst.setInt(1, id);

            pst.executeUpdate();

            JOptionPane.showMessageDialog(null, "Category deleted successfully!");

        } catch (Exception e) {
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