package pointofsale;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class Products {

    Connection con;

    public Products() {
        con = DBConnection.getConnection();
        if (con == null) {
            System.err.println("Failed to connect to database in Products constructor");
        }
    }

    // TEST DATABASE CONNECTION
    public boolean testConnection() {
        try {
            if (con == null) {
                System.err.println("Connection is null");
                return false;
            }
            if (con.isClosed()) {
                System.err.println("Connection is closed");
                return false;
            }
            // Test a simple query
            String sql = "SELECT 1";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                rs.close();
                pst.close();
                return true;
            }
        } catch (Exception e) {
            System.err.println("Database test failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // LOAD CATEGORY TO COMBOBOX
    public void loadCategoriesToComboBox(JComboBox<String> combo) {
        try {
            combo.removeAllItems();
            combo.addItem("SELECT CATEGORY");

            String sql = "SELECT name FROM categories ORDER BY name ASC";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                combo.addItem(rs.getString("name"));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading categories: " + e.getMessage());
        }
    }

    // LOAD SUPPLIER TO COMBOBOX
    public void loadSuppliersToComboBox(JComboBox<String> combo) {
        try {
            combo.removeAllItems();
            combo.addItem("SELECT SUPPLIER");

            String sql = "SELECT name FROM suppliers ORDER BY name ASC";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                combo.addItem(rs.getString("name"));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading suppliers: " + e.getMessage());
        }
    }

    // LOAD CATEGORY FILTER COMBO
    public void loadCategoryFilterCombo(JComboBox<String> combo) {
        try {
            combo.removeAllItems();
            combo.addItem("ALL");

            String sql = "SELECT name FROM categories ORDER BY name ASC";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                combo.addItem(rs.getString("name"));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading category filter: " + e.getMessage());
        }
    }

    // LOAD SUPPLIER FILTER COMBO
    public void loadSupplierFilterCombo(JComboBox<String> combo) {
        try {
            combo.removeAllItems();
            combo.addItem("ALL");

            String sql = "SELECT name FROM suppliers ORDER BY name ASC";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                combo.addItem(rs.getString("name"));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading supplier filter: " + e.getMessage());
        }
    }

    // CHECK DUPLICATE BARCODE
    public boolean isBarcodeExists(String barcode) {
        try {
            String sql = "SELECT COUNT(*) FROM products WHERE barcode = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, barcode);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error checking barcode: " + e.getMessage());
        }
        return false;
    }

    // CHECK DUPLICATE PRODUCT NAME
    public boolean isProductNameExists(String productName) {
        if (con == null) {
            System.err.println("Database connection is null");
            return false;
        }
        
        try {
            // Try with 'name' column first (most likely)
            String sql = "SELECT COUNT(*) FROM products WHERE name = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, productName);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            // If 'name' column doesn't exist, try 'product_name'
            try {
                String sql = "SELECT COUNT(*) FROM products WHERE product_name = ?";
                PreparedStatement pst = con.prepareStatement(sql);
                pst.setString(1, productName);
                ResultSet rs = pst.executeQuery();

                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            } catch (Exception e2) {
                System.err.println("Error checking product name with both column names: " + e2.getMessage());
                e2.printStackTrace();
                return false;
            }
        }
        return false;
    }

    // CHECK DUPLICATE PRODUCT NAME (FOR UPDATE)
    public boolean isProductNameExists(String productName, int excludeProductId) {
        if (con == null) {
            System.err.println("Database connection is null");
            return false;
        }
        
        try {
            // Try with 'name' column first (most likely)
            String sql = "SELECT COUNT(*) FROM products WHERE name = ? AND product_id != ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, productName);
            pst.setInt(2, excludeProductId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            // If 'name' column doesn't exist, try 'product_name'
            try {
                String sql = "SELECT COUNT(*) FROM products WHERE product_name = ? AND product_id != ?";
                PreparedStatement pst = con.prepareStatement(sql);
                pst.setString(1, productName);
                pst.setInt(2, excludeProductId);
                ResultSet rs = pst.executeQuery();

                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            } catch (Exception e2) {
                System.err.println("Error checking product name with both column names: " + e2.getMessage());
                e2.printStackTrace();
                return false;
            }
        }
        return false;
    }

    // GENERATE UNIQUE BARCODE
    public String generateUniqueBarcode() {
        String barcode;
        do {
            long value = System.currentTimeMillis() % 1000000000000L;
            barcode = String.valueOf(value);
        } while (isBarcodeExists(barcode));

        return barcode;
    }

    // GET CATEGORY ID BY NAME
    public int getCategoryIdByName(String categoryName) {
        try {
            String sql = "SELECT category_id FROM categories WHERE name = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, categoryName);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt("category_id");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error getting category ID: " + e.getMessage());
        }
        return -1;
    }

    // GET SUPPLIER ID BY NAME
    public int getSupplierIdByName(String supplierName) {
        try {
            String sql = "SELECT supplier_id FROM suppliers WHERE name = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, supplierName);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt("supplier_id");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error getting supplier ID: " + e.getMessage());
        }
        return -1;
    }

    // LOAD ALL PRODUCTS
    public void loadProducts(JTable table) {
        try {
            String sql =
                "SELECT p.product_id, p.barcode, p.name, c.name AS category_name, s.name AS supplier_name, " +
                "p.cost_price, p.selling_price, p.stock_quantity, p.reorder_level, p.product_image, " +
                "CASE " +
                "   WHEN p.stock_quantity = 0 THEN 'OUT OF STOCK' " +
                "   WHEN p.stock_quantity <= p.reorder_level THEN 'LOW STOCK' " +
                "   ELSE 'IN STOCK' " +
                "END AS stock_status, " +
                "p.created_at, p.updated_at " +
                "FROM products p " +
                "INNER JOIN categories c ON p.category_id = c.category_id " +
                "INNER JOIN suppliers s ON p.supplier_id = s.supplier_id " +
                "ORDER BY p.product_id ASC";

            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("product_id"),
                    rs.getString("barcode"),
                    rs.getString("name"),
                    rs.getString("category_name"),
                    rs.getString("supplier_name"),
                    rs.getDouble("cost_price"),
                    rs.getDouble("selling_price"),
                    rs.getString("product_image"),
                    rs.getInt("stock_quantity"),
                    rs.getInt("reorder_level"),
                    rs.getString("stock_status"),
                    rs.getString("created_at"),
                    rs.getString("updated_at")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading products: " + e.getMessage());
        }
    }

    // ADD PRODUCT
    public void addProduct(String barcode, String name, String categoryName, String supplierName,
                           double costPrice, double sellingPrice, int stockQty, int reorderLevel,
                           String imagePath) {
        try {
            int categoryId = getCategoryIdByName(categoryName);
            int supplierId = getSupplierIdByName(supplierName);

            if (categoryId == -1 || supplierId == -1) {
                JOptionPane.showMessageDialog(null, "Invalid category or supplier.");
                return;
            }

            String sql = "INSERT INTO products (barcode, name, category_id, supplier_id, cost_price, selling_price, stock_quantity, reorder_level, product_image) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pst = con.prepareStatement(sql);

            pst.setString(1, barcode);
            pst.setString(2, name);
            pst.setInt(3, categoryId);
            pst.setInt(4, supplierId);
            pst.setDouble(5, costPrice);
            pst.setDouble(6, sellingPrice);
            pst.setInt(7, stockQty);
            pst.setInt(8, reorderLevel);
            pst.setString(9, imagePath);

            pst.executeUpdate();

            JOptionPane.showMessageDialog(null, "Product added successfully!");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error adding product: " + e.getMessage());
        }
    }

    // UPDATE PRODUCT
    public void updateProduct(int productId, String barcode, String name, String categoryName, String supplierName,
                              double costPrice, double sellingPrice, int stockQty, int reorderLevel,
                              String imagePath) {
        try {
            int categoryId = getCategoryIdByName(categoryName);
            int supplierId = getSupplierIdByName(supplierName);

            if (categoryId == -1 || supplierId == -1) {
                JOptionPane.showMessageDialog(null, "Invalid category or supplier.");
                return;
            }

            String sql = "UPDATE products SET barcode=?, name=?, category_id=?, supplier_id=?, cost_price=?, selling_price=?, stock_quantity=?, reorder_level=?, product_image=? WHERE product_id=?";
            PreparedStatement pst = con.prepareStatement(sql);

            pst.setString(1, barcode);
            pst.setString(2, name);
            pst.setInt(3, categoryId);
            pst.setInt(4, supplierId);
            pst.setDouble(5, costPrice);
            pst.setDouble(6, sellingPrice);
            pst.setInt(7, stockQty);
            pst.setInt(8, reorderLevel);
            pst.setString(9, imagePath);
            pst.setInt(10, productId);

            pst.executeUpdate();

            JOptionPane.showMessageDialog(null, "Product updated successfully!");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error updating product: " + e.getMessage());
        }
    }

    // DELETE PRODUCT
    public void deleteProduct(int productId) {
        try {
            String sql = "DELETE FROM products WHERE product_id = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, productId);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(null, "Product deleted successfully!");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error deleting product: " + e.getMessage());
        }
    }

    // SEARCH + FILTER
    public void filterProducts(JTable table, String keyword, String stockStatus, String supplierName, String categoryName) {
        try {
            String sql =
                "SELECT p.product_id, p.barcode, p.name, c.name AS category_name, s.name AS supplier_name, " +
                "p.cost_price, p.selling_price, p.stock_quantity, p.reorder_level, p.product_image, " +
                "CASE " +
                "   WHEN p.stock_quantity = 0 THEN 'OUT OF STOCK' " +
                "   WHEN p.stock_quantity <= p.reorder_level THEN 'LOW STOCK' " +
                "   ELSE 'IN STOCK' " +
                "END AS stock_status, " +
                "p.created_at, p.updated_at " +
                "FROM products p " +
                "INNER JOIN categories c ON p.category_id = c.category_id " +
                "INNER JOIN suppliers s ON p.supplier_id = s.supplier_id " +
                "WHERE (p.barcode LIKE ? OR p.name LIKE ? OR c.name LIKE ? OR s.name LIKE ?) " +
                "AND (? = 'ALL' OR " +
                "    CASE " +
                "       WHEN p.stock_quantity = 0 THEN 'OUT OF STOCK' " +
                "       WHEN p.stock_quantity <= p.reorder_level THEN 'LOW STOCK' " +
                "       ELSE 'IN STOCK' " +
                "    END = ?) " +
                "AND (? = 'ALL' OR s.name = ?) " +
                "AND (? = 'ALL' OR c.name = ?) " +
                "ORDER BY p.product_id ASC";

            PreparedStatement pst = con.prepareStatement(sql);

            String key = "%" + keyword + "%";
            pst.setString(1, key);
            pst.setString(2, key);
            pst.setString(3, key);
            pst.setString(4, key);

            pst.setString(5, stockStatus);
            pst.setString(6, stockStatus);

            pst.setString(7, supplierName);
            pst.setString(8, supplierName);

            pst.setString(9, categoryName);
            pst.setString(10, categoryName);

            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("product_id"),
                    rs.getString("barcode"),
                    rs.getString("name"),
                    rs.getString("category_name"),
                    rs.getString("supplier_name"),
                    rs.getDouble("cost_price"),
                    rs.getDouble("selling_price"),
                    rs.getString("product_image"),
                    rs.getInt("stock_quantity"),
                    rs.getInt("reorder_level"),
                    rs.getString("stock_status"),
                    rs.getString("created_at"),
                    rs.getString("updated_at")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error filtering products: " + e.getMessage());
        }
    }
}