package pointofsale;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

public class Dashboard {

    /* =========================
       LOAD DASHBOARD STATISTICS
       ========================= */
    public void loadDashboardStats(
            JLabel totalProduct_lbl,
            JLabel totalSupplier_lbl,
            JLabel totalUser_lbl,
            JLabel lowstockDashboad_lbl,
            JLabel totalSalesDashboard,
            JLabel outOfStockdashboard_lbl,
            JLabel totalCategory_lbl,
            JLabel totalTransaction_lbl) {

        try {

            Connection con = DBConnection.getConnection();

            /* TOTAL PRODUCTS */
            PreparedStatement pst = con.prepareStatement(
                    "SELECT COUNT(*) AS total FROM products");
            ResultSet rs = pst.executeQuery();
            totalProduct_lbl.setText(rs.next() ? rs.getString("total") : "0");

            /* TOTAL SUPPLIERS */
            pst = con.prepareStatement(
                    "SELECT COUNT(*) AS total FROM suppliers");
            rs = pst.executeQuery();
            totalSupplier_lbl.setText(rs.next() ? rs.getString("total") : "0");

            /* TOTAL USERS */
            pst = con.prepareStatement(
                    "SELECT COUNT(*) AS total FROM users");
            rs = pst.executeQuery();
            totalUser_lbl.setText(rs.next() ? rs.getString("total") : "0");

            /* TOTAL CATEGORY */
            pst = con.prepareStatement(
                    "SELECT COUNT(*) AS total FROM categories");
            rs = pst.executeQuery();
            totalCategory_lbl.setText(rs.next() ? rs.getString("total") : "0");

            /* LOW STOCK */
            pst = con.prepareStatement(
                    "SELECT COUNT(*) AS total FROM products WHERE stock_quantity <= reorder_level AND stock_quantity > 0");
            rs = pst.executeQuery();
            lowstockDashboad_lbl.setText(rs.next() ? rs.getString("total") : "0");

            /* OUT OF STOCK */
            pst = con.prepareStatement(
                    "SELECT COUNT(*) AS total FROM products WHERE stock_quantity = 0");
            rs = pst.executeQuery();
            outOfStockdashboard_lbl.setText(rs.next() ? rs.getString("total") : "0");

            /* TOTAL SALES */
            pst = con.prepareStatement(
                    "SELECT IFNULL(SUM(total_amount),0) AS total FROM sales");
            rs = pst.executeQuery();
            totalSalesDashboard.setText(rs.next()
                    ? "₱" + rs.getString("total")
                    : "₱0");

            /* TOTAL TRANSACTION */
            pst = con.prepareStatement(
                    "SELECT COUNT(*) AS total FROM sales");
            rs = pst.executeQuery();
            totalTransaction_lbl.setText(rs.next() ? rs.getString("total") : "0");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =========================
       TOP SELLING PRODUCTS
       ========================= */
    public void loadTopSellingProducts(JTable dashboard_tbl) {

        try {

            DefaultTableModel model = (DefaultTableModel) dashboard_tbl.getModel();
            model.setRowCount(0);

            Connection con = DBConnection.getConnection();

            String sql =
                    "SELECT p.name, SUM(si.quantity) qty_sold, " +
                    "SUM(si.total_price) total_sales " +
                    "FROM sale_items si " +
                    "JOIN products p ON si.product_id = p.product_id " +
                    "GROUP BY p.name " +
                    "ORDER BY qty_sold DESC LIMIT 5";

            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {

                Object[] row = {
                    rs.getString("name"),
                    rs.getInt("qty_sold"),
                    "₱" + rs.getDouble("total_sales")
                };

                model.addRow(row);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =========================
       SALES PIE CHART
       ========================= */
    public void loadSalesPieChart(JPanel panel) {

        try {

            DefaultPieDataset dataset = new DefaultPieDataset();

            Connection con = DBConnection.getConnection();

            String sql =
                    "SELECT c.name, SUM(si.total_price) total " +
                    "FROM sale_items si " +
                    "JOIN products p ON si.product_id = p.product_id " +
                    "JOIN categories c ON p.category_id = c.category_id " +
                    "GROUP BY c.name";

            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                dataset.setValue(rs.getString("name"), rs.getDouble("total"));
            }

            JFreeChart chart = ChartFactory.createPieChart(
                    "Sales by Category",
                    dataset,
                    true,
                    true,
                    false);

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setBounds(0, 0, panel.getWidth(), panel.getHeight());

            panel.removeAll();
            panel.add(chartPanel);
            panel.revalidate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}