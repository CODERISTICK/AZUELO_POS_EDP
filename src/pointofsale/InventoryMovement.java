package pointofsale;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Date;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class InventoryMovement {

    Connection con;

    public InventoryMovement() {
        con = DBConnection.getConnection();
    }

    // =========================
    // PRODUCT DETAILS CLASS
    // =========================
    public static class ProductDetails {

        public int productId;
        public String productName;
        public String barcode;
        public String supplierName;
        public String categoryName;
        public int stockQty;
        public int reorderLevel;
        public String imagePath;
        public String status;
    }

    // =========================
    // STOCK IN DETAILS CLASS
    // =========================
    public static class StockInDetails {

        public int transactionId;
        public String productName;
        public String barcode;
        public String supplierName;
        public int quantity;
        public String referenceNumber;
        public java.sql.Date transactionDate;
        public String remarks;
        public String imagePath;
        public int currentStock;
        public int reorderLevel;
        public String status;
    }

    // =========================
    // STOCK OUT DETAILS CLASS
    // =========================
    public static class StockOutDetails {

        public int transactionId;
        public String productName;
        public String barcode;
        public int quantity;
        public String reason;
        public String referenceNumber;
        public java.sql.Date transactionDate;
        public String remarks;
        public String imagePath;
        public int currentStock;
        public int reorderLevel;
        public String status;
    }

    // =========================
    // LOAD PRODUCTS TO COMBOBOX
    // =========================
    public void loadProductsToComboBox(JComboBox<String> combo) {
        try {
            combo.removeAllItems();
            combo.addItem("SELECT PRODUCT");

            String sql = "SELECT name FROM products ORDER BY name ASC";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                combo.addItem(rs.getString("name"));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading products: " + e.getMessage());
        }
    }

    // =========================
    // GET PRODUCT DETAILS BY NAME
    // =========================
    public ProductDetails getProductDetailsByName(String productName) {
        ProductDetails details = null;

        try {
            String sql
                    = "SELECT p.product_id, p.name, p.barcode, p.stock_quantity, p.reorder_level, p.product_image, "
                    + "c.name AS category_name, s.name AS supplier_name "
                    + "FROM products p "
                    + "INNER JOIN categories c ON p.category_id = c.category_id "
                    + "INNER JOIN suppliers s ON p.supplier_id = s.supplier_id "
                    + "WHERE p.name = ?";

            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, productName);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                details = new ProductDetails();
                details.productId = rs.getInt("product_id");
                details.productName = rs.getString("name");
                details.barcode = rs.getString("barcode");
                details.supplierName = rs.getString("supplier_name");
                details.categoryName = rs.getString("category_name");
                details.stockQty = rs.getInt("stock_quantity");
                details.reorderLevel = rs.getInt("reorder_level");
                details.imagePath = rs.getString("product_image");
                details.status = getStockStatus(details.stockQty, details.reorderLevel);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error getting product details: " + e.getMessage());
        }

        return details;
    }

    // =========================
    // GET PRODUCT ID BY NAME
    // =========================
    public int getProductIdByName(String productName) {
        int productId = -1;

        try {
            String sql = "SELECT product_id FROM products WHERE name = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, productName);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                productId = rs.getInt("product_id");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error getting product ID: " + e.getMessage());
        }

        return productId;
    }

    // =========================
    // STOCK STATUS
    // =========================
    public String getStockStatus(int stockQty, int reorderLevel) {
        if (stockQty == 0) {
            return "OUT OF STOCK";
        } else if (stockQty <= reorderLevel) {
            return "LOW STOCK";
        } else {
            return "IN STOCK";
        }
    }

    // =========================
    // GET CURRENT STOCK
    // =========================
    public int getCurrentStockByProductId(int productId) {
        int stock = 0;

        try {
            String sql = "SELECT stock_quantity FROM products WHERE product_id = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, productId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                stock = rs.getInt("stock_quantity");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error getting current stock: " + e.getMessage());
        }

        return stock;
    }

    // =========================
    // SAVE STOCK IN
    // =========================
    public void saveStockIn(int productId, int userId, int quantity,
            String referenceNumber, java.util.Date transactionDate,
            String remarks) {
        try {
            con.setAutoCommit(false);

            String insertSql
                    = "INSERT INTO inventory_transactions "
                    + "(product_id, user_id, transaction_type, quantity, reference_number, transaction_date, reason, remarks) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement pstInsert = con.prepareStatement(insertSql);
            pstInsert.setInt(1, productId);
            pstInsert.setInt(2, userId);
            pstInsert.setString(3, "STOCK IN");
            pstInsert.setInt(4, quantity);
            pstInsert.setString(5, referenceNumber);
            pstInsert.setDate(6, new Date(transactionDate.getTime()));
            pstInsert.setString(7, "STOCK IN");
            pstInsert.setString(8, remarks);
            pstInsert.executeUpdate();

            String updateSql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE product_id = ?";
            PreparedStatement pstUpdate = con.prepareStatement(updateSql);
            pstUpdate.setInt(1, quantity);
            pstUpdate.setInt(2, productId);
            pstUpdate.executeUpdate();

            con.commit();
            con.setAutoCommit(true);

            JOptionPane.showMessageDialog(null, "Stock In saved successfully.");

        } catch (Exception e) {
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(null, "Error saving Stock In: " + e.getMessage());
        }
    }

    // =========================
    // SAVE STOCK OUT
    // =========================
    public boolean saveStockOut(int productId, int userId, int quantity,
            String referenceNumber, java.util.Date transactionDate,
            String reason, String remarks) {
        try {
            int currentStock = getCurrentStockByProductId(productId);

            if (quantity <= 0) {
                JOptionPane.showMessageDialog(null, "Quantity must be greater than 0.");
                return false;
            }

            if (quantity > currentStock) {
                JOptionPane.showMessageDialog(null, "Insufficient stock. Current stock is only " + currentStock + ".");
                return false;
            }

            con.setAutoCommit(false);

            String insertSql
                    = "INSERT INTO inventory_transactions "
                    + "(product_id, user_id, transaction_type, quantity, reference_number, transaction_date, reason, remarks) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement pstInsert = con.prepareStatement(insertSql);
            pstInsert.setInt(1, productId);
            pstInsert.setInt(2, userId);
            pstInsert.setString(3, "STOCK OUT");
            pstInsert.setInt(4, quantity);
            pstInsert.setString(5, referenceNumber);
            pstInsert.setDate(6, new Date(transactionDate.getTime()));
            pstInsert.setString(7, reason);
            pstInsert.setString(8, remarks);
            pstInsert.executeUpdate();

            String updateSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";
            PreparedStatement pstUpdate = con.prepareStatement(updateSql);
            pstUpdate.setInt(1, quantity);
            pstUpdate.setInt(2, productId);
            pstUpdate.executeUpdate();

            con.commit();
            con.setAutoCommit(true);

            JOptionPane.showMessageDialog(null, "Stock Out saved successfully.");
            return true;

        } catch (Exception e) {
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(null, "Error saving Stock Out: " + e.getMessage());
            return false;
        }
    }

    // =========================
    // LOAD STOCK IN TABLE
    // =========================
    public void loadStockInTable(JTable table, String keyword, String typeFilter) {
        try {
            String sql
                    = "SELECT it.transaction_id, p.name AS product_name, u.username, "
                    + "CASE "
                    + "   WHEN p.stock_quantity = 0 THEN 'OUT OF STOCK' "
                    + "   WHEN p.stock_quantity <= p.reorder_level THEN 'LOW STOCK' "
                    + "   ELSE 'IN STOCK' "
                    + "END AS stock_status, "
                    + "it.quantity, it.reference_number, it.remarks "
                    + "FROM inventory_transactions it "
                    + "INNER JOIN products p ON it.product_id = p.product_id "
                    + "INNER JOIN users u ON it.user_id = u.user_id "
                    + "WHERE it.transaction_type = 'STOCK IN' "
                    + "AND p.name LIKE ? "
                    + "AND (? = 'ALL' OR "
                    + "     CASE "
                    + "         WHEN p.stock_quantity = 0 THEN 'OUT OF STOCK' "
                    + "         WHEN p.stock_quantity <= p.reorder_level THEN 'LOW STOCK' "
                    + "         ELSE 'IN STOCK' "
                    + "     END = ?) "
                    + "ORDER BY it.transaction_id DESC";

            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, "%" + keyword + "%");
            pst.setString(2, typeFilter);
            pst.setString(3, typeFilter);

            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("transaction_id"),
                    rs.getString("product_name"),
                    rs.getString("username"),
                    rs.getString("stock_status"),
                    rs.getInt("quantity"),
                    rs.getString("reference_number"),
                    rs.getString("remarks")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading Stock In table: " + e.getMessage());
        }
    }

    // =========================
    // LOAD STOCK OUT TABLE
    // =========================
    public void loadStockOutTable(JTable table, String keyword) {
        try {
            String sql
                    = "SELECT it.transaction_id, p.name AS product_name, u.username, "
                    + "it.quantity, it.reference_number, it.transaction_date, it.reason, it.remarks "
                    + "FROM inventory_transactions it "
                    + "INNER JOIN products p ON it.product_id = p.product_id "
                    + "INNER JOIN users u ON it.user_id = u.user_id "
                    + "WHERE it.transaction_type = 'STOCK OUT' "
                    + "AND p.name LIKE ? "
                    + "ORDER BY it.transaction_id DESC";

            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, "%" + keyword + "%");

            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("transaction_id"),
                    rs.getString("product_name"),
                    rs.getString("username"),
                    rs.getInt("quantity"),
                    rs.getString("reference_number"),
                    rs.getDate("transaction_date"),
                    rs.getString("reason"),
                    rs.getString("remarks")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading Stock Out table: " + e.getMessage());
        }
    }

    // =========================
    // LOAD STOCK MOVEMENT TABLE
    // =========================
    public void loadStockMovementTable(JTable table, String keyword, String typeFilter) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT p.name AS product_name, c.name AS category_name, s.name AS supplier_name, "
                    + "p.stock_quantity, p.reorder_level, "
                    + "CASE "
                    + "   WHEN p.stock_quantity = 0 THEN 'OUT OF STOCK' "
                    + "   WHEN p.stock_quantity <= p.reorder_level THEN 'LOW STOCK' "
                    + "   ELSE 'IN STOCK' "
                    + "END AS stock_status "
                    + "FROM products p "
                    + "INNER JOIN categories c ON p.category_id = c.category_id "
                    + "INNER JOIN suppliers s ON p.supplier_id = s.supplier_id "
                    + "WHERE p.name LIKE ? "
            );

            if (typeFilter != null && !typeFilter.equalsIgnoreCase("ALL")) {
                if (typeFilter.equalsIgnoreCase("IN STOCK")
                        || typeFilter.equalsIgnoreCase("LOW STOCK")
                        || typeFilter.equalsIgnoreCase("OUT OF STOCK")) {
                    sql.append("AND (CASE ")
                            .append("WHEN p.stock_quantity = 0 THEN 'OUT OF STOCK' ")
                            .append("WHEN p.stock_quantity <= p.reorder_level THEN 'LOW STOCK' ")
                            .append("ELSE 'IN STOCK' END) = ? ");
                }
            }

            sql.append("ORDER BY p.name ASC");

            PreparedStatement pst = con.prepareStatement(sql.toString());
            pst.setString(1, "%" + keyword + "%");

            if (typeFilter != null
                    && !typeFilter.equalsIgnoreCase("ALL")
                    && (typeFilter.equalsIgnoreCase("IN STOCK")
                    || typeFilter.equalsIgnoreCase("LOW STOCK")
                    || typeFilter.equalsIgnoreCase("OUT OF STOCK"))) {
                pst.setString(2, typeFilter.toUpperCase());
            }

            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("product_name"),
                    rs.getString("category_name"),
                    rs.getString("supplier_name"),
                    rs.getInt("stock_quantity"),
                    rs.getInt("reorder_level"),
                    rs.getString("stock_status")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading Stock Movement table: " + e.getMessage());
        }
    }

    // =========================
    // GET STOCK IN DETAILS BY TRANSACTION ID
    // =========================
    public StockInDetails getStockInDetailsByTransactionId(int transactionId) {
        StockInDetails d = null;

        try {
            String sql
                    = "SELECT it.transaction_id, p.name AS product_name, p.barcode, s.name AS supplier_name, "
                    + "it.quantity, it.reference_number, it.transaction_date, it.remarks, "
                    + "p.product_image, p.stock_quantity, p.reorder_level "
                    + "FROM inventory_transactions it "
                    + "INNER JOIN products p ON it.product_id = p.product_id "
                    + "INNER JOIN suppliers s ON p.supplier_id = s.supplier_id "
                    + "WHERE it.transaction_id = ? AND it.transaction_type = 'STOCK IN'";

            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, transactionId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                d = new StockInDetails();
                d.transactionId = rs.getInt("transaction_id");
                d.productName = rs.getString("product_name");
                d.barcode = rs.getString("barcode");
                d.supplierName = rs.getString("supplier_name");
                d.quantity = rs.getInt("quantity");
                d.referenceNumber = rs.getString("reference_number");
                d.transactionDate = rs.getDate("transaction_date");
                d.remarks = rs.getString("remarks");
                d.imagePath = rs.getString("product_image");
                d.currentStock = rs.getInt("stock_quantity");
                d.reorderLevel = rs.getInt("reorder_level");
                d.status = getStockStatus(d.currentStock, d.reorderLevel);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error getting Stock In details: " + e.getMessage());
        }

        return d;
    }

    // =========================
    // GET STOCK OUT DETAILS BY TRANSACTION ID
    // =========================
    public StockOutDetails getStockOutDetailsByTransactionId(int transactionId) {
        StockOutDetails d = null;

        try {
            String sql
                    = "SELECT it.transaction_id, p.name AS product_name, p.barcode, "
                    + "it.quantity, it.reason, it.reference_number, it.transaction_date, it.remarks, "
                    + "p.product_image, p.stock_quantity, p.reorder_level "
                    + "FROM inventory_transactions it "
                    + "INNER JOIN products p ON it.product_id = p.product_id "
                    + "WHERE it.transaction_id = ? AND it.transaction_type = 'STOCK OUT'";

            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, transactionId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                d = new StockOutDetails();
                d.transactionId = rs.getInt("transaction_id");
                d.productName = rs.getString("product_name");
                d.barcode = rs.getString("barcode");
                d.quantity = rs.getInt("quantity");
                d.reason = rs.getString("reason");
                d.referenceNumber = rs.getString("reference_number");
                d.transactionDate = rs.getDate("transaction_date");
                d.remarks = rs.getString("remarks");
                d.imagePath = rs.getString("product_image");
                d.currentStock = rs.getInt("stock_quantity");
                d.reorderLevel = rs.getInt("reorder_level");
                d.status = getStockStatus(d.currentStock, d.reorderLevel);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error getting Stock Out details: " + e.getMessage());
        }

        return d;
    }
    
    
    public void loadProductCombo(javax.swing.JComboBox<String> comboBox) {
        comboBox.removeAllItems();
        comboBox.addItem("SELECT PRODUCT");

        try {
            Connection con = DBConnection.getConnection();

            String sql = "SELECT name FROM products ORDER BY name ASC";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                comboBox.addItem(rs.getString("name"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public void loadStockMovementTable(javax.swing.JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        try {
            Connection con = DBConnection.getConnection();

            String sql
                    = "SELECT p.name AS product_name, "
                    + "c.name AS category_name, "
                    + "s.name AS supplier_name, "
                    + "p.stock_quantity, "
                    + "p.reorder_level "
                    + "FROM products p "
                    + "JOIN categories c ON p.category_id = c.category_id "
                    + "JOIN suppliers s ON p.supplier_id = s.supplier_id "
                    + "ORDER BY p.name ASC";

            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                int stockQty = rs.getInt("stock_quantity");
                int reorderLevel = rs.getInt("reorder_level");

                String status;
                if (stockQty == 0) {
                    status = "OUT OF STOCK";
                } else if (stockQty <= reorderLevel) {
                    status = "LOW STOCK";
                } else {
                    status = "IN STOCK";
                }

                model.addRow(new Object[]{
                    rs.getString("product_name"),
                    rs.getString("category_name"),
                    rs.getString("supplier_name"),
                    stockQty,
                    reorderLevel,
                    status
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
}
