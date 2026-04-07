package pointofsale;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class Reports {

    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    public void loadReportTypes(JComboBox<String> reportTypeCombo) {
        reportTypeCombo.removeAllItems();
        reportTypeCombo.addItem("Daily Sales");
        reportTypeCombo.addItem("Sales by Product");
        reportTypeCombo.addItem("Profit Report");
    }

    public void loadCategoryCombo(JComboBox<String> categoryCombo) {
        categoryCombo.removeAllItems();
        categoryCombo.addItem("All Categories");

        String sql = "SELECT name FROM categories ORDER BY name ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                categoryCombo.addItem(rs.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadSupplierCombo(JComboBox<String> supplierCombo) {
        supplierCombo.removeAllItems();
        supplierCombo.addItem("All Suppliers");

        String sql = "SELECT name FROM suppliers ORDER BY name ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                supplierCombo.addItem(rs.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadPaymentCombo(JComboBox<String> paymentCombo) {
        paymentCombo.removeAllItems();
        paymentCombo.addItem("All");
        paymentCombo.addItem("Cash");
        paymentCombo.addItem("GCash");
        paymentCombo.addItem("Card");
    }

    public void setupReportTable(JTable reportTable) {
        DefaultTableModel model = new DefaultTableModel(
            new Object[][]{},
            new String[]{
                "Date", "Invoice", "Product", "Qty", "Unit Price", "Discount", "Sales", "Profit"
            }
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        reportTable.setModel(model);
        reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        reportTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        reportTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        reportTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        reportTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        reportTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        reportTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        reportTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        reportTable.getColumnModel().getColumn(7).setPreferredWidth(100);
    }

    public void generateReport(
            JTable reportTable,
            String reportType,
            String startDate,
            String endDate,
            String category,
            String supplier,
            String paymentMethod
    ) {
        DefaultTableModel model = (DefaultTableModel) reportTable.getModel();
        model.setRowCount(0);

        String normalizedType = reportType == null ? "Daily Sales" : reportType.trim();
        String costExpr = hasSaleItemsCostSnapshotColumn()
                ? "COALESCE(si.cost_price_snapshot, COALESCE(p.cost_price, 0))"
                : "COALESCE(p.cost_price, 0)";

        StringBuilder sql = new StringBuilder();
        if ("Sales by Product".equalsIgnoreCase(normalizedType)) {
            sql.append(
                "SELECT MAX(DATE(s.sale_date)) AS report_date, " +
                "'MULTIPLE' AS invoice_number, " +
                "p.name AS product_name, " +
                "SUM(si.quantity) AS quantity, " +
                "AVG(si.unit_price) AS unit_price, " +
                "SUM(si.discount) AS discount, " +
                "SUM(si.total_price) AS sales, " +
                "SUM(((si.unit_price - " + costExpr + ") * si.quantity) - si.discount) AS profit " +
                "FROM sales s " +
                "JOIN sale_items si ON s.sale_id = si.sale_id " +
                "JOIN products p ON si.product_id = p.product_id " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "LEFT JOIN suppliers sp ON p.supplier_id = sp.supplier_id " +
                "WHERE 1=1 "
            );
            appendStandardFilters(sql, startDate, endDate, category, supplier, paymentMethod);
            sql.append(" GROUP BY p.product_id, p.name ORDER BY sales DESC, p.name ASC ");
        } else if ("Profit Report".equalsIgnoreCase(normalizedType)) {
            sql.append(
                "SELECT MAX(DATE(s.sale_date)) AS report_date, " +
                "'MULTIPLE' AS invoice_number, " +
                "p.name AS product_name, " +
                "SUM(si.quantity) AS quantity, " +
                "AVG(si.unit_price) AS unit_price, " +
                "SUM(si.discount) AS discount, " +
                "SUM(si.total_price) AS sales, " +
                "SUM(((si.unit_price - " + costExpr + ") * si.quantity) - si.discount) AS profit " +
                "FROM sales s " +
                "JOIN sale_items si ON s.sale_id = si.sale_id " +
                "JOIN products p ON si.product_id = p.product_id " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "LEFT JOIN suppliers sp ON p.supplier_id = sp.supplier_id " +
                "WHERE 1=1 "
            );
            appendStandardFilters(sql, startDate, endDate, category, supplier, paymentMethod);
            sql.append(" GROUP BY p.product_id, p.name ORDER BY profit DESC, p.name ASC ");
        } else {
            sql.append(
                "SELECT DATE(s.sale_date) AS report_date, " +
                "s.invoice_number, " +
                "p.name AS product_name, " +
                "si.quantity, " +
                "si.unit_price, " +
                "si.discount, " +
                "si.total_price AS sales, " +
                "(((si.unit_price - " + costExpr + ") * si.quantity) - si.discount) AS profit " +
                "FROM sales s " +
                "JOIN sale_items si ON s.sale_id = si.sale_id " +
                "JOIN products p ON si.product_id = p.product_id " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "LEFT JOIN suppliers sp ON p.supplier_id = sp.supplier_id " +
                "WHERE 1=1 "
            );
            appendStandardFilters(sql, startDate, endDate, category, supplier, paymentMethod);
            sql.append(" ORDER BY DATE(s.sale_date) DESC, s.invoice_number ASC ");
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql.toString())) {

            int index = applyStandardFilters(pst, startDate, endDate, category, supplier, paymentMethod);
            if (index < 1) {
                index = 1;
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getString("report_date"));
                    row.add(rs.getString("invoice_number"));
                    row.add(rs.getString("product_name"));
                    row.add(rs.getInt("quantity"));
                    row.add(df.format(rs.getDouble("unit_price")));
                    row.add(df.format(rs.getDouble("discount")));
                    row.add(df.format(rs.getDouble("sales")));
                    row.add(df.format(rs.getDouble("profit")));
                    model.addRow(row);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error generating report: " + e.getMessage());
        }
    }

    private boolean hasSaleItemsCostSnapshotColumn() {
        String sql = "SELECT 1 FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() "
                + "AND table_name = 'sale_items' "
                + "AND column_name = 'cost_price_snapshot' LIMIT 1";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
    }

    private void appendStandardFilters(
            StringBuilder sql,
            String startDate,
            String endDate,
            String category,
            String supplier,
            String paymentMethod
    ) {
        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append(" AND DATE(s.sale_date) >= ? ");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append(" AND DATE(s.sale_date) <= ? ");
        }
        if (category != null && !"All Categories".equalsIgnoreCase(category.trim())) {
            sql.append(" AND c.name = ? ");
        }
        if (supplier != null && !"All Suppliers".equalsIgnoreCase(supplier.trim())) {
            sql.append(" AND sp.name = ? ");
        }
        if (paymentMethod != null && !"All".equalsIgnoreCase(paymentMethod.trim())) {
            sql.append(" AND s.payment_method = ? ");
        }
    }

    private int applyStandardFilters(
            PreparedStatement pst,
            String startDate,
            String endDate,
            String category,
            String supplier,
            String paymentMethod
    ) throws SQLException {
        int index = 1;

        if (startDate != null && !startDate.trim().isEmpty()) {
            pst.setString(index++, startDate.trim());
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            pst.setString(index++, endDate.trim());
        }
        if (category != null && !"All Categories".equalsIgnoreCase(category.trim())) {
            pst.setString(index++, category.trim());
        }
        if (supplier != null && !"All Suppliers".equalsIgnoreCase(supplier.trim())) {
            pst.setString(index++, supplier.trim());
        }
        if (paymentMethod != null && !"All".equalsIgnoreCase(paymentMethod.trim())) {
            pst.setString(index++, paymentMethod.trim());
        }

        return index;
    }

    public void exportTableToPDF(JTable table, String reportType) {
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "No data to export.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save PDF Report");

        String safeName = reportType.replaceAll("[^a-zA-Z0-9]", "_");
        chooser.setSelectedFile(new File(safeName + "_Report.pdf"));

        int option = chooser.showSaveDialog(null);
        if (option != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getAbsolutePath() + ".pdf");
        }

        PDDocument document = null;
        PDPageContentStream content = null;

        try {
            document = new PDDocument();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            content = new PDPageContentStream(document, page);

            float margin = 40;
            float y = 770;
            float rowHeight = 20;
            float[] colWidths = {65, 70, 100, 35, 70, 60, 60, 60};

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            // Title
            content.setFont(fontBold, 14);
            content.beginText();
            content.newLineAtOffset(margin, y);
            content.showText("StockWise POS - " + reportType);
            content.endText();

            y -= 30;

            // Header row
            content.setFont(fontBold, 9);

            float x = margin;
            for (int i = 0; i < table.getColumnCount(); i++) {
                content.addRect(x, y - rowHeight, colWidths[i], rowHeight);
                content.stroke();

                content.beginText();
                content.newLineAtOffset(x + 2, y - 14);
                content.showText(table.getColumnName(i));
                content.endText();

                x += colWidths[i];
            }

            y -= rowHeight;

            // Data rows
            content.setFont(fontRegular, 8);

            for (int row = 0; row < table.getRowCount(); row++) {

                // New page if space is low
                if (y < 60) {
                    content.close();

                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);

                    y = 770;

                    // Repeat title on new page
                    content.setFont(fontBold, 14);
                    content.beginText();
                    content.newLineAtOffset(margin, y);
                    content.showText("StockWise POS - " + reportType);
                    content.endText();

                    y -= 30;

                    // Repeat header on new page
                    content.setFont(fontBold, 9);
                    x = margin;
                    for (int i = 0; i < table.getColumnCount(); i++) {
                        content.addRect(x, y - rowHeight, colWidths[i], rowHeight);
                        content.stroke();

                        content.beginText();
                        content.newLineAtOffset(x + 2, y - 14);
                        content.showText(table.getColumnName(i));
                        content.endText();

                        x += colWidths[i];
                    }

                    y -= rowHeight;
                    content.setFont(fontRegular, 8);
                }

                x = margin;

                for (int col = 0; col < table.getColumnCount(); col++) {
                    content.addRect(x, y - rowHeight, colWidths[col], rowHeight);
                    content.stroke();

                    Object value = table.getValueAt(row, col);
                    String text = value == null ? "" : value.toString();

                    if (text.length() > 16) {
                        text = text.substring(0, 16);
                    }

                    content.beginText();
                    content.newLineAtOffset(x + 2, y - 14);
                    content.showText(text);
                    content.endText();

                    x += colWidths[col];
                }

                y -= rowHeight;
            }

            content.close();
            document.save(file);
            document.close();

            JOptionPane.showMessageDialog(null, "PDF exported successfully:\n" + file.getAbsolutePath());

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error exporting PDF: " + e.getMessage());
            try {
                if (content != null) {
                    content.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                if (document != null) {
                    document.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void exportTableToExcel(JTable table, String reportType) {
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "No data to export.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Excel Report");

        String safeName = reportType.replaceAll("[^a-zA-Z0-9]", "_");
        chooser.setSelectedFile(new File(safeName + "_Report.csv"));

        int option = chooser.showSaveDialog(null);
        if (option != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < table.getColumnCount(); i++) {
                sb.append(escapeCsv(table.getColumnName(i)));
                if (i < table.getColumnCount() - 1) {
                    sb.append(",");
                }
            }
            sb.append("\n");

            for (int row = 0; row < table.getRowCount(); row++) {
                for (int col = 0; col < table.getColumnCount(); col++) {
                    Object value = table.getValueAt(row, col);
                    String text = value == null ? "" : value.toString();
                    sb.append(escapeCsv(text));
                    if (col < table.getColumnCount() - 1) {
                        sb.append(",");
                    }
                }
                sb.append("\n");
            }

            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));

            JOptionPane.showMessageDialog(null, "Excel exported successfully:\n" + file.getAbsolutePath());

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error exporting Excel: " + e.getMessage());
        }
    }

    private String escapeCsv(String text) {
        if (text == null) {
            return "";
        }

        String escaped = text.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    public String formatDateChooser(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }
    
    
    public void formatReportTable(JTable table) {

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.getColumnModel().getColumn(0).setPreferredWidth(120); // Date
        table.getColumnModel().getColumn(1).setPreferredWidth(180); // Invoice
        table.getColumnModel().getColumn(2).setPreferredWidth(200); // Product
        table.getColumnModel().getColumn(3).setPreferredWidth(80);  // Qty
        table.getColumnModel().getColumn(4).setPreferredWidth(120); // Unit Price
        table.getColumnModel().getColumn(5).setPreferredWidth(120); // Discount
        table.getColumnModel().getColumn(6).setPreferredWidth(120); // Sales
        table.getColumnModel().getColumn(7).setPreferredWidth(120); // Profit
    }
}