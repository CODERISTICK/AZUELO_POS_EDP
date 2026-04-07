package pointofsale;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.PdfWriter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.util.Base64;
import javax.imageio.ImageIO;

public class PosController {
    // --- Totals Calculation Logic ---
    private void calculateTotals() {
        BigDecimal vatableSales = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        // Sum cart totals (VAT-inclusive prices) and line discounts.
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            Object totalObj = cartModel.getValueAt(i, 6); // Line total (after discount)
            Object discObj = cartModel.getValueAt(i, 5); // Discount column
            try {
                String totalStr = totalObj != null ? totalObj.toString().replace("₱", "").replace(",", "").trim() : "0";
                String discStr = discObj != null ? discObj.toString().replace("₱", "").replace(",", "").trim() : "0";
                total = total.add(new BigDecimal(totalStr));
                discount = discount.add(new BigDecimal(discStr));
            } catch (Exception e) {
                // ignore parse errors
            }
        }

        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        // PH default pricing is VAT-inclusive.
        // VATable Sales = Total / 1.12
        // VAT = Total - VATable Sales
        if (cartModel.getRowCount() > 0) {
            vatableSales = total.divide(VAT_INCLUSIVE_DIVISOR, 2, RoundingMode.HALF_UP);
            vat = total.subtract(vatableSales).setScale(2, RoundingMode.HALF_UP);
        }

        // Update fields (always update all payment fields regardless of tab)
        subtotalTxt.setText(formatPeso(vatableSales));
        vatTxt.setText(formatPeso(vat));
        discountTxt.setText(formatPeso(discount));
        totalTxt.setText(formatPeso(total));
        totalTxt.setForeground(SUCCESS);
        totalTxt.setFont(new Font("Segoe UI", Font.BOLD, 14));

        // Always update GCash and Card amount fields if present
        if (gcashAmountTxt != null) gcashAmountTxt.setText(formatPeso(total));
        if (cardAmountTxt != null) cardAmountTxt.setText(formatPeso(total));

        // Always update change for Cash tab
        updateChangeAmount(total);
    }

    private void updateChangeAmount(BigDecimal total) {
        try {
            String cashStr = cashTenderedTxt.getText().replace("₱", "").replace(",", "").trim();
            if (cashStr.isEmpty()) cashStr = "0";
            BigDecimal cash = new BigDecimal(cashStr);
            BigDecimal change = cash.subtract(total);
            if (change.compareTo(BigDecimal.ZERO) < 0) change = BigDecimal.ZERO;
            changeTxt.setText(formatPeso(change));
        } catch (Exception e) {
            changeTxt.setText("₱0.00");
        }
    }

    private final JPanel posPanel;
    private final JPanel headerPanel;
    private final JPanel barcodePanel;
    private final JPanel cartPanel;
    private final JPanel paymentPanel;
    private final JPanel buttonPanel;
    private final String loggedInUsername;
    private final String loggedInFullName;
    private final Runnable afterCheckoutRefresh;

    // Header
    private JLabel titleLbl;
    private JLabel dateTimeLbl;
    private JLabel cashierLbl;
    private JLabel invoiceLbl;
    private JLabel shortcutLbl;

    // Entry area
    private JTextField barcodeTxt;
    private JSpinner qtySpinner;
    private JTextField itemDiscountTxt;
    private JButton searchProductBtn;
    private JButton addItemBtn;
    private JButton removeItemBtn;
    private JButton refreshBtn;
    private JLabel barcodeStatusBadge;

    // Cart
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JScrollPane cartScroll;

    // Payment summary
    private JTextField subtotalTxt;
    private JTextField vatTxt;
    private JTextField discountTxt;
    private JTextField totalTxt;

    // Cash
    private JTextField cashTenderedTxt;
    private JTextField changeTxt;

    // GCash
    private JTextField gcashNumberTxt;
    private JTextField gcashReferenceTxt;
    private JTextField gcashAmountTxt;

    // Card
    private JTextField cardNumberTxt;
    private JTextField cardHolderTxt;
    private JTextField cardExpiryTxt;
    private JTextField cardCvvTxt;
    private JTextField cardAmountTxt;

    // Payment tabs
    private JPanel paymentContentPanel;
    private JButton cashTabBtn;
    private JButton gcashTabBtn;
    private JButton cardTabBtn;
    private String currentPaymentMethod = "Cash";

    // Buttons
    private JButton checkoutBtn;
    private JButton cancelBtn;
    private JButton printBtn;

    private Timer dateTimer;
    private Timer scannerTimer;
    private Timer barcodePreviewTimer;

    private static final BigDecimal VAT_RATE = new BigDecimal("0.12");
    private static final BigDecimal VAT_INCLUSIVE_DIVISOR = BigDecimal.ONE.add(VAT_RATE);

    private boolean formattingCashField = false;
    private boolean formattingDiscountField = false;
    private String lastReceiptText = null;

    private final DecimalFormat moneyFormat = new DecimalFormat(
            "#,##0.00",
            DecimalFormatSymbols.getInstance(Locale.US)
    );
    
    //fixing
    private boolean updatingPaymentUI = false;
    private boolean suppressCashTenderedListener = false; 

    // Modern colors
    private final Color BG_MAIN = new Color(241, 245, 249);
    private final Color BG_CARD = Color.WHITE;
    private final Color PRIMARY = new Color(30, 41, 59);
    private final Color PRIMARY_SOFT = new Color(51, 65, 85);
    private final Color ACCENT = new Color(59, 130, 246);
    private final Color SUCCESS = new Color(34, 197, 94);
    private final Color DANGER = new Color(239, 68, 68);
    private final Color WARNING = new Color(245, 158, 11);
    private final Color BORDER = new Color(226, 232, 240);
    private final Color TEXT_DARK = new Color(15, 23, 42);
    private final Color TEXT_MUTED = new Color(100, 116, 139);
    private final Color FIELD_BG = new Color(248, 250, 252);
    
    private JLabel productImageLbl;
    private JPanel stockInfoPanel;
    private JLabel stockLabelLbl;
    private JLabel stockValueLbl;
    private String currentPreviewBarcode;
    private byte[] selectedProductImageBytes;
    private final JPanel imagePanel;

    public PosController(
            JPanel posPanel,
            JPanel headerPanel,
            JPanel barcodePanel,
            JPanel cartPanel,
            JPanel paymentPanel,
            JPanel buttonPanel,
            JPanel imagePanel,
            String loggedInUsername,
                String loggedInFullName,
            Runnable afterCheckoutRefresh
    ) {
        this.posPanel = posPanel;
        this.headerPanel = headerPanel;
        this.barcodePanel = barcodePanel;
        this.cartPanel = cartPanel;
        this.paymentPanel = paymentPanel;
        this.buttonPanel = buttonPanel;
        this.imagePanel = imagePanel;
        this.loggedInUsername = loggedInUsername;
        this.loggedInFullName = loggedInFullName;
        this.afterCheckoutRefresh = afterCheckoutRefresh;

        initialize();
    }

    private void initialize() {
        styleRootPanels();
        buildHeaderPanel();
        buildBarcodePanel();
        buildCartPanel();
        buildPaymentPanel();
        buildButtonPanel();
        buildImagePanel();
        wireEvents();
        installKeyboardShortcuts();
        startDateTimeTimer();
        installBarcodeScannerDetection();
        resetPOS();
    }

    private void styleRootPanels() {
        posPanel.setBackground(BG_MAIN);

        styleSectionPanel(headerPanel);
        styleSectionPanel(barcodePanel);
        styleSectionPanel(cartPanel);
        styleSectionPanel(paymentPanel);
        styleSectionPanel(buttonPanel);
    }

    private void styleSectionPanel(JPanel panel) {
    panel.setBackground(BG_CARD);
    panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(10, 10, 10, 10)
    ));

    if (panel != paymentPanel) {
        panel.setLayout(null);
    }
}

    private void buildHeaderPanel() {
        headerPanel.removeAll();

        titleLbl = new JLabel("POS SALES");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLbl.setForeground(PRIMARY);
        titleLbl.setBounds(20, 8, 220, 30);
        headerPanel.add(titleLbl);

        shortcutLbl = new JLabel("F2 Checkout   |   F3 Search Product   |   F4 Cancel");
        shortcutLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        shortcutLbl.setForeground(TEXT_MUTED);
        shortcutLbl.setBounds(20, 38, 360, 18);
        headerPanel.add(shortcutLbl);

        dateTimeLbl = new JLabel("Date/Time: --");
        dateTimeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dateTimeLbl.setForeground(TEXT_DARK);
        dateTimeLbl.setBounds(430, 10, 240, 24);
        headerPanel.add(dateTimeLbl);

        cashierLbl = new JLabel("Cashier: " + getCashierDisplayName());
        cashierLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cashierLbl.setForeground(TEXT_DARK);
        cashierLbl.setBounds(690, 10, 170, 24);
        headerPanel.add(cashierLbl);

        invoiceLbl = new JLabel("Invoice: --");
        invoiceLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        invoiceLbl.setForeground(ACCENT);
        invoiceLbl.setBounds(900, 10, 260, 24);
        headerPanel.add(invoiceLbl);

        headerPanel.repaint();
        headerPanel.revalidate();
    }

    private void buildBarcodePanel() {
        barcodePanel.removeAll();

        int y = 14;

        JLabel barcodeLbl = createSmallLabel("Barcode", 20, y);
        barcodePanel.add(barcodeLbl);

        barcodeTxt = createInputField(90, y, 220, 32);
        barcodeTxt.setHorizontalAlignment(SwingConstants.LEFT);
        barcodePanel.add(barcodeTxt);

        searchProductBtn = createButton("SEARCH", PRIMARY_SOFT, Color.WHITE);
        searchProductBtn.setBounds(325, y, 95, 32);
        barcodePanel.add(searchProductBtn);

        JLabel qtyLbl = createSmallLabel("Qty", 440, y);
        barcodePanel.add(qtyLbl);

        qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        qtySpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        qtySpinner.setBounds(475, y, 80, 32);
        barcodePanel.add(qtySpinner);

        JLabel discountLbl = createSmallLabel("Discount", 575, y);
        barcodePanel.add(discountLbl);

        itemDiscountTxt = createInputField(650, y, 100, 32);
        itemDiscountTxt.setText("0.00");
        itemDiscountTxt.setHorizontalAlignment(SwingConstants.RIGHT);
        barcodePanel.add(itemDiscountTxt);

        addItemBtn = createButton("ADD ITEM", ACCENT, Color.WHITE);
        addItemBtn.setBounds(770, y, 110, 32);
        barcodePanel.add(addItemBtn);

        removeItemBtn = createButton("REMOVE", DANGER, Color.WHITE);
        removeItemBtn.setBounds(890, y, 100, 32);
        barcodePanel.add(removeItemBtn);

        refreshBtn = createButton("REFRESH", PRIMARY_SOFT, Color.WHITE);
        refreshBtn.setBounds(1000, y, 105, 32);
        barcodePanel.add(refreshBtn);

        JLabel hintLbl = new JLabel("Scan barcode or search product, set qty/discount, then click Add Item.");
        hintLbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hintLbl.setForeground(TEXT_MUTED);
        hintLbl.setBounds(20, 50, 470, 18);
        barcodePanel.add(hintLbl);

        barcodeStatusBadge = new JLabel("Ready to scan", SwingConstants.CENTER);
        barcodeStatusBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        barcodeStatusBadge.setOpaque(true);
        barcodeStatusBadge.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(191, 219, 254), 1, true),
            new EmptyBorder(3, 10, 3, 10)
        ));
        barcodeStatusBadge.setBounds(500, 48, 190, 24);
        barcodePanel.add(barcodeStatusBadge);
        setBarcodeStatus("Ready to scan", new Color(239, 246, 255), new Color(30, 64, 175), new Color(191, 219, 254));

        installEditableMoneyField(itemDiscountTxt, false);

        barcodePanel.repaint();
        barcodePanel.revalidate();
    }

    private void buildCartPanel() {
        cartPanel.removeAll();
        cartPanel.setLayout(null);

        JLabel cartTitle = new JLabel("CART ITEMS");
        cartTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cartTitle.setForeground(PRIMARY);
        cartTitle.setBounds(10, 10, 180, 25);
        cartPanel.add(cartTitle);

        String[] columns = {
            "Product ID",
            "Barcode",
            "Product Name",
            "Qty",
            "Unit Price",
            "Discount",
            "Subtotal"
        };

        cartModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        cartTable = new JTable(cartModel);
        cartTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cartTable.setRowHeight(28);
        cartTable.setSelectionBackground(new Color(219, 234, 254));
        cartTable.setSelectionForeground(TEXT_DARK);
        cartTable.setGridColor(new Color(235, 235, 235));
        cartTable.setShowVerticalLines(false);
        cartTable.setShowHorizontalLines(true);
        cartTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader header = cartTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 32));

        cartTable.getColumnModel().getColumn(0).setMinWidth(0);
        cartTable.getColumnModel().getColumn(0).setMaxWidth(0);
        cartTable.getColumnModel().getColumn(0).setWidth(0);

        cartTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(55);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(95);
        cartTable.getColumnModel().getColumn(5).setPreferredWidth(85);
        cartTable.getColumnModel().getColumn(6).setPreferredWidth(100);

        cartTable.getColumnModel().getColumn(4).setCellRenderer(new MoneyRenderer());
        cartTable.getColumnModel().getColumn(5).setCellRenderer(new MoneyRenderer());
        cartTable.getColumnModel().getColumn(6).setCellRenderer(new MoneyRenderer());

        cartTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedCartProductImage();
            }
        });

        cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(new LineBorder(BORDER, 1, true));
        cartScroll.setBounds(10, 45, cartPanel.getWidth() - 20, cartPanel.getHeight() - 55);
        cartPanel.add(cartScroll);

        cartPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (cartScroll != null) {
                    cartScroll.setBounds(10, 45, cartPanel.getWidth() - 20, cartPanel.getHeight() - 55);
                }
            }
        });

        cartPanel.revalidate();
        cartPanel.repaint();
    }
    
    
    private void buildPaymentPanel() {
        paymentPanel.removeAll();
        paymentPanel.setLayout(new BorderLayout());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel paymentTitle = new JLabel("PAYMENT DETAILS");
        paymentTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        paymentTitle.setForeground(PRIMARY);
        detailsPanel.add(paymentTitle, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(6, 0, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JPanel tabPanel = createPaymentTabsPanel(true);
        tabPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, tabPanel.getPreferredSize().height));
        detailsPanel.add(tabPanel, gbc);


        // Add summary panel before payment content
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 6, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JPanel summaryPanel = createSummaryPanel();
        detailsPanel.add(summaryPanel, gbc);

        // Now add payment content
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        paymentContentPanel.setMinimumSize(new Dimension(220, 180));
        paymentContentPanel.setPreferredSize(new Dimension(220, 200));

        JScrollPane paymentFormScroll = new JScrollPane(paymentContentPanel);
        paymentFormScroll.setBorder(BorderFactory.createEmptyBorder());
        paymentFormScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        paymentFormScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        paymentFormScroll.getViewport().setBackground(Color.WHITE);
        detailsPanel.add(paymentFormScroll, gbc);

        wrapper.add(detailsPanel, BorderLayout.CENTER);
        paymentPanel.add(wrapper, BorderLayout.CENTER);

        paymentPanel.revalidate();
        paymentPanel.repaint();
    }
    
    
    private void buildImagePanel() {
        if (imagePanel == null) {
            return;
        }

        imagePanel.removeAll();
        imagePanel.setLayout(null);
        imagePanel.setBackground(Color.WHITE);
        imagePanel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        productImageLbl = new JLabel("No Image", SwingConstants.CENTER);
        productImageLbl.setOpaque(true);
        productImageLbl.setBackground(Color.WHITE);
        productImageLbl.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        productImageLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        productImageLbl.setForeground(Color.GRAY);

        stockInfoPanel = new JPanel(new BorderLayout(8, 0));
        stockInfoPanel.setOpaque(true);
        stockInfoPanel.setBackground(new Color(248, 250, 252));
        stockInfoPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(203, 213, 225), 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));

        stockLabelLbl = new JLabel("Stock Quantity");
        stockLabelLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        stockLabelLbl.setForeground(TEXT_MUTED);

        stockValueLbl = new JLabel("--", SwingConstants.RIGHT);
        stockValueLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        stockValueLbl.setForeground(PRIMARY);

        stockInfoPanel.add(stockLabelLbl, BorderLayout.WEST);
        stockInfoPanel.add(stockValueLbl, BorderLayout.EAST);

        imagePanel.add(productImageLbl);
        imagePanel.add(stockInfoPanel);

        resizeImageLabel();
        updateStockDisplay(null);

        imagePanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeImageLabel();

                if (selectedProductImageBytes != null && selectedProductImageBytes.length > 0) {
                    showProductImage(selectedProductImageBytes);
                }
            }
        });

        imagePanel.revalidate();
        imagePanel.repaint();
    }

    private void resizeImageLabel() {
        if (imagePanel == null || productImageLbl == null || stockInfoPanel == null) {
            return;
        }

        int panelWidth = imagePanel.getWidth();
        int panelHeight = imagePanel.getHeight();

        if (panelWidth <= 20) {
            panelWidth = 160;
        }
        if (panelHeight <= 20) {
            panelHeight = 220;
        }

        int cardHeight = Math.max(46, Math.min(56, panelHeight / 4));
        int verticalGap = 10;
        int lblWidth = panelWidth - 20;
        int lblHeight = panelHeight - (20 + cardHeight + verticalGap);

        if (lblHeight < 80) {
            lblHeight = 80;
        }

        productImageLbl.setBounds(10, 10, lblWidth, lblHeight);
        stockInfoPanel.setBounds(10, 10 + lblHeight + verticalGap, lblWidth, cardHeight);
    }

    private void updateStockDisplay(Integer stockQty) {
        if (stockValueLbl == null) {
            return;
        }

        if (stockQty == null) {
            stockValueLbl.setText("--");
            stockValueLbl.setForeground(TEXT_MUTED);
            return;
        }

        stockValueLbl.setText(String.valueOf(Math.max(0, stockQty)));

        if (stockQty <= 0) {
            stockValueLbl.setForeground(DANGER);
        } else if (stockQty <= 10) {
            stockValueLbl.setForeground(WARNING);
        } else {
            stockValueLbl.setForeground(SUCCESS);
        }
    }

    private void showProductImage(byte[] imageBytes) {
        if (productImageLbl == null) {
            return;
        }

        productImageLbl.setIcon(null);

        if (imageBytes == null || imageBytes.length == 0) {
            productImageLbl.setText("No Image");
            return;
        }

        try {
            BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(imageBytes));

            if (buffered == null) {
                productImageLbl.setText("Invalid Image");
                return;
            }

            int w = productImageLbl.getWidth();
            int h = productImageLbl.getHeight();

            if (w <= 0) {
                w = 150;
            }
            if (h <= 0) {
                h = 180;
            }

            Image scaled = buffered.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            productImageLbl.setIcon(new ImageIcon(scaled));
            productImageLbl.setText("");
        } catch (Exception e) {
            e.printStackTrace();
            productImageLbl.setText("Invalid Image");
        }
    }

    private void loadProductImageByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            currentPreviewBarcode = null;
            selectedProductImageBytes = null;
            showProductImage(null);
            updateStockDisplay(null);
            setBarcodeStatus("Ready to scan", new Color(239, 246, 255), new Color(30, 64, 175), new Color(191, 219, 254));
            return;
        }

        String normalizedBarcode = barcode.trim();
        currentPreviewBarcode = normalizedBarcode;

        String sql = "SELECT product_id, product_image, stock_quantity FROM products WHERE barcode = ?";

        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, normalizedBarcode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String imageText = rs.getString("product_image");
                    int productId = rs.getInt("product_id");
                    int stockQty = rs.getInt("stock_quantity");
                    int reservedQty = getReservedCartQty(productId);
                    int availableQty = Math.max(0, stockQty - reservedQty);
                    updateStockDisplay(availableQty);
                    if (availableQty <= 0) {
                        setBarcodeStatus("Out of stock", new Color(254, 242, 242), new Color(185, 28, 28), new Color(252, 165, 165));
                    } else {
                        setBarcodeStatus("Product found", new Color(240, 253, 244), new Color(21, 128, 61), new Color(134, 239, 172));
                    }

                    if (imageText == null || imageText.trim().isEmpty()) {
                        selectedProductImageBytes = null;
                        showProductImage(null);
                        return;
                    }

                    imageText = imageText.trim();

                    try {
                        File file = new File(imageText);

                        if (file.exists()) {
                            BufferedImage img = ImageIO.read(file);

                            if (img != null) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ImageIO.write(img, "png", baos);
                                selectedProductImageBytes = baos.toByteArray();
                                showProductImage(selectedProductImageBytes);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        if (imageText.startsWith("data:image")) {
                            int commaIndex = imageText.indexOf(",");
                            if (commaIndex != -1) {
                                imageText = imageText.substring(commaIndex + 1);
                            }
                        }

                        byte[] decoded = Base64.getDecoder().decode(imageText);
                        selectedProductImageBytes = decoded;
                        showProductImage(decoded);
                        return;
                    } catch (Exception ignored) {
                    }

                    selectedProductImageBytes = null;
                    showProductImage(null);
                } else {
                    currentPreviewBarcode = null;
                    selectedProductImageBytes = null;
                    showProductImage(null);
                    updateStockDisplay(null);
                    setBarcodeStatus("Product not found", new Color(255, 247, 237), new Color(194, 65, 12), new Color(253, 186, 116));
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            currentPreviewBarcode = null;
            selectedProductImageBytes = null;
            showProductImage(null);
            updateStockDisplay(null);
            setBarcodeStatus("Lookup error", new Color(254, 242, 242), new Color(185, 28, 28), new Color(252, 165, 165));
        }
    }

    private void setBarcodeStatus(String text, Color bg, Color fg, Color borderColor) {
        if (barcodeStatusBadge == null) {
            return;
        }

        barcodeStatusBadge.setText(text);
        barcodeStatusBadge.setBackground(bg);
        barcodeStatusBadge.setForeground(fg);
        barcodeStatusBadge.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderColor, 1, true),
                new EmptyBorder(3, 10, 3, 10)
        ));
    }

    private int getReservedCartQty(int productId) {
        if (cartModel == null || productId <= 0) {
            return 0;
        }

        int reservedQty = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            Object rowProductObj = cartModel.getValueAt(i, 0);
            Object rowQtyObj = cartModel.getValueAt(i, 3);

            if (rowProductObj == null || rowQtyObj == null) {
                continue;
            }

            try {
                int rowProductId = Integer.parseInt(rowProductObj.toString());
                if (rowProductId == productId) {
                    reservedQty += Integer.parseInt(rowQtyObj.toString());
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return Math.max(0, reservedQty);
    }

    private void refreshCurrentPreviewStock() {
        if (cartTable == null || cartModel == null || cartModel.getRowCount() == 0) {
            clearPreviewSelection();
            return;
        }

        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow == -1) {
            clearPreviewSelection();
            return;
        }

        int modelRow = cartTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= cartModel.getRowCount()) {
            clearPreviewSelection();
            return;
        }

        String barcode = String.valueOf(cartModel.getValueAt(modelRow, 1));
        loadProductImageByBarcode(barcode);
    }

    private void clearPreviewSelection() {
        currentPreviewBarcode = null;
        selectedProductImageBytes = null;
        showProductImage(null);
        updateStockDisplay(null);
        setBarcodeStatus("Ready to scan", new Color(239, 246, 255), new Color(30, 64, 175), new Color(191, 219, 254));
    }

    private void loadSelectedCartProductImage() {
        if (cartTable == null) {
            showProductImage(null);
            updateStockDisplay(null);
            return;
        }

        int row = cartTable.getSelectedRow();
        if (row == -1) {
            showProductImage(null);
            updateStockDisplay(null);
            return;
        }

        int modelRow = cartTable.convertRowIndexToModel(row);
        String barcode = String.valueOf(cartModel.getValueAt(modelRow, 1));
        loadProductImageByBarcode(barcode);
    }

    // Clean version: createSummaryPanel
    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 0, 4, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Dimension fieldSize = new Dimension(120, 32);

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.4;
        panel.add(createFormLabel("VATable Sales"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        subtotalTxt = createModernReadOnlyField();
        subtotalTxt.setPreferredSize(fieldSize);
        subtotalTxt.setMinimumSize(fieldSize);
        subtotalTxt.setMaximumSize(fieldSize);
        panel.add(subtotalTxt, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.4;
        panel.add(createFormLabel("VAT (12%)"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        vatTxt = createModernReadOnlyField();
        vatTxt.setPreferredSize(fieldSize);
        vatTxt.setMinimumSize(fieldSize);
        vatTxt.setMaximumSize(fieldSize);
        panel.add(vatTxt, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.4;
        panel.add(createFormLabel("Discount Amount"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        discountTxt = createModernReadOnlyField();
        discountTxt.setPreferredSize(fieldSize);
        discountTxt.setMinimumSize(fieldSize);
        discountTxt.setMaximumSize(fieldSize);
        panel.add(discountTxt, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.4;
        panel.add(createFormLabel("Total Amount"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        totalTxt = createModernReadOnlyField();
        totalTxt.setPreferredSize(fieldSize);
        totalTxt.setMinimumSize(fieldSize);
        totalTxt.setMaximumSize(fieldSize);
        totalTxt.setForeground(SUCCESS);
        totalTxt.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(totalTxt, gbc);

        return panel;
    }

    private JPanel createPaymentTabsPanel() {
        return createPaymentTabsPanel(false);
    }

    // Overload for small/large tabs
    private JPanel createPaymentTabsPanel(boolean smallTabs) {
        JPanel tabPanel = new JPanel();
        tabPanel.setLayout(new GridBagLayout());
        tabPanel.setBackground(Color.WHITE);
        tabPanel.setBorder(new EmptyBorder(0, 0, 8, 0));
        tabPanel.setPreferredSize(null);
        tabPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        cashTabBtn = createPaymentTabButton("Cash", smallTabs);
        gcashTabBtn = createPaymentTabButton("GCash", smallTabs);
        cardTabBtn = createPaymentTabButton("Card", smallTabs);

        cashTabBtn.addActionListener(e -> {
            setActiveTab("Cash", cashTabBtn);
            showPaymentPanel("Cash");
        });
        gcashTabBtn.addActionListener(e -> {
            setActiveTab("GCash", gcashTabBtn);
            showPaymentPanel("GCash");
        });
        cardTabBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(posPanel, "Card payment is still not available for today.");
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 6);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        tabPanel.add(cashTabBtn, gbc);
        gbc.gridx = 1;
        tabPanel.add(gcashTabBtn, gbc);
        gbc.gridx = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        tabPanel.add(cardTabBtn, gbc);

        // Payment content panel is created here for use in buildPaymentPanel
        paymentContentPanel = new JPanel(new CardLayout());
        paymentContentPanel.setBackground(Color.WHITE);
        paymentContentPanel.add(createPaymentFormPanel("Cash"), "Cash");
        paymentContentPanel.add(createPaymentFormPanel("GCash"), "GCash");
        paymentContentPanel.add(createPaymentFormPanel("Card"), "Card");

        setActiveTab("Cash", cashTabBtn);
        showPaymentPanel("Cash");

        cardTabBtn.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        cardTabBtn.setToolTipText("Card payment is still not available for today.");

        return tabPanel;
    }
    

    private JButton createPaymentTabButton(String text) {
        return createPaymentTabButton(text, false);
    }

    // Overload for small/large tab buttons
    private JButton createPaymentTabButton(String text, boolean small) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, small ? 12 : 14));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBackground(new Color(229, 231, 235));
        button.setForeground(new Color(55, 65, 81));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(209, 213, 219), 1, true),
                new EmptyBorder(small ? 6 : 10, small ? 18 : 32, small ? 6 : 32, small ? 18 : 32)
        ));
        button.setPreferredSize(new Dimension(small ? 70 : 110, small ? 28 : 38));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setMargin(new Insets(0, 0, 0, 0));

        // Rounded corners and subtle shadow
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(209, 213, 219), 1, true),
                new EmptyBorder(small ? 6 : 10, small ? 18 : 32, small ? 6 : 10, small ? 18 : 32)
        ));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!button.getBackground().equals(new Color(74, 144, 226))) {
                    button.setBackground(new Color(243, 244, 246));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!button.getBackground().equals(new Color(74, 144, 226))) {
                    button.setBackground(new Color(229, 231, 235));
                }
            }
        });

        return button;
    }

    private JPanel createPaymentFormPanel(String method) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 0, 4, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // ...existing code...
        gbc.gridwidth = 1;
        gbc.insets = new Insets(6, 0, 6, 10);

        switch (method) {
            case "Cash":
                row = addCashFields(panel, gbc, row);
                break;
            case "GCash":
                row = addGCashFields(panel, gbc, row);
                break;
            case "Card":
                row = addCardFields(panel, gbc, row);
                break;
        }

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private int addCashFields(JPanel panel, GridBagConstraints gbc, int row) {
        Dimension fieldSize = new Dimension(120, 32);
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createFormLabel("Cash Tendered"), gbc);
        gbc.gridx = 1;
        cashTenderedTxt = createModernEditableField();
        cashTenderedTxt.setHorizontalAlignment(SwingConstants.RIGHT);
        cashTenderedTxt.setPreferredSize(fieldSize);
        cashTenderedTxt.setMinimumSize(fieldSize);
        cashTenderedTxt.setMaximumSize(fieldSize);
        installEditableMoneyField(cashTenderedTxt, true);
        panel.add(cashTenderedTxt, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createFormLabel("Change Amount"), gbc);
        gbc.gridx = 1;
        changeTxt = createModernReadOnlyField();
        changeTxt.setFont(new Font("Segoe UI", Font.BOLD, 14));
        changeTxt.setForeground(new Color(59, 130, 246));
        changeTxt.setPreferredSize(fieldSize);
        changeTxt.setMinimumSize(fieldSize);
        changeTxt.setMaximumSize(fieldSize);
        panel.add(changeTxt, gbc);
        row++;

        return row;
    }

    private int addGCashFields(JPanel panel, GridBagConstraints gbc, int row) {
        Dimension fieldSize = new Dimension(120, 32);

        gbc.gridx = 0; gbc.gridy = row;
        gbc.weightx = 0.4;
        panel.add(createFormLabel("Customer No"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        gcashNumberTxt = createModernEditableField();
        installDigitsOnlyField(gcashNumberTxt, 11);
        gcashNumberTxt.setPreferredSize(fieldSize);
        gcashNumberTxt.setMinimumSize(fieldSize);
        gcashNumberTxt.setMaximumSize(fieldSize);
        gcashNumberTxt.setToolTipText("Enter exactly 11 digits.");
        panel.add(gcashNumberTxt, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        gbc.weightx = 0.4;
        panel.add(createFormLabel("GCash Reference No."), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        gcashReferenceTxt = createModernEditableField();
        gcashReferenceTxt.setPreferredSize(fieldSize);
        gcashReferenceTxt.setMinimumSize(fieldSize);
        gcashReferenceTxt.setMaximumSize(fieldSize);
        panel.add(gcashReferenceTxt, gbc);
        row++;


        gbc.gridx = 0; gbc.gridy = row;
        gbc.weightx = 0.4;
        panel.add(createFormLabel("Amount Paid"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        gcashAmountTxt = createModernReadOnlyField();
        gcashAmountTxt.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gcashAmountTxt.setForeground(new Color(59, 130, 246));
        gcashAmountTxt.setPreferredSize(fieldSize);
        gcashAmountTxt.setMinimumSize(fieldSize);
        gcashAmountTxt.setMaximumSize(fieldSize);
        panel.add(gcashAmountTxt, gbc);
        row++;

        return row;
    }

    private int addCardFields(JPanel panel, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createFormLabel("Card Number"), gbc);
        gbc.gridx = 1;
        cardNumberTxt = createModernEditableField();
        panel.add(cardNumberTxt, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createFormLabel("Card Holder"), gbc);
        gbc.gridx = 1;
        cardHolderTxt = createModernEditableField();
        panel.add(cardHolderTxt, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createFormLabel("Expiry Date"), gbc);
        gbc.gridx = 1;
        cardExpiryTxt = createModernEditableField();
        panel.add(cardExpiryTxt, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createFormLabel("CVV"), gbc);
        gbc.gridx = 1;
        cardCvvTxt = createModernEditableField();
        panel.add(cardCvvTxt, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createFormLabel("Amount Paid"), gbc);
        gbc.gridx = 1;
        cardAmountTxt = createModernReadOnlyField();
        cardAmountTxt.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cardAmountTxt.setForeground(new Color(59, 130, 246));
        panel.add(cardAmountTxt, gbc);
        row++;

        return row;
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(new Color(55, 65, 81));
        return label;
    }

    private JTextField createModernReadOnlyField() {
        JTextField field = new JTextField("₱0.00");
        field.setEditable(false);
        field.setHorizontalAlignment(SwingConstants.RIGHT);
        field.setFont(new Font("Segoe UI", Font.BOLD, 13));
        field.setBackground(new Color(249, 250, 251));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(209, 213, 219), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        field.setOpaque(true);
        return field;
    }

    private JTextField createModernEditableField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(209, 213, 219), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        field.setOpaque(true);
        return field;
    }

    private void setActiveTab(String method, JButton activeBtn) {
        currentPaymentMethod = method;

        JButton[] tabs = {cashTabBtn, gcashTabBtn, cardTabBtn};
        for (JButton tab : tabs) {
            if (tab != null) {
                tab.setBackground(new Color(229, 231, 235));
                tab.setForeground(new Color(55, 65, 81));
                tab.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(209, 213, 219), 1),
                        new EmptyBorder(10, 20, 10, 20)
                ));
            }
        }

        if (activeBtn != null) {
            activeBtn.setBackground(new Color(74, 144, 226));
            activeBtn.setForeground(Color.WHITE);
            activeBtn.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(53, 122, 189), 1),
                    new EmptyBorder(10, 20, 10, 20)
            ));
        }

        updatePaymentMethod();
    }

    private void showPaymentPanel(String method) {
        CardLayout cl = (CardLayout) paymentContentPanel.getLayout();
        cl.show(paymentContentPanel, method);
    }

    private void buildButtonPanel() {
        buttonPanel.removeAll();

        checkoutBtn = createButton("CHECKOUT", SUCCESS, Color.WHITE);
        checkoutBtn.setBounds(20, 15, 130, 40);
        buttonPanel.add(checkoutBtn);

        cancelBtn = createButton("CANCEL", DANGER, Color.WHITE);
        cancelBtn.setBounds(165, 15, 110, 40);
        buttonPanel.add(cancelBtn);

        printBtn = createButton("PRINT RECEIPT", PRIMARY_SOFT, Color.WHITE);
        printBtn.setBounds(290, 15, 150, 40);
        buttonPanel.add(printBtn);

        buttonPanel.repaint();
        buttonPanel.revalidate();
    }

    private JLabel createSmallLabel(String text, int x, int y) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(TEXT_DARK);
        lbl.setBounds(x, y, 80, 30);
        return lbl;
    }

    private JLabel createFieldLabel(String text, int x, int y) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_DARK);
        lbl.setBounds(x, y, 140, 30);
        return lbl;
    }

    private JTextField createReadOnlyField(int x, int y, int w, int h) {
        JTextField txt = new JTextField("₱0.00");
        txt.setBounds(x, y, w, h);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txt.setEditable(false);
        txt.setHorizontalAlignment(SwingConstants.RIGHT);
        txt.setBackground(FIELD_BG);
        txt.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(5, 8, 5, 8)
        ));
        return txt;
    }

    private JTextField createInputField(int x, int y, int w, int h) {
        JTextField txt = new JTextField();
        txt.setBounds(x, y, w, h);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txt.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(5, 8, 5, 8)
        ));
        txt.setBackground(Color.WHITE);
        return txt;
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void wireEvents() {
        addItemBtn.addActionListener(e -> addProduct());
        removeItemBtn.addActionListener(e -> removeSelectedItem());
        refreshBtn.addActionListener(e -> refreshPosView());
        searchProductBtn.addActionListener(e -> openProductSearchPopup());

        barcodeTxt.addActionListener(e -> {
            updatePreviewFromBarcodeInput();
            qtySpinner.requestFocusInWindow();
        });

        barcodePreviewTimer = new Timer(220, e -> updatePreviewFromBarcodeInput());
        barcodePreviewTimer.setRepeats(false);

        barcodeTxt.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                barcodePreviewTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                barcodePreviewTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                barcodePreviewTimer.restart();
            }
        });

        checkoutBtn.addActionListener(e -> checkoutSale());

        cancelBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    posPanel,
                    "Clear current cart?",
                    "Cancel Sale",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                resetPOS();
            }
        });

        printBtn.addActionListener(e -> printReceipt());

        cartTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && cartTable.getSelectedRow() != -1) {
                    loadSelectedCartRowToInputs();
                }
            }
        });
    }

    private void updatePreviewFromBarcodeInput() {
        if (barcodeTxt == null) {
            return;
        }

        String barcode = barcodeTxt.getText().trim();
        if (barcode.isEmpty()) {
            if (cartTable != null && cartTable.getSelectedRow() != -1) {
                refreshCurrentPreviewStock();
            } else {
                clearPreviewSelection();
            }
            return;
        }

        loadProductImageByBarcode(barcode);
    }

    private void refreshPosView() {
        String barcode = barcodeTxt == null ? "" : barcodeTxt.getText().trim();

        if (!barcode.isEmpty()) {
            updatePreviewFromBarcodeInput();
        } else {
            refreshCurrentPreviewStock();
        }

        calculateTotals();
        if (barcodeTxt != null) {
            barcodeTxt.requestFocusInWindow();
        }
    }

    private void installKeyboardShortcuts() {
        InputMap inputMap = posPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = posPanel.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "checkout");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "searchProduct");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "cancelSale");

        actionMap.put("checkout", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkoutSale();
            }
        });

        actionMap.put("searchProduct", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openProductSearchPopup();
            }
        });

        actionMap.put("cancelSale", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                        posPanel,
                        "Clear current cart?",
                        "Cancel Sale",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    resetPOS();
                }
            }
        });
    }

    private void startDateTimeTimer() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateTimer = new Timer(1000, e -> dateTimeLbl.setText("Date/Time: " + sdf.format(new Date())));
        dateTimer.start();
    }

    private void installBarcodeScannerDetection() {
        scannerTimer = new Timer(120, e -> {
            // Keep focus on barcode field while typing.
            // Scanner workflows are already handled by Enter key action.
        });
        scannerTimer.setRepeats(false);

        barcodeTxt.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scannerTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scannerTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scannerTimer.restart();
            }
        });
    }

    private boolean looksLikeScannerInput(String text) {
        if (text == null || text.isEmpty()) return false;
        if (text.length() < 6) return false;
        return text.matches("[A-Za-z0-9\\-]+");
    }

    private void installDigitsOnlyField(JTextField field, int maxLength) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string == null) {
                    return;
                }

                String oldText = fb.getDocument().getText(0, fb.getDocument().getLength());
                String candidate = new StringBuilder(oldText).insert(offset, string).toString();
                if (candidate.matches("\\d{0," + maxLength + "}")) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                String replacement = (text == null) ? "" : text;
                String oldText = fb.getDocument().getText(0, fb.getDocument().getLength());
                String candidate = new StringBuilder(oldText).replace(offset, offset + length, replacement).toString();
                if (candidate.matches("\\d{0," + maxLength + "}")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                super.remove(fb, offset, length);
            }
        });
    }

    
    
    private void installEditableMoneyField(JTextField field, boolean cashField) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string == null) {
                    return;
                }

                String oldText = fb.getDocument().getText(0, fb.getDocument().getLength());
                String newText = new StringBuilder(oldText).insert(offset, string).toString();

                if (isValidMoneyEntry(newText)) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                String oldText = fb.getDocument().getText(0, fb.getDocument().getLength());
                String replacement = (text == null) ? "" : text;
                String newText = new StringBuilder(oldText).replace(offset, offset + length, replacement).toString();

                if (isValidMoneyEntry(newText)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                String oldText = fb.getDocument().getText(0, fb.getDocument().getLength());
                String newText = new StringBuilder(oldText).delete(offset, offset + length).toString();

                if (isValidMoneyEntry(newText)) {
                    super.remove(fb, offset, length);
                }
            }
        });

        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (cashField && !suppressCashTenderedListener) {
                    SwingUtilities.invokeLater(() -> updateChange());
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (cashField && !suppressCashTenderedListener) {
                    SwingUtilities.invokeLater(() -> updateChange());
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (cashField && !suppressCashTenderedListener) {
                    SwingUtilities.invokeLater(() -> updateChange());
                }
            }
        });

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String text = field.getText().trim();

                suppressCashTenderedListener = true;
                try {
                    if (text.isEmpty()) {
                        field.setText(cashField ? "" : "0.00");
                    } else {
                        BigDecimal val = parseMoney(text);
                        field.setText(formatMoney(val));
                    }
                } finally {
                    suppressCashTenderedListener = false;
                }

                if (cashField) {
                    SwingUtilities.invokeLater(() -> updateChange());
                }
            }
        });

        field.addActionListener(e -> {
            String text = field.getText().trim();

            suppressCashTenderedListener = true;
            try {
                if (text.isEmpty()) {
                    field.setText(cashField ? "" : "0.00");
                } else {
                    BigDecimal val = parseMoney(text);
                    field.setText(formatMoney(val));
                }
            } finally {
                suppressCashTenderedListener = false;
            }

            if (cashField) {
                SwingUtilities.invokeLater(() -> updateChange());
            }
        });
    }

    

    private boolean isValidMoneyEntry(String text) {
        if (text == null || text.isEmpty()) return true;
        String cleaned = text.replace(",", "").trim();
        return cleaned.matches("\\d{0,15}(\\.\\d{0,2})?");
    }

    private void resetPOS() {
        clearCart();
        clearItemEntryFields();
        clearPreviewSelection();

        if (subtotalTxt != null) {
            subtotalTxt.setText("₱0.00");
        }
        if (vatTxt != null) {
            vatTxt.setText("₱0.00");
        }
        if (discountTxt != null) {
            discountTxt.setText("₱0.00");
        }
        if (totalTxt != null) {
            totalTxt.setText("₱0.00");
        }

        if (cashTenderedTxt != null) {
            cashTenderedTxt.setText("");
        }
        if (changeTxt != null) {
            changeTxt.setText("₱0.00");
        }

        if (gcashNumberTxt != null) {
            gcashNumberTxt.setText("");
        }
        if (gcashReferenceTxt != null) {
            gcashReferenceTxt.setText("");
        }
        if (gcashAmountTxt != null) {
            gcashAmountTxt.setText("₱0.00");
        }

        if (cardNumberTxt != null) {
            cardNumberTxt.setText("");
        }
        if (cardHolderTxt != null) {
            cardHolderTxt.setText("");
        }
        if (cardExpiryTxt != null) {
            cardExpiryTxt.setText("");
        }
        if (cardCvvTxt != null) {
            cardCvvTxt.setText("");
        }
        if (cardAmountTxt != null) {
            cardAmountTxt.setText("₱0.00");
        }

        currentPaymentMethod = "Cash";
        if (cashTabBtn != null && paymentContentPanel != null) {
            setActiveTab("Cash", cashTabBtn);
            showPaymentPanel("Cash");
        }

        if (invoiceLbl != null) {
            invoiceLbl.setText("Invoice: " + generateInvoiceNo());
        }

        SwingUtilities.invokeLater(() -> {
            updatePaymentMethod();
            if (barcodeTxt != null) {
                barcodeTxt.requestFocusInWindow();
            }
        });
    }

    
    private void clearCart() {
        if (cartModel != null) {
            cartModel.setRowCount(0);
        }
    }

    private void clearItemEntryFields() {
        barcodeTxt.setText("");
        qtySpinner.setValue(1);
        itemDiscountTxt.setText("0.00");
        cartTable.clearSelection();
        clearPreviewSelection();
        barcodeTxt.requestFocusInWindow();
    }

    private String generateInvoiceNo() {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmssSSS").format(new Date());
        int random = (int) (Math.random() * 9000) + 1000;
        return "INV-" + timestamp + "-" + random;
    }

    private void openProductSearchPopup() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(posPanel), "Search Product", true);
        dialog.setSize(780, 460);
        dialog.setLocationRelativeTo(posPanel);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setBorder(new EmptyBorder(10, 10, 0, 10));

        JTextField searchTxt = new JTextField();
        searchTxt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        top.add(searchTxt, BorderLayout.CENTER);

        JButton selectBtn = createButton("SELECT PRODUCT", ACCENT, Color.WHITE);
        top.add(selectBtn, BorderLayout.EAST);

        dialog.add(top, BorderLayout.NORTH);

        String[] cols = {"Product ID", "Barcode", "Product Name", "Price", "Stock"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);
        table.getColumnModel().getColumn(3).setCellRenderer(new MoneyRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(0, 10, 10, 10));
        dialog.add(scroll, BorderLayout.CENTER);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        String sql = "SELECT product_id, barcode, name, selling_price, stock_quantity FROM products ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("product_id"),
                        rs.getString("barcode"),
                        rs.getString("name"),
                        rs.getBigDecimal("selling_price"),
                        rs.getInt("stock_quantity")
                });
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(posPanel, "Failed to load products.");
        }

        searchTxt.getDocument().addDocumentListener(new DocumentListener() {
            private void filter() {
                String text = searchTxt.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filter();
            }
        });

        Runnable selectAction = () -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(dialog, "Please select a product.");
                return;
            }

            int modelRow = table.convertRowIndexToModel(row);
            String barcode = String.valueOf(model.getValueAt(modelRow, 1));

            barcodeTxt.setText(barcode);
            qtySpinner.setValue(1);
            itemDiscountTxt.setText("0.00");
            cartTable.clearSelection();
            updatePreviewFromBarcodeInput();
            barcodeTxt.requestFocusInWindow();

            dialog.dispose();
        };

        selectBtn.addActionListener(e -> selectAction.run());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectAction.run();
                }
            }
        });

        dialog.setVisible(true);
    }

    private void addProduct() {
        String barcode = barcodeTxt.getText().trim();

        if (barcode.isEmpty()) {
            JOptionPane.showMessageDialog(posPanel, "Please scan barcode or search product.");
            return;
        }

        ProductData product = getProductByBarcode(barcode);

        if (product == null) {
            JOptionPane.showMessageDialog(posPanel, "Product not found.");
            clearPreviewSelection();
            barcodeTxt.requestFocusInWindow();
            barcodeTxt.selectAll();
            return;
        }

        int qty = (Integer) qtySpinner.getValue();
        BigDecimal discount = parseMoney(itemDiscountTxt.getText());

        if (qty <= 0) {
            JOptionPane.showMessageDialog(posPanel, "Quantity must be greater than 0.");
            return;
        }

        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            JOptionPane.showMessageDialog(posPanel, "Discount cannot be negative.");
            return;
        }

        upsertCartItem(product, qty, discount);
        clearItemEntryFields();
        calculateTotals();
    }

    private void upsertCartItem(ProductData product, int enteredQty, BigDecimal enteredDiscount) {
        int selectedRow = cartTable.getSelectedRow();

        // Edit mode: if selected row is same product, replace values
        if (selectedRow != -1) {
            int modelRow = cartTable.convertRowIndexToModel(selectedRow);
            int selectedProductId = Integer.parseInt(String.valueOf(cartModel.getValueAt(modelRow, 0)));

            if (selectedProductId == product.productId) {
                if (enteredQty > product.stockQuantity) {
                    JOptionPane.showMessageDialog(posPanel, "Insufficient stock.");
                    return;
                }

                BigDecimal gross = product.sellingPrice.multiply(BigDecimal.valueOf(enteredQty));
                if (enteredDiscount.compareTo(gross) > 0) {
                    JOptionPane.showMessageDialog(posPanel, "Discount is too high.");
                    return;
                }

                BigDecimal subtotal = gross.subtract(enteredDiscount);

                cartModel.setValueAt(enteredQty, modelRow, 3);
                cartModel.setValueAt(product.sellingPrice, modelRow, 4);
                cartModel.setValueAt(enteredDiscount, modelRow, 5);
                cartModel.setValueAt(subtotal, modelRow, 6);

                calculateTotals();
                cartTable.setRowSelectionInterval(modelRow, modelRow);
                refreshCurrentPreviewStock();
                return;
            }
        }

        // Add mode: merge if same product already exists
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            int rowProductId = Integer.parseInt(String.valueOf(cartModel.getValueAt(i, 0)));

            if (rowProductId == product.productId) {
                int currentQty = Integer.parseInt(String.valueOf(cartModel.getValueAt(i, 3)));
                BigDecimal currentDiscount = parseMoney(String.valueOf(cartModel.getValueAt(i, 5)));

                int newQty = currentQty + enteredQty;
                BigDecimal newDiscount = currentDiscount.add(enteredDiscount);

                if (newQty > product.stockQuantity) {
                    JOptionPane.showMessageDialog(posPanel, "Insufficient stock.");
                    return;
                }

                BigDecimal gross = product.sellingPrice.multiply(BigDecimal.valueOf(newQty));
                if (newDiscount.compareTo(gross) > 0) {
                    JOptionPane.showMessageDialog(posPanel, "Discount is too high.");
                    return;
                }

                BigDecimal subtotal = gross.subtract(newDiscount);

                cartModel.setValueAt(newQty, i, 3);
                cartModel.setValueAt(product.sellingPrice, i, 4);
                cartModel.setValueAt(newDiscount, i, 5);
                cartModel.setValueAt(subtotal, i, 6);

                calculateTotals();
                cartTable.setRowSelectionInterval(i, i);
                refreshCurrentPreviewStock();
                return;
            }
        }

        // New row
        if (enteredQty > product.stockQuantity) {
            JOptionPane.showMessageDialog(posPanel, "Insufficient stock.");
            return;
        }

        BigDecimal gross = product.sellingPrice.multiply(BigDecimal.valueOf(enteredQty));
        if (enteredDiscount.compareTo(gross) > 0) {
            JOptionPane.showMessageDialog(posPanel, "Discount is too high.");
            return;
        }

        BigDecimal subtotal = gross.subtract(enteredDiscount);

        cartModel.addRow(new Object[]{
                product.productId,
                product.barcode,
                product.name,
                enteredQty,
                product.sellingPrice,
                enteredDiscount,
                subtotal
        });

        calculateTotals();
        int lastRow = cartModel.getRowCount() - 1;
        cartTable.setRowSelectionInterval(lastRow, lastRow);
        refreshCurrentPreviewStock();
    }

    private void minusSelectedItemQty() {
        int row = cartTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(posPanel, "Please select an item first.");
            return;
        }

        int modelRow = cartTable.convertRowIndexToModel(row);
        int currentQty = Integer.parseInt(String.valueOf(cartModel.getValueAt(modelRow, 3)));
        int minusQty = (Integer) qtySpinner.getValue();

        if (minusQty <= 0) {
            JOptionPane.showMessageDialog(posPanel, "Quantity must be greater than 0.");
            return;
        }

        int newQty = currentQty - minusQty;

        if (newQty <= 0) {
            int confirm = JOptionPane.showConfirmDialog(
                    posPanel,
                    "Quantity will become zero or below. Remove item?",
                    "Remove Item",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                cartModel.removeRow(modelRow);
                computeTotals();
                clearItemEntryFields();
                refreshCurrentPreviewStock();
            }
            return;
        }

        BigDecimal unitPrice = parseMoney(String.valueOf(cartModel.getValueAt(modelRow, 4)));
        BigDecimal discount = parseMoney(String.valueOf(cartModel.getValueAt(modelRow, 5)));
        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(newQty));

        if (discount.compareTo(gross) > 0) {
            discount = BigDecimal.ZERO;
        }

        BigDecimal subtotal = gross.subtract(discount);

        cartModel.setValueAt(newQty, modelRow, 3);
        cartModel.setValueAt(discount, modelRow, 5);
        cartModel.setValueAt(subtotal, modelRow, 6);

        calculateTotals();
        cartTable.setRowSelectionInterval(modelRow, modelRow);
        refreshCurrentPreviewStock();
    }

    private void removeSelectedItem() {
        int row = cartTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(posPanel, "Please select an item to remove.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                posPanel,
                "Remove selected item?",
                "Remove Item",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            int modelRow = cartTable.convertRowIndexToModel(row);
            cartModel.removeRow(modelRow);
            calculateTotals();
            clearItemEntryFields();
            refreshCurrentPreviewStock();
        }
            // Listen for quantity or discount changes in the table
            cartModel.addTableModelListener(e -> calculateTotals());

            // Listen for changes in cashTenderedTxt to update change
            cashTenderedTxt.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) { calculateTotals(); }
                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) { calculateTotals(); }
                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) { calculateTotals(); }
            });
    }

    private void loadSelectedCartRowToInputs() {
        int row = cartTable.getSelectedRow();

        if (row == -1) {
            return;
        }

        int modelRow = cartTable.convertRowIndexToModel(row);

        String barcode = String.valueOf(cartModel.getValueAt(modelRow, 1));
        int qty = Integer.parseInt(String.valueOf(cartModel.getValueAt(modelRow, 3)));
        BigDecimal discount = parseMoney(String.valueOf(cartModel.getValueAt(modelRow, 5)));

        barcodeTxt.setText(barcode);
        qtySpinner.setValue(qty);
        itemDiscountTxt.setText(formatMoney(discount));
    }

    private void computeTotals() {
        // (Replaced by calculateTotals)
    }

    private void updatePaymentMethod() {
        if (updatingPaymentUI) {
            return;
        }
        if (totalTxt == null) {
            return;
        }

        try {
            updatingPaymentUI = true;
            BigDecimal total = parseMoney(totalTxt.getText());

            if ("Cash".equalsIgnoreCase(currentPaymentMethod)) {
                if (cashTenderedTxt != null) {
                    cashTenderedTxt.setEditable(true);
                    cashTenderedTxt.setEnabled(true);
                    cashTenderedTxt.setBackground(Color.WHITE);
                    updateChange();
                }
            } else if ("GCash".equalsIgnoreCase(currentPaymentMethod)) {
                if (cashTenderedTxt != null) {
                    cashTenderedTxt.setEditable(false);
                    cashTenderedTxt.setEnabled(false);
                    cashTenderedTxt.setBackground(FIELD_BG);
                }
                if (gcashAmountTxt != null) {
                    gcashAmountTxt.setText(formatPeso(total));
                }
                if (changeTxt != null) {
                    changeTxt.setText("₱0.00");
                }
            } else if ("Card".equalsIgnoreCase(currentPaymentMethod)) {
                if (cashTenderedTxt != null) {
                    cashTenderedTxt.setEditable(false);
                    cashTenderedTxt.setEnabled(false);
                    cashTenderedTxt.setBackground(FIELD_BG);
                }
                if (cardAmountTxt != null) {
                    cardAmountTxt.setText(formatPeso(total));
                }
                if (changeTxt != null) {
                    changeTxt.setText("₱0.00");
                }
            }
        } finally {
            updatingPaymentUI = false;
        }
    }

    private void updateChange() {
        if (updatingPaymentUI) {
            return;
        }
        if (totalTxt == null) {
            return;
        }

        updatingPaymentUI = true;
        try {
            BigDecimal total = parseMoney(totalTxt.getText());

            if ("Cash".equalsIgnoreCase(currentPaymentMethod)) {
                BigDecimal cash = BigDecimal.ZERO;
                if (cashTenderedTxt != null) {
                    cash = parseMoney(cashTenderedTxt.getText());
                }

                BigDecimal change = cash.subtract(total);

                if (changeTxt != null) {
                    if (change.compareTo(BigDecimal.ZERO) < 0) {
                        changeTxt.setText("₱0.00");
                    } else {
                        changeTxt.setText(formatPeso(change));
                    }
                }
            } else {
                if (changeTxt != null) {
                    changeTxt.setText("₱0.00");
                }
            }

            if (gcashAmountTxt != null) {
                gcashAmountTxt.setText(formatPeso(total));
            }

            if (cardAmountTxt != null) {
                cardAmountTxt.setText(formatPeso(total));
            }

        } finally {
            updatingPaymentUI = false;
        }
    }

    private void checkoutSale() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(posPanel, "Cart is empty.");
            return;
        }

        String paymentMethod = currentPaymentMethod;
        BigDecimal totalAmount = parseMoney(totalTxt.getText());
        BigDecimal subtotalAmount = parseMoney(subtotalTxt.getText());
        BigDecimal vatAmount = parseMoney(vatTxt.getText());
        BigDecimal discountAmount = parseMoney(discountTxt.getText());
        BigDecimal cashTendered;
        BigDecimal changeAmount;

        if ("Cash".equalsIgnoreCase(paymentMethod)) {
            cashTendered = parseMoney(cashTenderedTxt.getText());
            changeAmount = parseMoney(changeTxt.getText());

            if (cashTendered.compareTo(totalAmount) < 0) {
                JOptionPane.showMessageDialog(posPanel, "Insufficient cash tendered.");
                return;
            }

        } else if ("GCash".equalsIgnoreCase(paymentMethod)) {
            String gcashCustomerNo = gcashNumberTxt.getText().trim();

            if (gcashCustomerNo.isEmpty()) {
                JOptionPane.showMessageDialog(posPanel, "Customer No is required for GCash.");
                return;
            }

            if (!gcashCustomerNo.matches("\\d{11}")) {
                JOptionPane.showMessageDialog(posPanel, "Customer No must be exactly 11 digits.");
                return;
            }

            if (gcashReferenceTxt.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(posPanel, "GCash Reference No. is required.");
                return;
            }

            cashTendered = totalAmount;
            changeAmount = BigDecimal.ZERO;

        } else if ("Card".equalsIgnoreCase(paymentMethod)) {
            if (cardNumberTxt.getText().trim().isEmpty()
                    || cardHolderTxt.getText().trim().isEmpty()
                    || cardExpiryTxt.getText().trim().isEmpty()
                    || cardCvvTxt.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(posPanel, "Complete card details first.");
                return;
            }

            cashTendered = totalAmount;
            changeAmount = BigDecimal.ZERO;

        } else {
            cashTendered = BigDecimal.ZERO;
            changeAmount = BigDecimal.ZERO;
        }

        int confirm = JOptionPane.showConfirmDialog(
                posPanel,
                "Proceed checkout?",
                "Confirm Sale",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        Connection conn = null;
        PreparedStatement psSale = null;
        PreparedStatement psSaleItem = null;
        PreparedStatement psUpdateStock = null;
        PreparedStatement psInventory = null;
        PreparedStatement psLog = null;
        ResultSet rsKeys = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int userId = getUserIdByUsername(loggedInUsername, conn);
            if (userId <= 0) {
                throw new SQLException("Logged-in user not found.");
            }

            String invoiceNo = generateInvoiceNo();
            invoiceLbl.setText("Invoice: " + invoiceNo);

            String saleSql = "INSERT INTO sales "
                    + "(invoice_number, user_id, subtotal_amount, vat_amount, discount_amount, total_amount, payment_method, cash_tendered, change_amount) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            psSale = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS);
            psSale.setString(1, invoiceNo);
            psSale.setInt(2, userId);
            psSale.setBigDecimal(3, subtotalAmount);
            psSale.setBigDecimal(4, vatAmount);
            psSale.setBigDecimal(5, discountAmount);
            psSale.setBigDecimal(6, totalAmount);
            psSale.setString(7, paymentMethod);
            psSale.setBigDecimal(8, cashTendered);
            psSale.setBigDecimal(9, changeAmount);
            psSale.executeUpdate();

            rsKeys = psSale.getGeneratedKeys();
            int saleId = 0;
            if (rsKeys.next()) {
                saleId = rsKeys.getInt(1);
            }

            if (saleId <= 0) {
                throw new SQLException("Failed to create sale.");
            }

            boolean hasCostSnapshotColumn = ensureSaleItemsCostSnapshotColumn(conn);

            String saleItemSql;
            if (hasCostSnapshotColumn) {
                saleItemSql = "INSERT INTO sale_items "
                        + "(sale_id, product_id, quantity, unit_price, discount, total_price, cost_price_snapshot) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            } else {
                saleItemSql = "INSERT INTO sale_items "
                        + "(sale_id, product_id, quantity, unit_price, discount, total_price) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
            }

            psSaleItem = conn.prepareStatement(saleItemSql);

            String updateStockSql = "UPDATE products "
                    + "SET stock_quantity = stock_quantity - ? "
                    + "WHERE product_id = ? AND stock_quantity >= ?";

            psUpdateStock = conn.prepareStatement(updateStockSql);

            String inventorySql = "INSERT INTO inventory_transactions "
                    + "(product_id, user_id, transaction_type, quantity, reference_number, reason) "
                    + "VALUES (?, ?, 'Stock Out', ?, ?, ?)";

            psInventory = conn.prepareStatement(inventorySql);

            String logSql = "INSERT INTO transactions_log (user_id, action, description) VALUES (?, ?, ?)";
            psLog = conn.prepareStatement(logSql);

            for (int i = 0; i < cartModel.getRowCount(); i++) {
                int productId = Integer.parseInt(String.valueOf(cartModel.getValueAt(i, 0)));
                String productName = String.valueOf(cartModel.getValueAt(i, 2));
                int qty = Integer.parseInt(String.valueOf(cartModel.getValueAt(i, 3)));
                BigDecimal unitPrice = parseMoney(String.valueOf(cartModel.getValueAt(i, 4)));
                BigDecimal discount = parseMoney(String.valueOf(cartModel.getValueAt(i, 5)));
                BigDecimal subtotal = parseMoney(String.valueOf(cartModel.getValueAt(i, 6)));

                psSaleItem.setInt(1, saleId);
                psSaleItem.setInt(2, productId);
                psSaleItem.setInt(3, qty);
                psSaleItem.setBigDecimal(4, unitPrice);
                psSaleItem.setBigDecimal(5, discount);
                psSaleItem.setBigDecimal(6, subtotal);

                if (hasCostSnapshotColumn) {
                    BigDecimal costPriceSnapshot = getCurrentProductCost(productId, conn);
                    psSaleItem.setBigDecimal(7, costPriceSnapshot);
                }

                psSaleItem.addBatch();

                psUpdateStock.setInt(1, qty);
                psUpdateStock.setInt(2, productId);
                psUpdateStock.setInt(3, qty);
                int affected = psUpdateStock.executeUpdate();

                if (affected <= 0) {
                    throw new SQLException("Insufficient stock for product: " + productName);
                }

                psInventory.setInt(1, productId);
                psInventory.setInt(2, userId);
                psInventory.setInt(3, qty);
                psInventory.setString(4, invoiceNo);
                psInventory.setString(5, "POS sale");
                psInventory.addBatch();
            }

            psSaleItem.executeBatch();
            psInventory.executeBatch();

            psLog.setInt(1, userId);
            psLog.setString(2, "Sale");
            psLog.setString(3, "Processed sale " + invoiceNo + " total = " + formatPeso(totalAmount));
            psLog.executeUpdate();

            conn.commit();

            String receiptText = buildReceiptText(invoiceNo, paymentMethod, cashTendered, changeAmount);

            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(posPanel, "Checkout successful.");

                if (afterCheckoutRefresh != null) {
                    afterCheckoutRefresh.run();
                }

                showReceiptDialog(receiptText);
                resetPOS();
            });

        } catch (Exception ex) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ignored) {
            }

            JOptionPane.showMessageDialog(posPanel, "Checkout failed: " + ex.getMessage());
            ex.printStackTrace();

        } finally {
            closeQuietly(rsKeys);
            closeQuietly(psSale);
            closeQuietly(psSaleItem);
            closeQuietly(psUpdateStock);
            closeQuietly(psInventory);
            closeQuietly(psLog);

            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }

            closeQuietly(conn);
        }
    }

    private void printReceipt() {
        if (lastReceiptText == null || lastReceiptText.trim().isEmpty()) {
            JOptionPane.showMessageDialog(posPanel, "No recent receipt available.");
            return;
        }

        showReceiptDialog(lastReceiptText);
    }

    private String buildReceiptText(String invoiceNo, String paymentMethod, BigDecimal cashTendered, BigDecimal changeAmount) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("           STOCKWISE POS RECEIPT        \n");
        sb.append("========================================\n");
        sb.append("Invoice : ").append(invoiceNo).append("\n");
        sb.append("Cashier : ").append(getCashierDisplayName()).append("\n");
        sb.append("Date    : ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        sb.append("----------------------------------------\n");

        for (int i = 0; i < cartModel.getRowCount(); i++) {
            String name = String.valueOf(cartModel.getValueAt(i, 2));
            String qty = String.valueOf(cartModel.getValueAt(i, 3));
            String price = formatPeso(parseMoney(String.valueOf(cartModel.getValueAt(i, 4))));
            String discount = formatPeso(parseMoney(String.valueOf(cartModel.getValueAt(i, 5))));
            String total = formatPeso(parseMoney(String.valueOf(cartModel.getValueAt(i, 6))));

            sb.append(name).append("\n");
            sb.append("  ").append(qty)
                    .append(" x ").append(price)
                    .append("   Disc: ").append(discount)
                    .append("   =   ").append(total)
                    .append("\n");
        }

        sb.append("----------------------------------------\n");
        sb.append(String.format("%-20s %15s\n", "VATable Sales:", subtotalTxt.getText()));
        sb.append(String.format("%-20s %15s\n", "VAT (12%):", vatTxt.getText()));
        sb.append(String.format("%-20s %15s\n", "Discount:", discountTxt.getText()));
        sb.append(String.format("%-20s %15s\n", "Total:", totalTxt.getText()));
        sb.append(String.format("%-20s %15s\n", "Payment:", paymentMethod));
        sb.append(String.format("%-20s %15s\n", "Cash:", formatPeso(cashTendered)));
        sb.append(String.format("%-20s %15s\n", "Change:", formatPeso(changeAmount)));
        sb.append("========================================\n");
        sb.append("        Thank you for your purchase!    \n");
        sb.append("========================================\n");

        return sb.toString();
    }

    private String getCashierDisplayName() {
        if (loggedInFullName != null && !loggedInFullName.trim().isEmpty()) {
            return loggedInFullName.trim();
        }
        return loggedInUsername;
    }

    private void showReceiptDialog(String receiptText) {
        lastReceiptText = receiptText;

        Window owner = SwingUtilities.getWindowAncestor(posPanel);
        JDialog dialog = new JDialog(owner instanceof Frame ? (Frame) owner : null, "Receipt Preview", true);

        dialog.setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Bigger centered window, not fullscreen
        dialog.setSize(760, 820);
        dialog.setLocationRelativeTo(posPanel);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(18, 18, 18, 18));
        mainPanel.setBackground(new Color(245, 247, 250));

        // Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(245, 247, 250));

        JLabel titleLbl = new JLabel("Receipt Preview", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLbl.setForeground(new Color(30, 41, 59));
        topPanel.add(titleLbl, BorderLayout.CENTER);

        JLabel subLbl = new JLabel("Preview, print, or export the latest receipt", SwingConstants.CENTER);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLbl.setForeground(new Color(100, 116, 139));

        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setBackground(new Color(245, 247, 250));
        headerWrap.add(titleLbl, BorderLayout.NORTH);
        headerWrap.add(subLbl, BorderLayout.SOUTH);

        mainPanel.add(headerWrap, BorderLayout.NORTH);

        // Receipt card panel
        JPanel receiptCard = new JPanel(new BorderLayout());
        receiptCard.setBackground(Color.WHITE);
        receiptCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 226, 232), 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JTextArea receiptArea = new JTextArea(receiptText);
        receiptArea.setEditable(false);
        receiptArea.setFocusable(false);
        receiptArea.setLineWrap(false);
        receiptArea.setWrapStyleWord(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 18));
        receiptArea.setBackground(Color.WHITE);
        receiptArea.setForeground(Color.BLACK);
        receiptArea.setMargin(new Insets(10, 10, 10, 10));
        receiptArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(receiptArea);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);

        receiptCard.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(receiptCard, BorderLayout.CENTER);

        // Bottom buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(new Color(245, 247, 250));

        JButton previewPrintBtn = new JButton("Print");
        JButton exportPdfBtn = new JButton("Export PDF");
        JButton cancelPreviewBtn = new JButton("Close");

        styleReceiptButton(previewPrintBtn, new Color(59, 130, 246), Color.WHITE);
        styleReceiptButton(exportPdfBtn, new Color(51, 65, 85), Color.WHITE);
        styleReceiptButton(cancelPreviewBtn, new Color(239, 68, 68), Color.WHITE);

        buttonPanel.add(previewPrintBtn);
        buttonPanel.add(exportPdfBtn);
        buttonPanel.add(cancelPreviewBtn);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);

        previewPrintBtn.addActionListener(e -> {
            try {
                boolean printed = receiptArea.print();
                if (!printed) {
                    JOptionPane.showMessageDialog(dialog, "Print cancelled.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Print failed: " + ex.getMessage());
            }
        });

        exportPdfBtn.addActionListener(e -> exportReceiptToPdf(receiptText));
        cancelPreviewBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }
    
    private void styleReceiptButton(JButton button, Color bg, Color fg) {
        button.setPreferredSize(new Dimension(130, 42));
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void exportReceiptToPdf(String receiptText) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Receipt as PDF");
        chooser.setSelectedFile(new File("receipt-" + System.currentTimeMillis() + ".pdf"));

        int result = chooser.showSaveDialog(posPanel);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getAbsolutePath() + ".pdf");
        }

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            com.itextpdf.text.Font pdfFont = FontFactory.getFont(FontFactory.COURIER, 11);
            Paragraph paragraph = new Paragraph(receiptText, pdfFont);
            document.add(paragraph);

            document.close();

            JOptionPane.showMessageDialog(posPanel, "Receipt exported successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(posPanel, "PDF export failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private ProductData getProductByBarcode(String barcode) {
        String sql = "SELECT product_id, barcode, name, selling_price, stock_quantity "
                + "FROM products WHERE barcode = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, barcode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ProductData p = new ProductData();
                    p.productId = rs.getInt("product_id");
                    p.barcode = rs.getString("barcode");
                    p.name = rs.getString("name");
                    p.sellingPrice = rs.getBigDecimal("selling_price");
                    p.stockQuantity = rs.getInt("stock_quantity");
                    return p;
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(posPanel, "Error loading product: " + ex.getMessage());
            ex.printStackTrace();
        }

        return null;
    }

    private int getUserIdByUsername(String username, Connection conn) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        }
        return -1;
    }

    private boolean ensureSaleItemsCostSnapshotColumn(Connection conn) {
        try {
            if (hasSaleItemsCostSnapshotColumn(conn)) {
                return true;
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE sale_items ADD COLUMN cost_price_snapshot DECIMAL(12,2) NULL");
            } catch (SQLException ignored) {
                // Ignore: table may already be updated by another client or schema permissions may be limited.
            }

            return hasSaleItemsCostSnapshotColumn(conn);
        } catch (SQLException ignored) {
            return false;
        }
    }

    private boolean hasSaleItemsCostSnapshotColumn(Connection conn) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        String catalog = conn.getCatalog();

        try (ResultSet rs = metaData.getColumns(catalog, null, "sale_items", "cost_price_snapshot")) {
            if (rs.next()) {
                return true;
            }
        }

        try (ResultSet rs = metaData.getColumns(catalog, null, "SALE_ITEMS", "COST_PRICE_SNAPSHOT")) {
            return rs.next();
        }
    }

    private BigDecimal getCurrentProductCost(int productId, Connection conn) {
        String sql = "SELECT COALESCE(cost_price, 0) AS cost_price FROM products WHERE product_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal cost = rs.getBigDecimal("cost_price");
                    return cost == null ? BigDecimal.ZERO : cost;
                }
            }
        } catch (SQLException ignored) {
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal parseMoney(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return BigDecimal.ZERO;

            String cleaned = value.replace("₱", "")
                    .replace(",", "")
                    .trim();

            if (cleaned.isEmpty() || cleaned.equals(".")) return BigDecimal.ZERO;

            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) value = BigDecimal.ZERO;
        return moneyFormat.format(value.setScale(2, RoundingMode.HALF_UP));
    }

    private String formatPeso(BigDecimal value) {
        return "₱" + formatMoney(value);
    }

    private void closeQuietly(AutoCloseable c) {
        try {
            if (c != null) c.close();
        } catch (Exception ignored) {
        }
    }

    private class MoneyRenderer extends DefaultTableCellRenderer {
        public MoneyRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        protected void setValue(Object value) {
            BigDecimal amount = parseMoney(value == null ? "0.00" : value.toString());
            setText(formatPeso(amount));
        }
    }

    private static class ProductData {
        int productId;
        String barcode;
        String name;
        BigDecimal sellingPrice;
        int stockQuantity;
    }
}