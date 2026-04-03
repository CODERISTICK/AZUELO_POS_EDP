package pointofsale;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class SalesHistory {

    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    public static class SalesSummary {
        public double totalSales = 0;
        public int totalTransactions = 0;
        public int totalItemsSold = 0;
        public double totalDiscount = 0;
    }

    public void loadCashierCombo(JComboBox<String> cashierCombo) {
        cashierCombo.removeAllItems();
        cashierCombo.addItem("ALL CASHIERS");

        String sql = "SELECT user_id, first_name, last_name FROM users WHERE status='Active' ORDER BY first_name ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                String cashierName = rs.getInt("user_id") + " - "
                        + rs.getString("first_name") + " "
                        + rs.getString("last_name");
                cashierCombo.addItem(cashierName);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadPaymentMethodCombo(JComboBox<String> paymentCombo) {
        paymentCombo.removeAllItems();
        paymentCombo.addItem("ALL METHODS");
        paymentCombo.addItem("Cash");
        paymentCombo.addItem("GCash");
        paymentCombo.addItem("Card");
    }

    public void loadSalesHistoryTable(JTable salesListTable) {
        loadSalesHistoryTableModern(salesListTable, "", "", "", "ALL CASHIERS", "ALL METHODS");
    }

    public void loadSalesHistoryTableModern(
            JTable salesListTable,
            String startDate,
            String endDate,
            String invoiceNo,
            String cashierComboValue,
            String paymentMethodComboValue
    ) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Sale ID", "Date", "Receipt No.", "Customer", "Total Amount", "Payment Method", "Cashier"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        StringBuilder sql = new StringBuilder(
                "SELECT s.sale_id, s.sale_date, s.invoice_number, "
                + "s.total_amount, s.payment_method, "
                + "CONCAT(u.first_name, ' ', u.last_name) AS cashier "
                + "FROM sales s "
                + "JOIN users u ON s.user_id = u.user_id "
                + "WHERE 1=1 "
        );

        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append(" AND DATE(s.sale_date) >= ? ");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append(" AND DATE(s.sale_date) <= ? ");
        }
        if (invoiceNo != null && !invoiceNo.trim().isEmpty()) {
            sql.append(" AND s.invoice_number LIKE ? ");
        }
        if (paymentMethodComboValue != null && !paymentMethodComboValue.equals("ALL METHODS")) {
            sql.append(" AND s.payment_method = ? ");
        }
        if (cashierComboValue != null && !cashierComboValue.equals("ALL CASHIERS")) {
            sql.append(" AND s.user_id = ? ");
        }

        sql.append(" ORDER BY s.sale_date DESC ");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql.toString())) {

            int paramIndex = 1;

            if (startDate != null && !startDate.trim().isEmpty()) {
                pst.setString(paramIndex++, startDate);
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                pst.setString(paramIndex++, endDate);
            }
            if (invoiceNo != null && !invoiceNo.trim().isEmpty()) {
                pst.setString(paramIndex++, "%" + invoiceNo + "%");
            }
            if (paymentMethodComboValue != null && !paymentMethodComboValue.equals("ALL METHODS")) {
                pst.setString(paramIndex++, paymentMethodComboValue);
            }
            if (cashierComboValue != null && !cashierComboValue.equals("ALL CASHIERS")) {
                int userId = Integer.parseInt(cashierComboValue.split(" - ")[0]);
                pst.setInt(paramIndex++, userId);
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("sale_id"));
                    row.add(formatDateTime(rs.getString("sale_date")));
                    row.add(rs.getString("invoice_number"));
                    row.add("Walk-in");
                    row.add("₱" + df.format(rs.getDouble("total_amount")));
                    row.add(rs.getString("payment_method"));
                    row.add(rs.getString("cashier"));
                    model.addRow(row);
                }
            }

            salesListTable.setModel(model);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String formatDateTime(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.trim().isEmpty()) {
            return "";
        }

        try {
            java.util.Date parsed = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(rawDateTime);
            return new SimpleDateFormat("yyyy-MM-dd hh:mm a").format(parsed);
        } catch (Exception ex) {
            return rawDateTime;
        }
    }

    public void searchSalesHistory(
            JTable salesListTable,
            String startDate,
            String endDate,
            String invoiceNo,
            String cashierComboValue,
            String paymentMethodComboValue
    ) {
        DefaultTableModel model = (DefaultTableModel) salesListTable.getModel();
        model.setRowCount(0);

        StringBuilder sql = new StringBuilder(
            "SELECT s.sale_id, s.invoice_number, s.sale_date, " +
            "CONCAT(u.first_name, ' ', u.last_name) AS cashier, " +
            "s.subtotal_amount, s.vat_amount, s.discount_amount, " +
            "s.total_amount, s.payment_method, s.change_amount " +
            "FROM sales s " +
            "JOIN users u ON s.user_id = u.user_id " +
            "WHERE 1=1 "
        );

        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append(" AND DATE(s.sale_date) >= ? ");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append(" AND DATE(s.sale_date) <= ? ");
        }
        if (invoiceNo != null && !invoiceNo.trim().isEmpty()) {
            sql.append(" AND s.invoice_number LIKE ? ");
        }
        if (paymentMethodComboValue != null && !paymentMethodComboValue.equals("ALL METHODS")) {
            sql.append(" AND s.payment_method = ? ");
        }
        if (cashierComboValue != null && !cashierComboValue.equals("ALL CASHIERS")) {
            sql.append(" AND s.user_id = ? ");
        }

        sql.append(" ORDER BY s.sale_date DESC ");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql.toString())) {

            int paramIndex = 1;

            if (startDate != null && !startDate.trim().isEmpty()) {
                pst.setString(paramIndex++, startDate);
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                pst.setString(paramIndex++, endDate);
            }
            if (invoiceNo != null && !invoiceNo.trim().isEmpty()) {
                pst.setString(paramIndex++, "%" + invoiceNo + "%");
            }
            if (paymentMethodComboValue != null && !paymentMethodComboValue.equals("ALL METHODS")) {
                pst.setString(paramIndex++, paymentMethodComboValue);
            }
            if (cashierComboValue != null && !cashierComboValue.equals("ALL CASHIERS")) {
                int userId = Integer.parseInt(cashierComboValue.split(" - ")[0]);
                pst.setInt(paramIndex++, userId);
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("sale_id"));
                    row.add(rs.getString("invoice_number"));
                    row.add(rs.getString("sale_date"));
                    row.add(rs.getString("cashier"));
                    row.add(df.format(rs.getDouble("subtotal_amount")));
                    row.add(df.format(rs.getDouble("vat_amount")));
                    row.add(df.format(rs.getDouble("discount_amount")));
                    row.add(df.format(rs.getDouble("total_amount")));
                    row.add(rs.getString("payment_method"));
                    row.add(df.format(rs.getDouble("change_amount")));
                    model.addRow(row);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadSelectedSaleItems(int saleId, JTable selectedSaleItemsTable) {
        DefaultTableModel model = (DefaultTableModel) selectedSaleItemsTable.getModel();
        model.setRowCount(0);

        String sql =
            "SELECT p.name AS product_name, si.quantity, si.unit_price, si.discount, si.total_price " +
            "FROM sale_items si " +
            "JOIN products p ON si.product_id = p.product_id " +
            "WHERE si.sale_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, saleId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getString("product_name"));
                    row.add(rs.getInt("quantity"));
                    row.add(df.format(rs.getDouble("unit_price")));
                    row.add(df.format(rs.getDouble("discount")));
                    row.add(df.format(rs.getDouble("total_price")));
                    model.addRow(row);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public SalesSummary getSalesSummary(
            String startDate,
            String endDate,
            String invoiceNo,
            String cashierComboValue,
            String paymentMethodComboValue
    ) {
        SalesSummary summary = new SalesSummary();

        StringBuilder salesSql = new StringBuilder(
                "SELECT "
                + "COALESCE(SUM(s.total_amount), 0) AS total_sales, "
                + "COUNT(*) AS total_transactions, "
                + "COALESCE(SUM(s.discount_amount), 0) AS total_discount "
                + "FROM sales s "
                + "JOIN users u ON s.user_id = u.user_id "
                + "WHERE 1=1 "
        );

        if (startDate != null && !startDate.trim().isEmpty()) {
            salesSql.append(" AND DATE(s.sale_date) >= ? ");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            salesSql.append(" AND DATE(s.sale_date) <= ? ");
        }
        if (invoiceNo != null && !invoiceNo.trim().isEmpty()) {
            salesSql.append(" AND s.invoice_number LIKE ? ");
        }
        if (paymentMethodComboValue != null && !paymentMethodComboValue.equals("ALL METHODS")) {
            salesSql.append(" AND s.payment_method = ? ");
        }
        if (cashierComboValue != null && !cashierComboValue.equals("ALL CASHIERS")) {
            salesSql.append(" AND s.user_id = ? ");
        }

        StringBuilder itemsSql = new StringBuilder(
                "SELECT COALESCE(SUM(si.quantity), 0) AS total_items_sold "
                + "FROM sale_items si "
                + "JOIN sales s ON si.sale_id = s.sale_id "
                + "JOIN users u ON s.user_id = u.user_id "
                + "WHERE 1=1 "
        );

        if (startDate != null && !startDate.trim().isEmpty()) {
            itemsSql.append(" AND DATE(s.sale_date) >= ? ");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            itemsSql.append(" AND DATE(s.sale_date) <= ? ");
        }
        if (invoiceNo != null && !invoiceNo.trim().isEmpty()) {
            itemsSql.append(" AND s.invoice_number LIKE ? ");
        }
        if (paymentMethodComboValue != null && !paymentMethodComboValue.equals("ALL METHODS")) {
            itemsSql.append(" AND s.payment_method = ? ");
        }
        if (cashierComboValue != null && !cashierComboValue.equals("ALL CASHIERS")) {
            itemsSql.append(" AND s.user_id = ? ");
        }

        try (Connection con = DBConnection.getConnection()) {

            try (PreparedStatement pstSales = con.prepareStatement(salesSql.toString())) {
                int paramIndex = 1;

                if (startDate != null && !startDate.trim().isEmpty()) {
                    pstSales.setString(paramIndex++, startDate);
                }
                if (endDate != null && !endDate.trim().isEmpty()) {
                    pstSales.setString(paramIndex++, endDate);
                }
                if (invoiceNo != null && !invoiceNo.trim().isEmpty()) {
                    pstSales.setString(paramIndex++, "%" + invoiceNo + "%");
                }
                if (paymentMethodComboValue != null && !paymentMethodComboValue.equals("ALL METHODS")) {
                    pstSales.setString(paramIndex++, paymentMethodComboValue);
                }
                if (cashierComboValue != null && !cashierComboValue.equals("ALL CASHIERS")) {
                    int userId = Integer.parseInt(cashierComboValue.split(" - ")[0]);
                    pstSales.setInt(paramIndex++, userId);
                }

                try (ResultSet rs = pstSales.executeQuery()) {
                    if (rs.next()) {
                        summary.totalSales = rs.getDouble("total_sales");
                        summary.totalTransactions = rs.getInt("total_transactions");
                        summary.totalDiscount = rs.getDouble("total_discount");
                    }
                }
            }

            try (PreparedStatement pstItems = con.prepareStatement(itemsSql.toString())) {
                int paramIndex = 1;

                if (startDate != null && !startDate.trim().isEmpty()) {
                    pstItems.setString(paramIndex++, startDate);
                }
                if (endDate != null && !endDate.trim().isEmpty()) {
                    pstItems.setString(paramIndex++, endDate);
                }
                if (invoiceNo != null && !invoiceNo.trim().isEmpty()) {
                    pstItems.setString(paramIndex++, "%" + invoiceNo + "%");
                }
                if (paymentMethodComboValue != null && !paymentMethodComboValue.equals("ALL METHODS")) {
                    pstItems.setString(paramIndex++, paymentMethodComboValue);
                }
                if (cashierComboValue != null && !cashierComboValue.equals("ALL CASHIERS")) {
                    int userId = Integer.parseInt(cashierComboValue.split(" - ")[0]);
                    pstItems.setInt(paramIndex++, userId);
                }

                try (ResultSet rs = pstItems.executeQuery()) {
                    if (rs.next()) {
                        summary.totalItemsSold = rs.getInt("total_items_sold");
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return summary;
    }

    public void setSummaryLabels(
            JLabel totalSalesLbl,
            JLabel transactionHistoryLbl,
            JLabel itemSoldLbl,
            JLabel totalDiscountLbl,
            SalesSummary summary
    ) {
        totalSalesLbl.setText("₱" + df.format(summary.totalSales));
        transactionHistoryLbl.setText(String.valueOf(summary.totalTransactions));
        itemSoldLbl.setText(String.valueOf(summary.totalItemsSold));
        totalDiscountLbl.setText("₱" + df.format(summary.totalDiscount));
    }

    public void clearFilters(
            javax.swing.JTextField startDateTxt,
            javax.swing.JTextField endDateTxt,
            javax.swing.JTextField invoiceNoTxt,
            JComboBox<String> cashierCombo,
            JComboBox<String> paymentCombo,
            JTable selectedSaleItemsTable
    ) {
        startDateTxt.setText("");
        endDateTxt.setText("");
        invoiceNoTxt.setText("");
        cashierCombo.setSelectedIndex(0);
        paymentCombo.setSelectedIndex(0);

        DefaultTableModel itemModel = (DefaultTableModel) selectedSaleItemsTable.getModel();
        itemModel.setRowCount(0);
    }
}