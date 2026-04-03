/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package pointofsale.Screens;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Image;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import org.netbeans.lib.awtextra.AbsoluteLayout;
import pointofsale.Category;
import pointofsale.Products;
import pointofsale.Supplier;
import pointofsale.Users;
import pointofsale.InventoryMovement;
import pointofsale.PosController;
import pointofsale.SalesHistory;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.table.DefaultTableModel;
import pointofsale.Reports;
import pointofsale.Dashboard;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.Timer;

public class MainPanel extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainPanel.class.getName());

    
    //hover
    private java.awt.Color defaultColor = new java.awt.Color(44, 62, 80);
    private java.awt.Color hoverColor = new java.awt.Color(52, 152, 219);
    private java.awt.Color selectedColor = new java.awt.Color(41, 128, 185);

    private javax.swing.JPanel selectedMenu = null;
    
    
    
    //categoryVariable
    boolean categoryEditMode = false;
    
    //suppliers
    boolean supplierEditMode = false;

    String oldSupplierName = "";
    String oldContactPerson = "";
    String oldContactNumber = "";
    String oldEmail = "";
    String oldAddress = "";
    
     private boolean isValidEmail(String email) {
        // Enhanced email validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex) && !email.contains(" ") && email.length() > 5;
    }

    private boolean isNumbersOnly(String text) {
        return text.matches("\\d+");
    }
    
    private boolean isValidPhilippineContactNumber(String contactNumber) {
        // Philippine contact number validation: exactly 11 digits
        return contactNumber.matches("^\\d{11}$") && contactNumber.startsWith("09");
    }
    
    
    //products
    boolean productEditMode = false;
    String selectedProductImagePath = "";
    int selectedProductId = -1;
    
    private boolean isIntegerOnly(String text) {
        return text.matches("\\d+");
    }

    private boolean isDecimalNumber(String text) {
        return text.matches("\\d+(\\.\\d+)?");
    }

   String oldBarcode = "";
   
   //inventory
    String inventoryMode = "STOCK IN";
    int currentUserId = 1; // replace later with logged in user id
    
    
    private String loggedInUsername;
    private PosController posController;
    
    //salesHistory
    private SalesHistory salesHistory = new SalesHistory();
    
    //reports
    private Reports reports = new Reports();
    
    //users
    private boolean isEditMode = false;
    private int selectedUserId = -1;
    
    private String loggedInWelcomeName;
    private String loggedInFullName;
    private String loggedInRole;

    private String salesHistoryQuickPeriod = "TODAY";
    private String salesHistorySummaryFocus = "SALES";
    private javax.swing.JButton salesTodayFilterBtn;
    private javax.swing.JButton salesWeekFilterBtn;
    private javax.swing.JButton salesMonthFilterBtn;
    private javax.swing.JButton salesYearFilterBtn;
    private String dashboardSalesFilter = "TODAY";

    private javax.swing.JButton salesTodayBtn;
    private javax.swing.JButton salesWeekBtn;
    private javax.swing.JButton salesMonthBtn;
    private javax.swing.JButton salesYearBtn;
    
    //rbac
    private java.util.Set<javax.swing.JPanel> restrictedPanels = new java.util.HashSet<>();
    private java.util.Set<String> allowedModules = new java.util.HashSet<>();
    
    public MainPanel() {
        initComponents();
        showCard("dashboard");

        // category
        Category cat = new Category();
        cat.loadCategories(Category_tbl);

        // suppliers
        Supplier sup = new Supplier();
        sup.loadSuppliers(supplier_tbl);

        // users
        Users user = new Users();
        user.loadUsers(user_tbl);

        setupSidebarMenu();
        initializeDashboardSalesFilterButtons();

        selectedMenu = DashboardPanel1;
        DashboardPanel1.setBackground(selectedColor);

        java.awt.CardLayout cl = (java.awt.CardLayout) contentPanel.getLayout();
        cl.show(contentPanel, "dashboard");

        // widthOfUserTable
        user_tbl.getColumnModel().getColumn(0).setMinWidth(40);
        user_tbl.getColumnModel().getColumn(0).setMaxWidth(60);

        user_tbl.getColumnModel().getColumn(1).setPreferredWidth(200);
        user_tbl.getColumnModel().getColumn(2).setPreferredWidth(120);
        user_tbl.getColumnModel().getColumn(3).setPreferredWidth(80);
        user_tbl.getColumnModel().getColumn(4).setPreferredWidth(150);
        user_tbl.getColumnModel().getColumn(5).setPreferredWidth(150);

        // products
        Products prod = new Products();
        prod.loadProducts(product_tbl);
        prod.loadCategoriesToComboBox(category_cmb);
        prod.loadSuppliersToComboBox(supplier_cmb);
        prod.loadCategoryFilterCombo(categoryProduct_cmb);
        prod.loadSupplierFilterCombo(supplierProduct_cmb);

        statusProduct_cmb.removeAllItems();
        statusProduct_cmb.addItem("ALL");
        statusProduct_cmb.addItem("IN STOCK");
        statusProduct_cmb.addItem("LOW STOCK");
        statusProduct_cmb.addItem("OUT OF STOCK");

        formatProductTable();
        setProductTableStatusColor();

        // Inventory
        InventoryMovement inv = new InventoryMovement();
        inv.loadProductsToComboBox(productStockIN_cmb);
        inv.loadProductsToComboBox(ProductOut_cmb);

        inv.loadStockInTable(StockIn_tbl, "", "ALL");
        inv.loadStockOutTable(StockOUT_tbl, "");
        reloadStockMovementTable();

        stocksType_cmb.addActionListener(e -> reloadStockMovementTable());

        typeIn_cmb.removeAllItems();
        typeIn_cmb.addItem("ALL");
        typeIn_cmb.addItem("IN STOCK");
        typeIn_cmb.addItem("LOW STOCK");
        typeIn_cmb.addItem("OUT OF STOCK");

        reason_cmb.removeAllItems();
        reason_cmb.addItem("Damage");
        reason_cmb.addItem("Expired");
        reason_cmb.addItem("Adjustment");

        saveInventory_btn.setText("Save Stock In");

        jDateChooser1.setDate(new java.util.Date());
        jDateChooser2.setDate(new java.util.Date());

        setStockMovementStatusColor();
        setStatusFieldColor(statusIdentifier_txt);
        setStatusFieldColor(statusIdentifierOUT_txt);
        
        
        //salesHistory
        salesHistory.loadCashierCombo(cashierSalesHistory_cmb);
        salesHistory.loadPaymentMethodCombo(paymentMethodSAlesHistory_cmb);
        initializeSalesHistoryInteractiveUI();
        applySalesQuickPeriod("TODAY");
        
        //reports
        reports.loadReportTypes(ReportType_txt);
        reports.loadCategoryCombo(categoryReport_cmb);
        reports.loadSupplierCombo(supplierReport_cmb);
        reports.loadPaymentCombo(payment_cmb);
        reports.setupReportTable(report_tbl);
        
        reports.loadReportTypes(ReportType_txt);
        reports.loadCategoryCombo(categoryReport_cmb);
        reports.loadSupplierCombo(supplierReport_cmb);
        reports.loadPaymentCombo(payment_cmb);

        reports.formatReportTable(report_tbl);
        
        
        //dashboard
        Dashboard dashboard = new Dashboard();

        dashboard.loadDashboardStats(
                totalProduct_lbl,
                totalSupplier_lbl,
                totalUser_lbl,
                lowstockDashboad_lbl,
                totalSalesDashboard,
                outOfStockdashboard_lbl,
                totalCategory_lbl,
            totalTransaction_lbl,
            dashboardSalesFilter
        );

        dashboard.loadTopSellingProducts(dashboard_tbl);
        dashboard.loadSalesPieChart(pieChart_panel);
            refreshDashboard();
            refreshInventoryModule();
            
            
            
        
        setupDefaultView();
        
        startDateTimeClock();
        
        
         refreshProductCombos();
         setupDashboardTableSelection();
         cashierSalesHistory_cmb.addActionListener(e -> applySalesHistoryFilters());
         paymentMethodSAlesHistory_cmb.addActionListener(e -> applySalesHistoryFilters());
         installSalesHistoryRealtimeListeners();
        
    }

    public MainPanel(String username, String welcomeName, String fullName, String role) {
        this();

        this.loggedInUsername = username;
        this.loggedInWelcomeName = welcomeName;
        this.loggedInFullName = fullName;
        this.loggedInRole = role;

        loadLoggedInUserDetails();
        applyRoleBasedAccess();

        Runnable afterCheckoutRefresh = () -> {
    // put your refresh methods here
    // example:
    // dashboardPanel.refreshDashboard();
    // salesHistoryPanel.loadSalesHistory();
    // reportsPanel.loadReports();
};
        
        posController = new PosController(
                posPanel,
                headerPanel,
                barcodePanel,
                cartPanel,
                paymentPanel,
                buttonPanel,
                imagePanel,
                loggedInUsername,
            loggedInFullName,
                afterCheckoutRefresh
        );
    }
    
    
    //dashboard
    private void setupDashboardTableSelection() {
        dashboard_tbl.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            if (dashboard_tbl.getSelectedRow() == -1) {
                refreshDashboard();
            }
        });
    }

    private void initializeDashboardSalesFilterButtons() {
        if (DashboardPanelHeader == null) {
            return;
        }

        salesTodayBtn = new javax.swing.JButton("Today");
        salesWeekBtn = new javax.swing.JButton("Week");
        salesMonthBtn = new javax.swing.JButton("Month");
        salesYearBtn = new javax.swing.JButton("Year");

        styleDashboardSalesFilterButton(salesTodayBtn, "TODAY");
        styleDashboardSalesFilterButton(salesWeekBtn, "WEEK");
        styleDashboardSalesFilterButton(salesMonthBtn, "MONTH");
        styleDashboardSalesFilterButton(salesYearBtn, "YEAR");

        DashboardPanelHeader.add(salesTodayBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(760, 5, 70, 24));
        DashboardPanelHeader.add(salesWeekBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(835, 5, 70, 24));
        DashboardPanelHeader.add(salesMonthBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(910, 5, 70, 24));
        DashboardPanelHeader.add(salesYearBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(985, 5, 70, 24));

        updateDashboardSalesFilterButtons();
        DashboardPanelHeader.revalidate();
        DashboardPanelHeader.repaint();
    }

    private void styleDashboardSalesFilterButton(javax.swing.JButton button, String period) {
        button.setFocusable(false);
        button.setForeground(Color.WHITE);
        button.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 11));
        button.setBorder(javax.swing.BorderFactory.createLineBorder(new Color(173, 189, 204)));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(e -> applyDashboardSalesFilter(period));
    }

    private void applyDashboardSalesFilter(String period) {
        dashboardSalesFilter = period;
        updateDashboardSalesFilterButtons();
        refreshDashboard();
    }

    private void updateDashboardSalesFilterButtons() {
        setDashboardFilterButtonState(salesTodayBtn, "TODAY");
        setDashboardFilterButtonState(salesWeekBtn, "WEEK");
        setDashboardFilterButtonState(salesMonthBtn, "MONTH");
        setDashboardFilterButtonState(salesYearBtn, "YEAR");
    }

    private void setDashboardFilterButtonState(javax.swing.JButton button, String period) {
        if (button == null) {
            return;
        }

        boolean active = dashboardSalesFilter.equalsIgnoreCase(period);
        button.setBackground(active ? new Color(52, 152, 219) : new Color(75, 96, 117));
    }
    
    
    private void startDateTimeClock() {
        Timer timer = new Timer(1000, e -> {
            LocalDateTime now = LocalDateTime.now();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy | hh:mm:ss a");
            dateTime_lbl.setText(now.format(formatter));
        });

        timer.start();
    }
    
    //user credential
   

    private void loadLoggedInUserDetails() {

        userNameWelcome_lbl.setText("Welcome, " + loggedInWelcomeName);

        fullnameofUser_lbl.setText(loggedInFullName);

        roleUser_lbl.setText(loggedInRole);

        onlineOffline_lbl.setText("● Online");
        onlineOffline_lbl.setForeground(new java.awt.Color(0, 180, 0));
        onlineOffline_lbl.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
    }

    
    
    //showCard
    // show content card only
private void showCard(String name) {
        java.awt.CardLayout cl = (java.awt.CardLayout) contentPanel.getLayout();
        cl.show(contentPanel, name);
    }

// show header card only
    private void showHeaderCard(String name) {
        java.awt.CardLayout cl = (java.awt.CardLayout) HeaderTitle.getLayout();
        cl.show(HeaderTitle, name);
    }

// show both content + header
    private void showModule(String contentCardName, String headerCardName) {
        java.awt.CardLayout contentCL = (java.awt.CardLayout) contentPanel.getLayout();
        contentCL.show(contentPanel, contentCardName);

        java.awt.CardLayout headerCL = (java.awt.CardLayout) HeaderTitle.getLayout();
        headerCL.show(HeaderTitle, headerCardName);
    }

// hover
    private void menuHover(javax.swing.JPanel panel) {
        if (isRestricted(panel)) {
            panel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            panel.setBackground(defaultColor.darker());
            return;
        }

        panel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        if (panel != selectedMenu) {
            panel.setBackground(hoverColor);
        }
    }

    private void menuExit(javax.swing.JPanel panel) {
        if (isRestricted(panel)) {
            panel.setBackground(defaultColor.darker());
            return;
        }

        if (panel != selectedMenu) {
            panel.setBackground(defaultColor);
        } else {
            panel.setBackground(selectedColor);
        }
    }

// click menu
       private void menuClick(javax.swing.JPanel panel, String contentCardName, String headerCardName) {
        if (!hasAccess(contentCardName)) {
            JOptionPane.showMessageDialog(
                    this,
                    "You are not allowed to access this module.",
                    "Access Denied",
                    JOptionPane.WARNING_MESSAGE
            );

            // VERY IMPORTANT: force return to allowed module
            forceStayOnAllowedModule();
            return;
        }

        resetSidebarColors();
        selectedMenu = panel;
        panel.setBackground(selectedColor);
        showModule(contentCardName, headerCardName);
    }
    
    
    private void setupSidebarMenu() {

        DashboardPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                menuHover(DashboardPanel1);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                menuExit(DashboardPanel1);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menuClick(DashboardPanel1, "dashboard", "dashboardCard");
            }
        });

        PosPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                menuHover(PosPanel);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                menuExit(PosPanel);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menuClick(PosPanel, "pos", "posCard");
            }
        });

        SalesHistoryPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                menuHover(SalesHistoryPanel);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                menuExit(SalesHistoryPanel);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menuClick(SalesHistoryPanel, "salesHistory", "salesCard");
            }
        });

        InventoryPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                menuHover(InventoryPanel);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                menuExit(InventoryPanel);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menuClick(InventoryPanel, "inventory", "inventoryCard");
            }
        });

        ProductsPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                menuHover(ProductsPanel);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                menuExit(ProductsPanel);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menuClick(ProductsPanel, "products", "productCard");
            }
        });

        CategoriesPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                menuHover(CategoriesPanel);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                menuExit(CategoriesPanel);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menuClick(CategoriesPanel, "categories", "categoryCard");
            }
        });

        SuppliersPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                menuHover(SuppliersPanel);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                menuExit(SuppliersPanel);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menuClick(SuppliersPanel, "suppliers", "supplierCard");
            }
        });

        ReportsPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                menuHover(ReportsPanel);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                menuExit(ReportsPanel);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menuClick(ReportsPanel, "reports", "reportCard");
            }
        });

        UsersPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                menuHover(UsersPanel);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                menuExit(UsersPanel);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menuClick(UsersPanel, "users", "userCard");
            }
        });
    }
    
    
    
         private void setupDefaultView() {
        selectedMenu = DashboardPanel1;
        DashboardPanel1.setBackground(selectedColor);

        showModule("dashboard", "dashboardCard");
    }
         
         
         
         
         //rbac
      private void applyRoleBasedAccess() {
        allowedModules.clear();
        clearRestrictedPanels();

        if (loggedInRole == null || loggedInRole.trim().isEmpty()) {
            allowedModules.add("dashboard");
        } else {
            String role = loggedInRole.trim();

            switch (role) {
                case "Super Admin":
                case "Admin":
                    allowedModules.add("dashboard");
                    allowedModules.add("pos");
                    allowedModules.add("salesHistory");
                    allowedModules.add("inventory");
                    allowedModules.add("products");
                    allowedModules.add("categories");
                    allowedModules.add("suppliers");
                    allowedModules.add("reports");
                    allowedModules.add("users");
                    break;

                case "Manager":
                    allowedModules.add("dashboard");
                    allowedModules.add("pos");
                    allowedModules.add("salesHistory");
                    allowedModules.add("inventory");
                    allowedModules.add("products");
                    allowedModules.add("categories");
                    allowedModules.add("suppliers");
                    allowedModules.add("reports");
                    break;

                case "Cashier":
                    allowedModules.add("dashboard");
                    allowedModules.add("pos");
                    allowedModules.add("salesHistory");
                    break;

                case "Inventory Clerk":
                    allowedModules.add("dashboard");
                    allowedModules.add("inventory");
                    allowedModules.add("products");
                    allowedModules.add("categories");
                    allowedModules.add("suppliers");
                    break;

                default:
                    allowedModules.add("dashboard");
                    break;
            }
        }

        styleSidebarByAccess();
        forceStayOnAllowedModule();
    }

    private void openDefaultModuleByRole() {
        resetSidebarColors();

        if ("Cashier".equalsIgnoreCase(loggedInRole)) {
            selectedMenu = PosPanel;
            PosPanel.setBackground(selectedColor);
            showModule("pos", "posCard");
        } else if ("Inventory Clerk".equalsIgnoreCase(loggedInRole)) {
            selectedMenu = InventoryPanel;
            InventoryPanel.setBackground(selectedColor);
            showModule("inventory", "inventoryCard");
        } else {
            selectedMenu = DashboardPanel1;
            DashboardPanel1.setBackground(selectedColor);
            showModule("dashboard", "dashboardCard");
        }
    }

     private void resetSidebarColors() { //error
        resetOnePanel(DashboardPanel1);
        resetOnePanel(PosPanel);
        resetOnePanel(SalesHistoryPanel);
        resetOnePanel(InventoryPanel);
        resetOnePanel(ProductsPanel);
        resetOnePanel(CategoriesPanel);
        resetOnePanel(SuppliersPanel);
        resetOnePanel(ReportsPanel);
        resetOnePanel(UsersPanel);
    }

    private void resetOnePanel(javax.swing.JPanel panel) {
        if (isRestricted(panel)) {
            panel.setBackground(defaultColor.darker());
        } else {
            panel.setBackground(defaultColor);
        }
    }

    private void resetSinglePanel(javax.swing.JPanel panel) {
        if (isRestricted(panel)) {
            panel.setBackground(defaultColor.darker());
        } else {
            panel.setBackground(defaultColor);
        }
    }

    private void setPanelRestricted(javax.swing.JPanel panel, boolean restricted) {
        if (restricted) {
            restrictedPanels.add(panel);
            panel.setBackground(defaultColor.darker());
            panel.setToolTipText("Access denied");
            panel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else {
            restrictedPanels.remove(panel);
            if (panel != selectedMenu) {
                panel.setBackground(defaultColor);
            }
            panel.setToolTipText(null);
            panel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }
    }

    private boolean isRestricted(javax.swing.JPanel panel) {
        return restrictedPanels.contains(panel);
    }

    private void clearRestrictedPanels() {
        restrictedPanels.clear();
    }

    private boolean canAccessModule(String moduleName) {
        return allowedModules.contains(moduleName);
    }

    private boolean hasAccess(String moduleName) {
        return canAccessModule(moduleName);
    }

    private boolean hasModuleAccess(String moduleName) {
        return moduleName != null && hasAccess(moduleName);
    }

    private boolean ensureAccessOrWarn(String moduleName) {
        if (!hasModuleAccess(moduleName)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Access Denied",
                    "Access Denied",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        return true;
    }

    private void openDashboardModule(String moduleName, String contentCardName, String headerCardName) {
        if (!ensureAccessOrWarn(moduleName)) {
            return;
        }

        showModule(contentCardName, headerCardName);
    }

    private void openInventoryStockMonitoring() {
        if (!ensureAccessOrWarn("inventory")) {
            return;
        }

        showModule("inventory", "inventoryCard");

        CardLayout contentCards = (CardLayout) InventoryContentPanel.getLayout();
        contentCards.show(InventoryContentPanel, "StockMovement");

        CardLayout tableCards = (CardLayout) jtablePanel.getLayout();
        tableCards.show(jtablePanel, "StockMovementJ");

        inventoryMode = "STOCK MOVEMENT";
        saveInventory_btn.setText("Save Inventory");
    }

    private void styleSidebarByAccess() {
        updateSidebarVisibility();
    }

    private void updateSidebarVisibility() {
        javax.swing.JPanel[] panels = new javax.swing.JPanel[]{
            UsersPanel,
            CategoriesPanel,
            SuppliersPanel,
            ProductsPanel,
            InventoryPanel,
            PosPanel,
            SalesHistoryPanel,
            ReportsPanel,
            DashboardPanel1
        };

        String[] moduleNames = new String[]{
            "users",
            "categories",
            "suppliers",
            "products",
            "inventory",
            "pos",
            "salesHistory",
            "reports",
            "dashboard"
        };

        for (int i = 0; i < panels.length; i++) {
            boolean visible = hasAccess(moduleNames[i]);
            panels[i].setVisible(visible);
        }

        repositionVisibleSidebarPanels(panels);
    }

    private void repositionVisibleSidebarPanels(javax.swing.JPanel[] panels) {
        // Derive base metrics from the first panel to preserve sizing; fallback to defaults if zero.
        int baseX = (panels.length > 0) ? panels[0].getX() : 0;
        int baseY = (panels.length > 0) ? panels[0].getY() : 60;
        int baseWidth = (panels.length > 0 && panels[0].getWidth() > 0) ? panels[0].getWidth() : 190;
        int baseHeight = (panels.length > 0 && panels[0].getHeight() > 0) ? panels[0].getHeight() : 40;

        int gap = 10;
        int currentY = baseY;

        for (javax.swing.JPanel panel : panels) {
            if (panel.isVisible()) {
                panel.setBounds(baseX, currentY, baseWidth, baseHeight);
                currentY += baseHeight + gap;
            }
        }

        SideBarPanel.revalidate();
        SideBarPanel.repaint();
    }
    
    
    
       private void forceStayOnAllowedModule() {
        if ("Cashier".equalsIgnoreCase(loggedInRole)) {
            selectedMenu = PosPanel;
            resetSidebarColors();
            PosPanel.setBackground(selectedColor);
            showModule("pos", "posCard");
        } else if ("Inventory Clerk".equalsIgnoreCase(loggedInRole)) {
            selectedMenu = InventoryPanel;
            resetSidebarColors();
            InventoryPanel.setBackground(selectedColor);
            showModule("inventory", "inventoryCard");
        } else {
            selectedMenu = DashboardPanel1;
            resetSidebarColors();
            DashboardPanel1.setBackground(selectedColor);
            showModule("dashboard", "dashboardCard");
        }
    }

    private void clearFields() {
        user_tbl.clearSelection();

        userID_txt.setText("[Auto]");
        firstname_txt1.setText("");
        middlename_txt.setText("");
        lastname_txt.setText("");
        username_txt.setText("");
        password_txt.setText("");
        confirmPass_txt.setText("");
        confirmPass_txt.setForeground(java.awt.Color.BLACK);

        role_cmb.setEnabled(true);
        role_cmb.removeAllItems();
        role_cmb.addItem("Super Admin");
        role_cmb.addItem("Admin");
        role_cmb.addItem("Cashier");
        role_cmb.addItem("Inventory Clerk");
        role_cmb.addItem("Manager");
        role_cmb.setSelectedIndex(0);
        status_cmb.setSelectedIndex(0);

        createdAt_txt1.setText("[Auto]");
        updatedat_txt1.setText("[Auto]");

        isEditMode = false;
        selectedUserId = -1;
        addUser_btn.setEnabled(true);
        update_btn.setText("UPDATE");

        password_txt.setEchoChar('\u2022');
        confirmPass_txt.setEchoChar('\u2022');
    }

    private void checkBarcodeDuplicate() {
        String barcode = barcode_txt.getText().trim();

        if (barcode.isEmpty()) {
            barcode_txt.setBackground(Color.white);
            return;
        }

        Products prod = new Products();

        if (prod.isBarcodeExists(barcode)) {
            barcode_txt.setBackground(new Color(255, 102, 102));
        } else {
            barcode_txt.setBackground(Color.white);
        }
    }
      
     
     
     private void clearProductFields() {
        barcode_txt.setText("");
        productName_txt.setText("");
        costPrice_txt.setText("");
        sellingPrice_txt.setText("");
        stockquantity_txt.setText("");
        reorderLevel_txt.setText("");
        searchProduct_txt.setText("");

        category_cmb.setSelectedIndex(0);
        supplier_cmb.setSelectedIndex(0);
        categoryProduct_cmb.setSelectedIndex(0);
        supplierProduct_cmb.setSelectedIndex(0);
        statusProduct_cmb.setSelectedIndex(0);

        autoGenerateBarcode_rbt.setSelected(false);
        barcode_txt.setEditable(true);
        barcode_txt.setBackground(Color.white);

        Image_txt.setIcon(null);
        Image_txt.setText("");
        selectedProductImagePath = "";

        // Reset view mode and enable all buttons/fields
        setProductViewMode(false);
        
        productEditMode = false;
        selectedProductId = -1;
    }

    private void setProductViewMode(boolean isViewMode) {
        if (isViewMode) {
            // Disable all buttons except clear
            addProduct_btn.setEnabled(false);
            updateProduct_btn.setEnabled(false);
            deleteProduct_btn.setEnabled(false);
            chooseImage_btn.setEnabled(false);
            removeImage_btn.setEnabled(false);
            
            // Make all fields read-only
            barcode_txt.setEditable(false);
            productName_txt.setEditable(false);
            costPrice_txt.setEditable(false);
            sellingPrice_txt.setEditable(false);
            stockquantity_txt.setEditable(false);
            reorderLevel_txt.setEditable(false);
            category_cmb.setEnabled(false);
            supplier_cmb.setEnabled(false);
            autoGenerateBarcode_rbt.setEnabled(false);
            
            // Set visual indication for view mode
            barcode_txt.setBackground(new Color(240, 240, 240));
            productName_txt.setBackground(new Color(240, 240, 240));
            costPrice_txt.setBackground(new Color(240, 240, 240));
            sellingPrice_txt.setBackground(new Color(240, 240, 240));
            stockquantity_txt.setBackground(new Color(240, 240, 240));
            reorderLevel_txt.setBackground(new Color(240, 240, 240));
        } else {
            // Enable all buttons
            addProduct_btn.setEnabled(true);
            updateProduct_btn.setEnabled(true);
            deleteProduct_btn.setEnabled(true);
            chooseImage_btn.setEnabled(true);
            removeImage_btn.setEnabled(true);
            
            // Make all fields editable
            barcode_txt.setEditable(true);
            productName_txt.setEditable(true);
            costPrice_txt.setEditable(true);
            sellingPrice_txt.setEditable(true);
            stockquantity_txt.setEditable(true);
            reorderLevel_txt.setEditable(true);
            category_cmb.setEnabled(true);
            supplier_cmb.setEnabled(true);
            autoGenerateBarcode_rbt.setEnabled(true);
            
            // Reset background colors and tooltips
            barcode_txt.setBackground(Color.white);
            barcode_txt.setToolTipText("");
            productName_txt.setBackground(Color.white);
            productName_txt.setToolTipText("");
            costPrice_txt.setBackground(Color.white);
            costPrice_txt.setToolTipText("");
            sellingPrice_txt.setBackground(Color.white);
            sellingPrice_txt.setToolTipText("");
            stockquantity_txt.setBackground(Color.white);
            stockquantity_txt.setToolTipText("");
            reorderLevel_txt.setBackground(Color.white);
            reorderLevel_txt.setToolTipText("");
        }
    }
     
     
     private void filterProductsNow() {

        String keyword = searchProduct_txt.getText().trim();

        String status = statusProduct_cmb.getSelectedItem() == null
                ? "ALL"
                : statusProduct_cmb.getSelectedItem().toString();

        String supplier = supplierProduct_cmb.getSelectedItem() == null
                ? "ALL"
                : supplierProduct_cmb.getSelectedItem().toString();

        String category = categoryProduct_cmb.getSelectedItem() == null
                ? "ALL"
                : categoryProduct_cmb.getSelectedItem().toString();

        Products prod = new Products();
        prod.filterProducts(product_tbl, keyword, status, supplier, category);
    }
     
     
     private void setProductTableStatusColor() {
        product_tbl.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                java.awt.Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                if (value != null) {
                    String status = value.toString();

                    if (status.equals("IN STOCK")) {
                        c.setForeground(new Color(0, 128, 0));
                    } else if (status.equals("LOW STOCK")) {
                        c.setForeground(new Color(255, 140, 0));
                    } else if (status.equals("OUT OF STOCK")) {
                        c.setForeground(new Color(204, 0, 0));
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                } else {
                    c.setForeground(Color.BLACK);
                }

                return c;
            }
        });
    }
     
     
     
     private void formatProductTable() {
        product_tbl.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);

        product_tbl.getColumnModel().getColumn(0).setPreferredWidth(60);
        product_tbl.getColumnModel().getColumn(1).setPreferredWidth(120);
        product_tbl.getColumnModel().getColumn(2).setPreferredWidth(150);
        product_tbl.getColumnModel().getColumn(3).setPreferredWidth(120);
        product_tbl.getColumnModel().getColumn(4).setPreferredWidth(120);
        product_tbl.getColumnModel().getColumn(5).setPreferredWidth(90);
        product_tbl.getColumnModel().getColumn(6).setPreferredWidth(90);
        product_tbl.getColumnModel().getColumn(7).setPreferredWidth(100);
        product_tbl.getColumnModel().getColumn(8).setPreferredWidth(100);
        product_tbl.getColumnModel().getColumn(9).setPreferredWidth(100);
        product_tbl.getColumnModel().getColumn(10).setPreferredWidth(100);
        product_tbl.getColumnModel().getColumn(11).setPreferredWidth(140);
        product_tbl.getColumnModel().getColumn(12).setPreferredWidth(140);
    }
    
     
     //inventory
     private void clearInventoryFields() {

        productStockIN_cmb.setSelectedIndex(0);
        ProductOut_cmb.setSelectedIndex(0);

        barcodeIN_txt.setText("");
        barcodeOUT_txt.setText("");

        supplierIN_btn.setText("");

        quantityIn_txt.setText("");
        quantityOUT_txt.setText("");

        ReferenceNo_txt.setText("");
        ReferenceNoOUT_txt.setText("");

        remarksIn_txt.setText("");
        remarksOUT_txt.setText("");

        barcodeinGen_lbl.setText("");
        barcodeOUTGen_lbl.setText("");

        currentStock_lbl.setText("");
        currentStockOUT_lbl.setText("");

        statusIdentifier_txt.setText("");
        statusIdentifierOUT_txt.setText("");

        statusIdentifier_txt.setBackground(Color.WHITE);
        statusIdentifierOUT_txt.setBackground(Color.WHITE);
        
        

        ImageIn_lbl.setIcon(null);
        ImageIn_lbl.setText("");

        ImageOUT_lbl.setIcon(null);
        ImageOUT_lbl.setText("");

        searchIN_txt.setText("");
        searchOUT_txt.setText("");

        if (typeIn_cmb.getItemCount() > 0) {
            typeIn_cmb.setSelectedIndex(0);
        }

        if (reason_cmb.getItemCount() > 0) {
            reason_cmb.setSelectedIndex(0);
        }

        jDateChooser1.setDate(new java.util.Date());
        jDateChooser2.setDate(new java.util.Date());
        
        
         ProductMovement_lbl.setText("");
         categoryMovement_lbl.setText("");
         supplierMovement_lbl.setText("");
         statusMovement_lbl.setText("");
         reorderLevel_lbl.setText("");
         StockQTYMovement_lbl.setText("");

         ImageMovementDisplay_lbl.setIcon(null);
    }
     
     
     private void applyStatusColor(javax.swing.JTextField txt, String status) {
        txt.setOpaque(true);

        if (status == null) {
            txt.setBackground(Color.WHITE);
            txt.setForeground(Color.BLACK);
            return;
        }

        switch (status) {
            case "IN STOCK":
                txt.setBackground(new Color(0, 128, 0));
                txt.setForeground(Color.WHITE);
                break;
            case "LOW STOCK":
                txt.setBackground(new Color(255, 140, 0));
                txt.setForeground(Color.WHITE);
                break;
            case "OUT OF STOCK":
                txt.setBackground(new Color(204, 0, 0));
                txt.setForeground(Color.WHITE);
                break;
            default:
                txt.setBackground(Color.WHITE);
                txt.setForeground(Color.BLACK);
                break;
        }
    }

    private void setStatusFieldColor(javax.swing.JTextField txt) {
        txt.setOpaque(true);
    }
    
    private void setStockMovementStatusColor() {
        stockMovement_tbl.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                java.awt.Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                if (value != null) {
                    String status = value.toString();

                    if (status.equals("IN STOCK")) {
                        c.setForeground(new Color(0, 128, 0));
                    } else if (status.equals("LOW STOCK")) {
                        c.setForeground(new Color(255, 140, 0));
                    } else if (status.equals("OUT OF STOCK")) {
                        c.setForeground(new Color(204, 0, 0));
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                } else {
                    c.setForeground(Color.BLACK);
                }

                return c;
            }
        });
    }
    
    
    private void refreshSelectedStockInProduct() {
        if (productStockIN_cmb.getSelectedItem() == null) {
            return;
        }

        String productName = productStockIN_cmb.getSelectedItem().toString();
        if (productName.equals("SELECT PRODUCT")) {
            return;
        }

        InventoryMovement inv = new InventoryMovement();
        InventoryMovement.ProductDetails d = inv.getProductDetailsByName(productName);

        if (d != null) {
            currentStock_lbl.setText(String.valueOf(d.stockQty));
            statusIdentifier_txt.setText(d.status);
            applyStatusColor(statusIdentifier_txt, d.status);
        }
    }

    private void refreshSelectedStockOutProduct() {
        if (ProductOut_cmb.getSelectedItem() == null) {
            return;
        }

        String productName = ProductOut_cmb.getSelectedItem().toString();
        if (productName.equals("SELECT PRODUCT")) {
            return;
        }

        InventoryMovement inv = new InventoryMovement();
        InventoryMovement.ProductDetails d = inv.getProductDetailsByName(productName);

        if (d != null) {
            currentStockOUT_lbl.setText(String.valueOf(d.stockQty));
            statusIdentifierOUT_txt.setText(d.status);
            applyStatusColor(statusIdentifierOUT_txt, d.status);
        }
    }
    
    
    private void applyMovementStatusColor(String status) {

        statusMovement_lbl.setOpaque(true);

        if (status == null) {
            statusMovement_lbl.setBackground(java.awt.Color.WHITE);
            statusMovement_lbl.setForeground(java.awt.Color.BLACK);
            return;
        }

        switch (status) {
            case "IN STOCK":
                statusMovement_lbl.setBackground(new java.awt.Color(0, 128, 0));
                statusMovement_lbl.setForeground(java.awt.Color.WHITE);
                break;

            case "LOW STOCK":
                statusMovement_lbl.setBackground(new java.awt.Color(255, 140, 0));
                statusMovement_lbl.setForeground(java.awt.Color.WHITE);
                break;

            case "OUT OF STOCK":
                statusMovement_lbl.setBackground(new java.awt.Color(204, 0, 0));
                statusMovement_lbl.setForeground(java.awt.Color.WHITE);
                break;

            default:
                statusMovement_lbl.setBackground(java.awt.Color.WHITE);
                statusMovement_lbl.setForeground(java.awt.Color.BLACK);
                break;
        }
    }
    
    
    
    
    private void refreshDashboard() {
        Dashboard dashboard = new Dashboard();

        dashboard.loadDashboardStats(
                totalProduct_lbl,
                totalSupplier_lbl,
                totalUser_lbl,
                lowstockDashboad_lbl,
                totalSalesDashboard,
                outOfStockdashboard_lbl,
                totalCategory_lbl,
            totalTransaction_lbl,
            dashboardSalesFilter
        );

        dashboard.loadTopSellingProducts(dashboard_tbl);
        dashboard.loadSalesPieChart(pieChart_panel);
    }
    
    
    private void refreshInventoryModule() {
        InventoryMovement inv = new InventoryMovement();

        // reload stock monitoring table
        reloadStockMovementTable();

        // reload product combo boxes
        inv.loadProductCombo(productStockIN_cmb);
        inv.loadProductCombo(ProductOut_cmb);
    }

    private String getSelectedStockMovementFilter() {
        String selected = stocksType_cmb.getSelectedItem() == null
                ? "ALL"
                : stocksType_cmb.getSelectedItem().toString().trim();

        switch (selected.toUpperCase()) {
            case "LOW STOCK":
                return "LOW STOCK";
            case "OUT OF STOCK":
                return "OUT OF STOCK";
            case "IN STOCK":
                return "IN STOCK";
            default:
                return "ALL";
        }
    }

    private void reloadStockMovementTable() {
        InventoryMovement inv = new InventoryMovement();
        String keyword = stockMovementSearch_txt.getText().trim();
        String type = getSelectedStockMovementFilter();
        inv.loadStockMovementTable(stockMovement_tbl, keyword, type);
    }
    
    
    private void refreshSalesHistoryModule() {
        applySalesHistoryFilters();
    }


    private void initializeSalesHistoryInteractiveUI() {
        if (salesHistoryPanel == null) {
            return;
        }

        bindSalesSummaryCardInteractions();

        salesTodayFilterBtn = createSalesQuickFilterButton("Today", "TODAY");
        salesWeekFilterBtn = createSalesQuickFilterButton("Week", "WEEK");
        salesMonthFilterBtn = createSalesQuickFilterButton("Month", "MONTH");
        salesYearFilterBtn = createSalesQuickFilterButton("Year", "YEAR");

        layoutSalesQuickFilterButtons();
        updateSalesFilterButtonLabels();
        salesHistoryPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                layoutSalesQuickFilterButtons();
            }
        });

        salesHistoryPanel.revalidate();
        salesHistoryPanel.repaint();
    }

    private javax.swing.JButton createSalesQuickFilterButton(String text, String period) {
        javax.swing.JButton button = new javax.swing.JButton(text);
        button.setFocusable(false);
        button.setForeground(java.awt.Color.WHITE);
        button.setBackground(new java.awt.Color(74, 85, 104));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 11));
        button.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(110, 124, 143), 1));
        button.addActionListener(e -> applySalesQuickPeriod(period));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new java.awt.Color(96, 113, 135));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                updateSalesFilterButtons();
            }
        });
        return button;
    }

    private void bindSalesSummaryCardInteractions() {
        attachSalesSummaryCard(jPanel38, "SALES");
        attachSalesSummaryCard(jPanel40, "TRANSACTIONS");
        attachSalesSummaryCard(jPanel39, "PRODUCTS");
    }

    private void attachSalesSummaryCard(javax.swing.JPanel card, String focus) {
        card.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        card.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255, 70), 1));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                salesHistorySummaryFocus = focus;
                updateSalesFilterButtonLabels();
                updateSalesSummaryCardStyles();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 2));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                updateSalesSummaryCardStyles();
            }
        });
    }

    private void applySalesQuickPeriod(String period) {
        salesHistoryQuickPeriod = period;
        applyDateRangeForPeriod(period);

        applySalesHistoryFilters();
    }

    private void layoutSalesQuickFilterButtons() {
        if (salesTodayFilterBtn == null || salesHistoryPanel == null) {
            return;
        }

        int y = 230;
        int btnW = 130;
        int btnH = 28;
        int gap = 12;
        int totalW = (btnW * 4) + (gap * 3);
        int startX = Math.max(40, (salesHistoryPanel.getWidth() - totalW) / 2);

        placeSalesQuickFilterButton(salesTodayFilterBtn, startX, y, btnW, btnH);
        placeSalesQuickFilterButton(salesWeekFilterBtn, startX + (btnW + gap), y, btnW, btnH);
        placeSalesQuickFilterButton(salesMonthFilterBtn, startX + ((btnW + gap) * 2), y, btnW, btnH);
        placeSalesQuickFilterButton(salesYearFilterBtn, startX + ((btnW + gap) * 3), y, btnW, btnH);

        salesHistoryPanel.revalidate();
        salesHistoryPanel.repaint();
    }

    private void placeSalesQuickFilterButton(javax.swing.JButton button, int x, int y, int w, int h) {
        if (button == null || salesHistoryPanel == null) {
            return;
        }

        if (button.getParent() == salesHistoryPanel) {
            salesHistoryPanel.remove(button);
        }

        salesHistoryPanel.add(button, new org.netbeans.lib.awtextra.AbsoluteConstraints(x, y, w, h));
    }

    private void updateSalesFilterButtonLabels() {
        if (salesTodayFilterBtn == null) {
            return;
        }

        if ("TRANSACTIONS".equals(salesHistorySummaryFocus)) {
            salesTodayFilterBtn.setText("Today Txn");
            salesWeekFilterBtn.setText("Week Txn");
            salesMonthFilterBtn.setText("Month Txn");
            salesYearFilterBtn.setText("Year Txn");
        } else if ("PRODUCTS".equals(salesHistorySummaryFocus)) {
            salesTodayFilterBtn.setText("Today Items");
            salesWeekFilterBtn.setText("Week Items");
            salesMonthFilterBtn.setText("Month Items");
            salesYearFilterBtn.setText("Year Items");
        } else {
            salesTodayFilterBtn.setText("Today Sales");
            salesWeekFilterBtn.setText("Week Sales");
            salesMonthFilterBtn.setText("Month Sales");
            salesYearFilterBtn.setText("Year Sales");
        }
    }

    private void applyDateRangeForPeriod(String period) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.util.Date start;
        java.util.Date end;

        if ("TODAY".equalsIgnoreCase(period)) {
            start = cal.getTime();
            end = cal.getTime();
        } else if ("WEEK".equalsIgnoreCase(period)) {
            cal.setFirstDayOfWeek(java.util.Calendar.MONDAY);
            cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
            start = cal.getTime();
            cal.add(java.util.Calendar.DAY_OF_MONTH, 6);
            end = cal.getTime();
        } else if ("MONTH".equalsIgnoreCase(period)) {
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
            start = cal.getTime();
            cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
            end = cal.getTime();
        } else if ("YEAR".equalsIgnoreCase(period)) {
            cal.set(java.util.Calendar.DAY_OF_YEAR, 1);
            start = cal.getTime();
            cal.set(java.util.Calendar.MONTH, java.util.Calendar.DECEMBER);
            cal.set(java.util.Calendar.DAY_OF_MONTH, 31);
            end = cal.getTime();
        } else {
            return;
        }

        StartDateSalesHistory_txt.setDate(start);
        endDateSalesHistory_txt.setDate(end);
    }

    private void applySalesHistoryFilters() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date start = StartDateSalesHistory_txt.getDate();
        Date end = endDateSalesHistory_txt.getDate();

        String startDate = (start == null) ? "" : sdf.format(start);
        String endDate = (end == null) ? "" : sdf.format(end);
        String invoiceNo = InvoiceNoSalesHistory_txt.getText().trim();
        String cashier = (String) cashierSalesHistory_cmb.getSelectedItem();
        String paymentMethod = (String) paymentMethodSAlesHistory_cmb.getSelectedItem();

        salesHistory.loadSalesHistoryTableModern(
                salesList_tbl,
                startDate,
                endDate,
                invoiceNo,
                cashier,
                paymentMethod
        );

        SalesHistory.SalesSummary summary = salesHistory.getSalesSummary(
                startDate,
                endDate,
                invoiceNo,
                cashier,
                paymentMethod
        );

        salesHistory.setSummaryLabels(
                totalSales_lbl,
                transactionHsitory_lbl,
                itemSold_lbl,
                totalDiscount_lbl,
                summary
        );

        configureSalesHistoryTableColumns();
        updateSalesSummaryTitles();
        updateSalesFilterButtons();
        updateSalesSummaryCardStyles();
        updateSalesFilterButtonLabels();
    }

    private void configureSalesHistoryTableColumns() {
        if (salesList_tbl.getColumnModel().getColumnCount() < 7) {
            return;
        }

        salesList_tbl.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        salesList_tbl.getColumnModel().getColumn(0).setMinWidth(0);
        salesList_tbl.getColumnModel().getColumn(0).setMaxWidth(0);
        salesList_tbl.getColumnModel().getColumn(0).setWidth(0);

        salesList_tbl.getColumnModel().getColumn(1).setPreferredWidth(150); // Date
        salesList_tbl.getColumnModel().getColumn(2).setPreferredWidth(170); // Receipt No.
        salesList_tbl.getColumnModel().getColumn(3).setPreferredWidth(130); // Customer
        salesList_tbl.getColumnModel().getColumn(4).setPreferredWidth(120); // Total Amount
        salesList_tbl.getColumnModel().getColumn(5).setPreferredWidth(120); // Payment Method
        salesList_tbl.getColumnModel().getColumn(6).setPreferredWidth(180); // Cashier
    }

    private void updateSalesSummaryTitles() {
        jLabel126.setText("TOTAL SALES");
        jLabel123.setText("TRANSACTIONS");
        jLabel124.setText("ITEMS SOLD");
    }

    private void updateSalesFilterButtons() {
        setSalesFilterButtonActive(salesTodayFilterBtn, "TODAY".equalsIgnoreCase(salesHistoryQuickPeriod));
        setSalesFilterButtonActive(salesWeekFilterBtn, "WEEK".equalsIgnoreCase(salesHistoryQuickPeriod));
        setSalesFilterButtonActive(salesMonthFilterBtn, "MONTH".equalsIgnoreCase(salesHistoryQuickPeriod));
        setSalesFilterButtonActive(salesYearFilterBtn, "YEAR".equalsIgnoreCase(salesHistoryQuickPeriod));
    }

    private void setSalesFilterButtonActive(javax.swing.JButton btn, boolean active) {
        if (btn == null) {
            return;
        }
        btn.setBackground(active ? new java.awt.Color(37, 99, 235) : new java.awt.Color(74, 85, 104));
        btn.setBorder(javax.swing.BorderFactory.createLineBorder(
                active ? new java.awt.Color(147, 197, 253) : new java.awt.Color(110, 124, 143),
                active ? 2 : 1
        ));
    }

    private void updateSalesSummaryCardStyles() {
        jPanel38.setBorder(javax.swing.BorderFactory.createLineBorder(
                "SALES".equals(salesHistorySummaryFocus) ? java.awt.Color.WHITE : new java.awt.Color(255, 255, 255, 70),
                "SALES".equals(salesHistorySummaryFocus) ? 2 : 1
        ));
        jPanel40.setBorder(javax.swing.BorderFactory.createLineBorder(
                "TRANSACTIONS".equals(salesHistorySummaryFocus) ? java.awt.Color.WHITE : new java.awt.Color(255, 255, 255, 70),
                "TRANSACTIONS".equals(salesHistorySummaryFocus) ? 2 : 1
        ));
        jPanel39.setBorder(javax.swing.BorderFactory.createLineBorder(
                "PRODUCTS".equals(salesHistorySummaryFocus) ? java.awt.Color.WHITE : new java.awt.Color(255, 255, 255, 70),
                "PRODUCTS".equals(salesHistorySummaryFocus) ? 2 : 1
        ));
    }

    private void installSalesHistoryRealtimeListeners() {
        InvoiceNoSalesHistory_txt.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applySalesHistoryFilters();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applySalesHistoryFilters();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applySalesHistoryFilters();
            }
        });

        StartDateSalesHistory_txt.getDateEditor().addPropertyChangeListener("date", evt -> {
            salesHistoryQuickPeriod = "";
            applySalesHistoryFilters();
        });

        endDateSalesHistory_txt.getDateEditor().addPropertyChangeListener("date", evt -> {
            salesHistoryQuickPeriod = "";
            applySalesHistoryFilters();
        });
    }
    
    
    private void refreshReportsModule() {
        Reports reports = new Reports();

        String reportType = ReportType_txt.getSelectedItem().toString();
        String category = categoryReport_cmb.getSelectedItem().toString();
        String supplier = supplierReport_cmb.getSelectedItem().toString();
        String payment = payment_cmb.getSelectedItem().toString();

        String startDate = reports.formatDateChooser(startDateRepor_txt.getDate());
        String endDate = reports.formatDateChooser(endDateReport_txt.getDate());

        reports.generateReport(
                report_tbl,
                reportType,
                startDate,
                endDate,
                category,
                supplier,
                payment
        );
    }
    
    public void refreshAllAfterCheckout() {
        refreshDashboard();
        refreshSalesHistoryModule();
        refreshReportsModule();
        refreshInventoryModule();
    }
   
    
    
    //otherCredential
    
    //refreshCategory and supplier
    private void refreshProductCombos() {
        Products prod = new Products();
        prod.loadCategoriesToComboBox(category_cmb);
        prod.loadSuppliersToComboBox(supplier_cmb);
        prod.loadCategoryFilterCombo(category_cmb);
        prod.loadSupplierFilterCombo(supplier_cmb);
    }
    
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        MainPanel = new javax.swing.JPanel();
        HeaderPanel = new javax.swing.JPanel();
        userNameWelcome_lbl = new javax.swing.JLabel();
        logout_btn = new javax.swing.JButton();
        jLabel135 = new javax.swing.JLabel();
        jLabel137 = new javax.swing.JLabel();
        jLabel157 = new javax.swing.JLabel();
        HeaderTitle = new javax.swing.JPanel();
        DashboardPanelHeader = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        PosPanelHeader = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        SalesHistoryPanelHeader = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        InventoryPanelHeader = new javax.swing.JPanel();
        jLabel28 = new javax.swing.JLabel();
        ProductPanelHeader = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        CategoryPanelHeader = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        SupplierPanelHeader = new javax.swing.JPanel();
        jLabel23 = new javax.swing.JLabel();
        ReportsPanelHeader = new javax.swing.JPanel();
        jLabel26 = new javax.swing.JLabel();
        UsersPanelHeader = new javax.swing.JPanel();
        jLabel27 = new javax.swing.JLabel();
        SideBarPanel = new javax.swing.JPanel();
        DashboardPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        InventoryPanel = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel136 = new javax.swing.JLabel();
        ReportsPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel141 = new javax.swing.JLabel();
        PosPanel = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel131 = new javax.swing.JLabel();
        ProductsPanel = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel138 = new javax.swing.JLabel();
        CategoriesPanel = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jLabel139 = new javax.swing.JLabel();
        SuppliersPanel = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jLabel140 = new javax.swing.JLabel();
        UsersPanel = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jPanel29 = new javax.swing.JPanel();
        onlineOffline_lbl = new javax.swing.JLabel();
        dateTime_lbl = new javax.swing.JLabel();
        fullnameofUser_lbl = new javax.swing.JLabel();
        roleUser_lbl = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        SalesHistoryPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel134 = new javax.swing.JLabel();
        contentPanel = new javax.swing.JPanel();
        DashboardPanel = new javax.swing.JPanel();
        products_panel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        totalProduct_lbl = new javax.swing.JLabel();
        jLabel143 = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        pieChart_panel = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        dashboard_tbl = new javax.swing.JTable();
        jLabel32 = new javax.swing.JLabel();
        totalCategory_Panel = new javax.swing.JPanel();
        jLabel25 = new javax.swing.JLabel();
        jPanel59 = new javax.swing.JPanel();
        totalCategory_lbl = new javax.swing.JLabel();
        jLabel142 = new javax.swing.JLabel();
        totalSales_Panel = new javax.swing.JPanel();
        jLabel24 = new javax.swing.JLabel();
        jPanel57 = new javax.swing.JPanel();
        totalSalesDashboard = new javax.swing.JLabel();
        jLabel147 = new javax.swing.JLabel();
        transaction_panel = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jLabel29 = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jPanel60 = new javax.swing.JPanel();
        totalTransaction_lbl = new javax.swing.JLabel();
        jLabel149 = new javax.swing.JLabel();
        lowStock_Panel = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        jPanel55 = new javax.swing.JPanel();
        lowstockDashboad_lbl = new javax.swing.JLabel();
        jLabel145 = new javax.swing.JLabel();
        suppliers_panel = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        totalSupplier_lbl = new javax.swing.JLabel();
        jLabel144 = new javax.swing.JLabel();
        users_panel = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        jPanel56 = new javax.swing.JPanel();
        totalUser_lbl = new javax.swing.JLabel();
        jLabel146 = new javax.swing.JLabel();
        outOfStock_Panel = new javax.swing.JPanel();
        jPanel54 = new javax.swing.JPanel();
        jLabel132 = new javax.swing.JLabel();
        jLabel133 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jPanel58 = new javax.swing.JPanel();
        outOfStockdashboard_lbl = new javax.swing.JLabel();
        jLabel148 = new javax.swing.JLabel();
        posPanel = new javax.swing.JPanel();
        headerPanel = new javax.swing.JPanel();
        barcodePanel = new javax.swing.JPanel();
        cartPanel = new javax.swing.JPanel();
        paymentPanel = new javax.swing.JPanel();
        buttonPanel = new javax.swing.JPanel();
        imagePanel = new javax.swing.JPanel();
        userPanel = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jLabel36 = new javax.swing.JLabel();
        jLabel40 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        jLabel53 = new javax.swing.JLabel();
        middlename_txt = new javax.swing.JTextField();
        lastname_txt = new javax.swing.JTextField();
        username_txt = new javax.swing.JTextField();
        role_cmb = new javax.swing.JComboBox<>();
        status_cmb = new javax.swing.JComboBox<>();
        userID_txt = new javax.swing.JTextField();
        addUser_btn = new javax.swing.JButton();
        update_btn = new javax.swing.JButton();
        delete_btn = new javax.swing.JButton();
        clear_btn = new javax.swing.JButton();
        ConfirmpasswordToggle_btn = new javax.swing.JToggleButton();
        passwordToggle_btn = new javax.swing.JToggleButton();
        password_txt = new javax.swing.JPasswordField();
        jLabel55 = new javax.swing.JLabel();
        firstname_txt1 = new javax.swing.JTextField();
        confirmPass_txt = new javax.swing.JPasswordField();
        jLabel57 = new javax.swing.JLabel();
        jPanel14 = new javax.swing.JPanel();
        jLabel33 = new javax.swing.JLabel();
        jPanel16 = new javax.swing.JPanel();
        role_cmb1 = new javax.swing.JComboBox<>();
        jScrollPane2 = new javax.swing.JScrollPane();
        user_tbl = new javax.swing.JTable();
        jLabel47 = new javax.swing.JLabel();
        jLabel150 = new javax.swing.JLabel();
        searchUser_txt = new javax.swing.JTextField();
        jLabel56 = new javax.swing.JLabel();
        salesPanel = new javax.swing.JPanel();
        categoriesPanel = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        addCategory_btn = new javax.swing.JButton();
        clearCategory_btn = new javax.swing.JButton();
        updateCategory_btn = new javax.swing.JButton();
        deleteCategory_btn = new javax.swing.JButton();
        categoryName_txt = new javax.swing.JTextField();
        jLabel37 = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        Description_txt = new javax.swing.JTextArea();
        jLabel43 = new javax.swing.JLabel();
        updatedat_txt1 = new javax.swing.JTextField();
        createdAt_txt1 = new javax.swing.JTextField();
        jPanel18 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        Category_tbl = new javax.swing.JTable();
        jLabel42 = new javax.swing.JLabel();
        jLabel151 = new javax.swing.JLabel();
        categoriesSearch_txt = new javax.swing.JTextField();
        supplierPanel = new javax.swing.JPanel();
        jPanel19 = new javax.swing.JPanel();
        supplierName_txt = new javax.swing.JTextField();
        jLabel44 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        contactPerson_txt = new javax.swing.JTextField();
        jLabel46 = new javax.swing.JLabel();
        contactNumber_txt = new javax.swing.JTextField();
        jLabel58 = new javax.swing.JLabel();
        email_txt = new javax.swing.JTextField();
        jLabel59 = new javax.swing.JLabel();
        address_txt = new javax.swing.JTextField();
        jLabel60 = new javax.swing.JLabel();
        ClearSupplier_btn = new javax.swing.JButton();
        addSupplier_btn = new javax.swing.JButton();
        UpdateSupplier_btn = new javax.swing.JButton();
        deleteSupplier_btn = new javax.swing.JButton();
        jPanel20 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        supplier_tbl = new javax.swing.JTable();
        jLabel152 = new javax.swing.JLabel();
        supplierSearch_txt = new javax.swing.JTextField();
        jLabel63 = new javax.swing.JLabel();
        reportsPanel = new javax.swing.JPanel();
        jPanel48 = new javax.swing.JPanel();
        jPanel49 = new javax.swing.JPanel();
        jLabel104 = new javax.swing.JLabel();
        jPanel50 = new javax.swing.JPanel();
        jPanel51 = new javax.swing.JPanel();
        jLabel110 = new javax.swing.JLabel();
        jLabel111 = new javax.swing.JLabel();
        jLabel114 = new javax.swing.JLabel();
        jLabel129 = new javax.swing.JLabel();
        endDateReport_txt = new com.toedter.calendar.JDateChooser();
        startDateRepor_txt = new com.toedter.calendar.JDateChooser();
        payment_cmb = new javax.swing.JComboBox<>();
        categoryReport_cmb = new javax.swing.JComboBox<>();
        supplierReport_cmb = new javax.swing.JComboBox<>();
        exportExel_btn = new javax.swing.JButton();
        generateReport_btn = new javax.swing.JButton();
        exportPdf_btn = new javax.swing.JButton();
        ReportType_txt = new javax.swing.JComboBox<>();
        jLabel130 = new javax.swing.JLabel();
        jScrollPane14 = new javax.swing.JScrollPane();
        report_tbl = new javax.swing.JTable();
        jPanel52 = new javax.swing.JPanel();
        jLabel128 = new javax.swing.JLabel();
        productsPanel = new javax.swing.JPanel();
        jPanel21 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        product_tbl = new javax.swing.JTable();
        jLabel153 = new javax.swing.JLabel();
        searchProduct_txt = new javax.swing.JTextField();
        jLabel74 = new javax.swing.JLabel();
        jLabel75 = new javax.swing.JLabel();
        statusProduct_cmb = new javax.swing.JComboBox<>();
        jLabel76 = new javax.swing.JLabel();
        categoryProduct_cmb = new javax.swing.JComboBox<>();
        jLabel77 = new javax.swing.JLabel();
        supplierProduct_cmb = new javax.swing.JComboBox<>();
        jPanel22 = new javax.swing.JPanel();
        jLabel64 = new javax.swing.JLabel();
        jLabel65 = new javax.swing.JLabel();
        jLabel66 = new javax.swing.JLabel();
        jLabel67 = new javax.swing.JLabel();
        category_cmb = new javax.swing.JComboBox<>();
        barcode_txt = new javax.swing.JTextField();
        productName_txt = new javax.swing.JTextField();
        jLabel68 = new javax.swing.JLabel();
        supplier_cmb = new javax.swing.JComboBox<>();
        jLabel69 = new javax.swing.JLabel();
        costPrice_txt = new javax.swing.JTextField();
        jLabel70 = new javax.swing.JLabel();
        sellingPrice_txt = new javax.swing.JTextField();
        jLabel71 = new javax.swing.JLabel();
        stockquantity_txt = new javax.swing.JTextField();
        jLabel72 = new javax.swing.JLabel();
        reorderLevel_txt = new javax.swing.JTextField();
        jLabel73 = new javax.swing.JLabel();
        clearProduct_btn = new javax.swing.JButton();
        addProduct_btn = new javax.swing.JButton();
        updateProduct_btn = new javax.swing.JButton();
        deleteProduct_btn = new javax.swing.JButton();
        jPanel23 = new javax.swing.JPanel();
        removeImage_btn = new javax.swing.JButton();
        chooseImage_btn = new javax.swing.JButton();
        Image_txt = new javax.swing.JLabel();
        autoGenerateBarcode_rbt = new javax.swing.JCheckBox();
        inventoryPanel = new javax.swing.JPanel();
        jLabel78 = new javax.swing.JLabel();
        jPanel24 = new javax.swing.JPanel();
        InventoryContentPanel = new javax.swing.JPanel();
        StockIN = new javax.swing.JPanel();
        jLabel79 = new javax.swing.JLabel();
        jPanel25 = new javax.swing.JPanel();
        jLabel82 = new javax.swing.JLabel();
        jLabel83 = new javax.swing.JLabel();
        jLabel84 = new javax.swing.JLabel();
        jLabel85 = new javax.swing.JLabel();
        productStockIN_cmb = new javax.swing.JComboBox<>();
        ReferenceNo_txt = new javax.swing.JTextField();
        supplierIN_btn = new javax.swing.JTextField();
        jDateChooser1 = new com.toedter.calendar.JDateChooser();
        jScrollPane7 = new javax.swing.JScrollPane();
        remarksIn_txt = new javax.swing.JTextArea();
        jLabel86 = new javax.swing.JLabel();
        quantityIn_txt = new javax.swing.JTextField();
        jPanel27 = new javax.swing.JPanel();
        jPanel28 = new javax.swing.JPanel();
        jTextField5 = new javax.swing.JTextField();
        ImageIn_lbl = new javax.swing.JLabel();
        jLabel88 = new javax.swing.JLabel();
        jLabel89 = new javax.swing.JLabel();
        currentStock_lbl = new javax.swing.JLabel();
        barcodeinGen_lbl = new javax.swing.JLabel();
        statusIdentifier_txt = new javax.swing.JTextField();
        barcodeIN_txt = new javax.swing.JTextField();
        jLabel105 = new javax.swing.JLabel();
        jLabel107 = new javax.swing.JLabel();
        StockOut = new javax.swing.JPanel();
        jLabel92 = new javax.swing.JLabel();
        jLabel93 = new javax.swing.JLabel();
        ReferenceNoOUT_txt = new javax.swing.JTextField();
        jLabel94 = new javax.swing.JLabel();
        jLabel95 = new javax.swing.JLabel();
        reason_cmb = new javax.swing.JComboBox<>();
        jLabel96 = new javax.swing.JLabel();
        jPanel30 = new javax.swing.JPanel();
        jLabel97 = new javax.swing.JLabel();
        jLabel98 = new javax.swing.JLabel();
        jDateChooser2 = new com.toedter.calendar.JDateChooser();
        jScrollPane9 = new javax.swing.JScrollPane();
        remarksOUT_txt = new javax.swing.JTextArea();
        jLabel99 = new javax.swing.JLabel();
        quantityOUT_txt = new javax.swing.JTextField();
        jPanel31 = new javax.swing.JPanel();
        jPanel32 = new javax.swing.JPanel();
        jTextField11 = new javax.swing.JTextField();
        ImageOUT_lbl = new javax.swing.JLabel();
        jLabel101 = new javax.swing.JLabel();
        jLabel102 = new javax.swing.JLabel();
        currentStockOUT_lbl = new javax.swing.JLabel();
        barcodeOUTGen_lbl = new javax.swing.JLabel();
        statusIdentifierOUT_txt = new javax.swing.JTextField();
        barcodeOUT_txt = new javax.swing.JTextField();
        ProductOut_cmb = new javax.swing.JComboBox<>();
        StockMovement = new javax.swing.JPanel();
        jPanel35 = new javax.swing.JPanel();
        jLabel115 = new javax.swing.JLabel();
        jLabel112 = new javax.swing.JLabel();
        jLabel117 = new javax.swing.JLabel();
        jLabel118 = new javax.swing.JLabel();
        jLabel119 = new javax.swing.JLabel();
        jLabel120 = new javax.swing.JLabel();
        jLabel121 = new javax.swing.JLabel();
        jPanel37 = new javax.swing.JPanel();
        ImageMovementDisplay_lbl = new javax.swing.JLabel();
        StockQTYMovement_lbl = new javax.swing.JLabel();
        ProductMovement_lbl = new javax.swing.JLabel();
        categoryMovement_lbl = new javax.swing.JLabel();
        supplierMovement_lbl = new javax.swing.JLabel();
        statusMovement_lbl = new javax.swing.JLabel();
        reorderLevel_lbl = new javax.swing.JLabel();
        jtablePanel = new javax.swing.JPanel();
        StockinJ = new javax.swing.JPanel();
        jScrollPane8 = new javax.swing.JScrollPane();
        StockIn_tbl = new javax.swing.JTable();
        jLabel81 = new javax.swing.JLabel();
        jLabel154 = new javax.swing.JLabel();
        searchIN_txt = new javax.swing.JTextField();
        jLabel106 = new javax.swing.JLabel();
        typeIn_cmb = new javax.swing.JComboBox<>();
        jLabel80 = new javax.swing.JLabel();
        jPanel26 = new javax.swing.JPanel();
        stockOutJ = new javax.swing.JPanel();
        jLabel108 = new javax.swing.JLabel();
        jLabel109 = new javax.swing.JLabel();
        jLabel155 = new javax.swing.JLabel();
        searchOUT_txt = new javax.swing.JTextField();
        jScrollPane10 = new javax.swing.JScrollPane();
        StockOUT_tbl = new javax.swing.JTable();
        jPanel34 = new javax.swing.JPanel();
        StockMovementJ = new javax.swing.JPanel();
        jScrollPane11 = new javax.swing.JScrollPane();
        stockMovement_tbl = new javax.swing.JTable();
        jLabel156 = new javax.swing.JLabel();
        stockMovementSearch_txt = new javax.swing.JTextField();
        jLabel113 = new javax.swing.JLabel();
        jPanel36 = new javax.swing.JPanel();
        stockMovementType_cmb = new javax.swing.JComboBox<>();
        jLabel116 = new javax.swing.JLabel();
        jLabel158 = new javax.swing.JLabel();
        stocksType_cmb = new javax.swing.JComboBox<>();
        stockmovement_btn = new javax.swing.JButton();
        clearInv_btn = new javax.swing.JButton();
        stockout_btn = new javax.swing.JButton();
        stockin_btn = new javax.swing.JButton();
        saveInventory_btn = new javax.swing.JButton();
        salesHistoryPanel = new javax.swing.JPanel();
        Header = new javax.swing.JPanel();
        jLabel87 = new javax.swing.JLabel();
        jLabel90 = new javax.swing.JLabel();
        InvoiceNoSalesHistory_txt = new javax.swing.JTextField();
        jLabel91 = new javax.swing.JLabel();
        jLabel100 = new javax.swing.JLabel();
        cashierSalesHistory_cmb = new javax.swing.JComboBox<>();
        jLabel103 = new javax.swing.JLabel();
        paymentMethodSAlesHistory_cmb = new javax.swing.JComboBox<>();
        clearsalesHistory_btn = new javax.swing.JButton();
        searchSalesHistory_btn = new javax.swing.JButton();
        jPanel33 = new javax.swing.JPanel();
        StartDateSalesHistory_txt = new com.toedter.calendar.JDateChooser();
        endDateSalesHistory_txt = new com.toedter.calendar.JDateChooser();
        jPanel38 = new javax.swing.JPanel();
        jPanel42 = new javax.swing.JPanel();
        totalSales_lbl = new javax.swing.JLabel();
        jLabel126 = new javax.swing.JLabel();
        jPanel39 = new javax.swing.JPanel();
        jPanel44 = new javax.swing.JPanel();
        itemSold_lbl = new javax.swing.JLabel();
        jLabel124 = new javax.swing.JLabel();
        jPanel40 = new javax.swing.JPanel();
        jPanel43 = new javax.swing.JPanel();
        transactionHsitory_lbl = new javax.swing.JLabel();
        jLabel123 = new javax.swing.JLabel();
        jPanel41 = new javax.swing.JPanel();
        jPanel45 = new javax.swing.JPanel();
        totalDiscount_lbl = new javax.swing.JLabel();
        jLabel125 = new javax.swing.JLabel();
        jScrollPane12 = new javax.swing.JScrollPane();
        salesList_tbl = new javax.swing.JTable();
        jScrollPane13 = new javax.swing.JScrollPane();
        selectedSaleItems_tbl = new javax.swing.JTable();
        jPanel46 = new javax.swing.JPanel();
        jLabel127 = new javax.swing.JLabel();
        jPanel47 = new javax.swing.JPanel();
        jLabel122 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(1403, 744));
        setMinimumSize(new java.awt.Dimension(1403, 744));
        setResizable(false);

        MainPanel.setBackground(new java.awt.Color(245, 247, 250));
        MainPanel.setMaximumSize(new java.awt.Dimension(1403, 744));
        MainPanel.setMinimumSize(new java.awt.Dimension(1403, 744));
        MainPanel.setPreferredSize(new java.awt.Dimension(1403, 744));
        MainPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        HeaderPanel.setBackground(new java.awt.Color(52, 73, 94));
        HeaderPanel.setMaximumSize(new java.awt.Dimension(1410, 70));
        HeaderPanel.setMinimumSize(new java.awt.Dimension(1410, 70));
        HeaderPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        userNameWelcome_lbl.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        userNameWelcome_lbl.setForeground(new java.awt.Color(255, 255, 255));
        userNameWelcome_lbl.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        HeaderPanel.add(userNameWelcome_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(970, 10, 270, 50));

        logout_btn.setBackground(new java.awt.Color(147, 0, 0));
        logout_btn.setForeground(new java.awt.Color(0, 0, 0));
        logout_btn.setText("Logout");
        logout_btn.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED, new java.awt.Color(153, 0, 0), null));
        logout_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logout_btnActionPerformed(evt);
            }
        });
        HeaderPanel.add(logout_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(1300, 20, 90, 30));

        jLabel135.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel135.setForeground(new java.awt.Color(255, 255, 255));
        jLabel135.setText("StockWise POS Where Inventory Meets Intelligence");
        HeaderPanel.add(jLabel135, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 0, 560, 60));

        jLabel137.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/user.png"))); // NOI18N
        HeaderPanel.add(jLabel137, new org.netbeans.lib.awtextra.AbsoluteConstraints(1250, 10, 30, 50));

        jLabel157.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/logoFinal.png"))); // NOI18N
        jLabel157.setText("jLabel157");
        HeaderPanel.add(jLabel157, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 10, 110, 50));

        MainPanel.add(HeaderPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1410, 70));

        HeaderTitle.setBackground(new java.awt.Color(52, 73, 94));
        HeaderTitle.setLayout(new java.awt.CardLayout());

        DashboardPanelHeader.setBackground(new java.awt.Color(52, 73, 94));
        DashboardPanelHeader.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Dashboard");
        DashboardPanelHeader.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(14, 6, -1, -1));

        HeaderTitle.add(DashboardPanelHeader, "dashboardCard");

        PosPanelHeader.setBackground(new java.awt.Color(52, 73, 94));
        PosPanelHeader.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel11.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 255, 255));
        jLabel11.setText("POINT OF SALE CONTROLLER");
        PosPanelHeader.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(14, 6, -1, -1));

        HeaderTitle.add(PosPanelHeader, "posCard");

        SalesHistoryPanelHeader.setBackground(new java.awt.Color(52, 73, 94));
        SalesHistoryPanelHeader.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel12.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("Sales History");
        SalesHistoryPanelHeader.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(14, 6, -1, -1));

        HeaderTitle.add(SalesHistoryPanelHeader, "salesCard");

        InventoryPanelHeader.setBackground(new java.awt.Color(52, 73, 94));
        InventoryPanelHeader.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel28.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel28.setForeground(new java.awt.Color(255, 255, 255));
        jLabel28.setText("Inventory Management");
        InventoryPanelHeader.add(jLabel28, new org.netbeans.lib.awtextra.AbsoluteConstraints(14, 6, 190, -1));

        HeaderTitle.add(InventoryPanelHeader, "inventoryCard");

        ProductPanelHeader.setBackground(new java.awt.Color(52, 73, 94));
        ProductPanelHeader.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel16.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(255, 255, 255));
        jLabel16.setText("Product Management");
        ProductPanelHeader.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(14, 6, -1, -1));

        HeaderTitle.add(ProductPanelHeader, "productCard");

        CategoryPanelHeader.setBackground(new java.awt.Color(52, 73, 94));
        CategoryPanelHeader.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel21.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel21.setForeground(new java.awt.Color(255, 255, 255));
        jLabel21.setText("Category Management");
        CategoryPanelHeader.add(jLabel21, new org.netbeans.lib.awtextra.AbsoluteConstraints(14, 6, -1, -1));

        HeaderTitle.add(CategoryPanelHeader, "categoryCard");

        SupplierPanelHeader.setBackground(new java.awt.Color(52, 73, 94));
        SupplierPanelHeader.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel23.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel23.setForeground(new java.awt.Color(255, 255, 255));
        jLabel23.setText("Supplier Management");
        SupplierPanelHeader.add(jLabel23, new org.netbeans.lib.awtextra.AbsoluteConstraints(14, 6, -1, -1));

        HeaderTitle.add(SupplierPanelHeader, "supplierCard");

        ReportsPanelHeader.setBackground(new java.awt.Color(52, 73, 94));
        ReportsPanelHeader.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel26.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel26.setForeground(new java.awt.Color(255, 255, 255));
        jLabel26.setText("Reports");
        ReportsPanelHeader.add(jLabel26, new org.netbeans.lib.awtextra.AbsoluteConstraints(14, 6, -1, -1));

        HeaderTitle.add(ReportsPanelHeader, "reportCard");

        UsersPanelHeader.setBackground(new java.awt.Color(52, 73, 94));
        UsersPanelHeader.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel27.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel27.setForeground(new java.awt.Color(255, 255, 255));
        jLabel27.setText("User Management");
        UsersPanelHeader.add(jLabel27, new org.netbeans.lib.awtextra.AbsoluteConstraints(14, 6, -1, -1));

        HeaderTitle.add(UsersPanelHeader, "userCard");

        MainPanel.add(HeaderTitle, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 80, 1190, 40));

        SideBarPanel.setBackground(new java.awt.Color(44, 62, 80));
        SideBarPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        DashboardPanel1.setBackground(new java.awt.Color(44, 62, 80));
        DashboardPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                DashboardPanel1MouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                DashboardPanel1MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                DashboardPanel1MouseExited(evt);
            }
        });
        DashboardPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(236, 240, 241));
        jLabel2.setText("Dashboard");
        DashboardPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, 80, -1));

        jLabel31.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/data.png"))); // NOI18N
        DashboardPanel1.add(jLabel31, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));

        SideBarPanel.add(DashboardPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 460, 190, 40));

        InventoryPanel.setBackground(new java.awt.Color(44, 62, 80));
        InventoryPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                InventoryPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                InventoryPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                InventoryPanelMouseExited(evt);
            }
        });
        InventoryPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(236, 240, 241));
        jLabel4.setText("Inventory");
        InventoryPanel.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, 80, -1));

        jLabel136.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/material-management.png"))); // NOI18N
        InventoryPanel.add(jLabel136, new org.netbeans.lib.awtextra.AbsoluteConstraints(26, 10, -1, -1));

        SideBarPanel.add(InventoryPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 260, 190, 40));

        ReportsPanel.setBackground(new java.awt.Color(44, 62, 80));
        ReportsPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ReportsPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ReportsPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ReportsPanelMouseExited(evt);
            }
        });
        ReportsPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel5.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(236, 240, 241));
        jLabel5.setText("Reports ");
        ReportsPanel.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, 80, -1));

        jLabel141.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/document.png"))); // NOI18N
        ReportsPanel.add(jLabel141, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, 30, 40));

        SideBarPanel.add(ReportsPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 410, 190, 40));

        PosPanel.setBackground(new java.awt.Color(44, 62, 80));
        PosPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                PosPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                PosPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                PosPanelMouseExited(evt);
            }
        });
        PosPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(236, 240, 241));
        jLabel6.setText("Pos");
        PosPanel.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, 40, -1));

        jLabel131.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/pos-terminal.png"))); // NOI18N
        PosPanel.add(jLabel131, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));

        SideBarPanel.add(PosPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 310, 190, 40));

        ProductsPanel.setBackground(new java.awt.Color(44, 62, 80));
        ProductsPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ProductsPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ProductsPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ProductsPanelMouseExited(evt);
            }
        });
        ProductsPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(236, 240, 241));
        jLabel7.setText("Products");
        ProductsPanel.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, 80, -1));

        jLabel138.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/new-product.png"))); // NOI18N
        ProductsPanel.add(jLabel138, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 4, -1, 30));

        SideBarPanel.add(ProductsPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 210, 190, 40));

        CategoriesPanel.setBackground(new java.awt.Color(44, 62, 80));
        CategoriesPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                CategoriesPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                CategoriesPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                CategoriesPanelMouseExited(evt);
            }
        });
        CategoriesPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel8.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(236, 240, 241));
        jLabel8.setText("Categories");
        CategoriesPanel.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, 80, -1));

        jLabel139.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/market-segment.png"))); // NOI18N
        CategoriesPanel.add(jLabel139, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, -1, 40));

        SideBarPanel.add(CategoriesPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 110, 190, 40));

        SuppliersPanel.setBackground(new java.awt.Color(44, 62, 80));
        SuppliersPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                SuppliersPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                SuppliersPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                SuppliersPanelMouseExited(evt);
            }
        });
        SuppliersPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel9.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(236, 240, 241));
        jLabel9.setText("Suppliers");
        SuppliersPanel.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, 80, -1));

        jLabel140.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/supplier.png"))); // NOI18N
        SuppliersPanel.add(jLabel140, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, 30, 40));

        SideBarPanel.add(SuppliersPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 160, 190, 40));

        UsersPanel.setBackground(new java.awt.Color(44, 62, 80));
        UsersPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                UsersPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                UsersPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                UsersPanelMouseExited(evt);
            }
        });
        UsersPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel10.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(236, 240, 241));
        jLabel10.setText("Users");
        UsersPanel.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, 80, -1));

        jLabel15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/team.png"))); // NOI18N
        UsersPanel.add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, 30, 40));

        SideBarPanel.add(UsersPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 60, 190, 40));

        jPanel29.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel29Layout = new javax.swing.GroupLayout(jPanel29);
        jPanel29.setLayout(jPanel29Layout);
        jPanel29Layout.setHorizontalGroup(
            jPanel29Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 190, Short.MAX_VALUE)
        );
        jPanel29Layout.setVerticalGroup(
            jPanel29Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        SideBarPanel.add(jPanel29, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 530, 190, 1));

        onlineOffline_lbl.setForeground(new java.awt.Color(255, 255, 255));
        SideBarPanel.add(onlineOffline_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 620, 130, 20));

        dateTime_lbl.setForeground(new java.awt.Color(255, 255, 255));
        dateTime_lbl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        SideBarPanel.add(dateTime_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 650, 190, 20));

        fullnameofUser_lbl.setForeground(new java.awt.Color(255, 255, 255));
        SideBarPanel.add(fullnameofUser_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 560, 130, 20));

        roleUser_lbl.setForeground(new java.awt.Color(255, 255, 255));
        SideBarPanel.add(roleUser_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 590, 130, 20));

        jLabel34.setForeground(new java.awt.Color(255, 255, 255));
        jLabel34.setText("CURRENT USER");
        SideBarPanel.add(jLabel34, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 536, 130, 20));

        SalesHistoryPanel.setBackground(new java.awt.Color(44, 62, 80));
        SalesHistoryPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                SalesHistoryPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                SalesHistoryPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                SalesHistoryPanelMouseExited(evt);
            }
        });
        SalesHistoryPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(236, 240, 241));
        jLabel1.setText("Sales History");
        SalesHistoryPanel.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, 80, -1));

        jLabel134.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/buy.png"))); // NOI18N
        SalesHistoryPanel.add(jLabel134, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));

        SideBarPanel.add(SalesHistoryPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 360, 190, 40));

        MainPanel.add(SideBarPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 70, 190, 680));

        contentPanel.setBackground(new java.awt.Color(255, 255, 255));
        contentPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        contentPanel.setLayout(new java.awt.CardLayout());

        DashboardPanel.setBackground(new java.awt.Color(255, 255, 255));
        DashboardPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        products_panel.setBackground(new java.awt.Color(24, 15, 105));
        products_panel.setBorder(javax.swing.BorderFactory.createEtchedBorder(java.awt.Color.blue, null));
        products_panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                products_panelMouseClicked(evt);
            }
        });
        products_panel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        products_panel.add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 0, 1, 100));

        jLabel19.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(255, 255, 255));
        jLabel19.setText("TOTAL PRODUCTS");
        products_panel.add(jLabel19, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 10, -1, -1));

        totalProduct_lbl.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        totalProduct_lbl.setForeground(new java.awt.Color(255, 255, 255));
        products_panel.add(totalProduct_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 50, 160, 40));

        jLabel143.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/new-product.png"))); // NOI18N
        products_panel.add(jLabel143, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 30, -1));

        DashboardPanel.add(products_panel, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 20, 210, 100));

        jPanel11.setBackground(new java.awt.Color(236, 240, 241));
        jPanel11.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel17.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(0, 0, 0));
        jLabel17.setText("DAILY SALES GRAPH");
        jPanel11.add(jLabel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 190, -1));

        pieChart_panel.setBackground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout pieChart_panelLayout = new javax.swing.GroupLayout(pieChart_panel);
        pieChart_panel.setLayout(pieChart_panelLayout);
        pieChart_panelLayout.setHorizontalGroup(
            pieChart_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 630, Short.MAX_VALUE)
        );
        pieChart_panelLayout.setVerticalGroup(
            pieChart_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 260, Short.MAX_VALUE)
        );

        jPanel11.add(pieChart_panel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, 630, 260));

        DashboardPanel.add(jPanel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 260, 650, 310));

        jPanel12.setBackground(new java.awt.Color(236, 240, 241));
        jPanel12.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        dashboard_tbl.setBackground(new java.awt.Color(255, 255, 255));
        dashboard_tbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Product Name", "Qty Sold", "Total Sales"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        dashboard_tbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                dashboard_tblMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(dashboard_tbl);
        if (dashboard_tbl.getColumnModel().getColumnCount() > 0) {
            dashboard_tbl.getColumnModel().getColumn(0).setResizable(false);
            dashboard_tbl.getColumnModel().getColumn(1).setResizable(false);
            dashboard_tbl.getColumnModel().getColumn(2).setResizable(false);
        }

        jPanel12.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, 460, 260));

        jLabel32.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel32.setForeground(new java.awt.Color(0, 0, 0));
        jLabel32.setText("TOP SELLING PRODUCTS");
        jPanel12.add(jLabel32, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 190, -1));

        DashboardPanel.add(jPanel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 260, 480, 310));

        totalCategory_Panel.setBackground(new java.awt.Color(18, 114, 137));
        totalCategory_Panel.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(102, 51, 255), null));
        totalCategory_Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                totalCategory_PanelMouseClicked(evt);
            }
        });
        totalCategory_Panel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel25.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel25.setForeground(new java.awt.Color(255, 255, 255));
        jLabel25.setText("TOTAL CATEGORY");
        totalCategory_Panel.add(jLabel25, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, -1, -1));

        jPanel59.setBackground(new java.awt.Color(0, 0, 0));

        javax.swing.GroupLayout jPanel59Layout = new javax.swing.GroupLayout(jPanel59);
        jPanel59.setLayout(jPanel59Layout);
        jPanel59Layout.setHorizontalGroup(
            jPanel59Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );
        jPanel59Layout.setVerticalGroup(
            jPanel59Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        totalCategory_Panel.add(jPanel59, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 0, 1, -1));

        totalCategory_lbl.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        totalCategory_lbl.setForeground(new java.awt.Color(255, 255, 255));
        totalCategory_Panel.add(totalCategory_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 50, 150, 40));

        jLabel142.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/market-segment.png"))); // NOI18N
        totalCategory_Panel.add(jLabel142, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 30, -1));

        DashboardPanel.add(totalCategory_Panel, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 20, 210, 100));

        totalSales_Panel.setBackground(new java.awt.Color(211, 69, 18));
        totalSales_Panel.setBorder(javax.swing.BorderFactory.createEtchedBorder(java.awt.Color.orange, null));
        totalSales_Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                totalSales_PanelMouseClicked(evt);
            }
        });
        totalSales_Panel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel24.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel24.setForeground(new java.awt.Color(255, 255, 255));
        jLabel24.setText("TOTAL SALES");
        totalSales_Panel.add(jLabel24, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, -1, -1));

        jPanel57.setBackground(new java.awt.Color(0, 0, 0));

        javax.swing.GroupLayout jPanel57Layout = new javax.swing.GroupLayout(jPanel57);
        jPanel57.setLayout(jPanel57Layout);
        jPanel57Layout.setHorizontalGroup(
            jPanel57Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );
        jPanel57Layout.setVerticalGroup(
            jPanel57Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        totalSales_Panel.add(jPanel57, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 0, 1, -1));

        totalSalesDashboard.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        totalSalesDashboard.setForeground(new java.awt.Color(255, 255, 255));
        totalSales_Panel.add(totalSalesDashboard, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 50, 150, 40));

        jLabel147.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/sales.png"))); // NOI18N
        totalSales_Panel.add(jLabel147, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 30, -1));

        DashboardPanel.add(totalSales_Panel, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 20, 210, 100));

        transaction_panel.setBackground(new java.awt.Color(8, 124, 8));
        transaction_panel.setBorder(javax.swing.BorderFactory.createEtchedBorder(java.awt.Color.blue, null));
        transaction_panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                transaction_panelMouseClicked(evt);
            }
        });
        transaction_panel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel10.setBackground(new java.awt.Color(236, 240, 241));
        jPanel10.setBorder(javax.swing.BorderFactory.createEtchedBorder(java.awt.Color.blue, null));
        jPanel10.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel29.setForeground(new java.awt.Color(0, 0, 0));
        jLabel29.setText("TOTAL PRODUCTS");
        jPanel10.add(jLabel29, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 10, -1, -1));

        jLabel30.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel30.setForeground(new java.awt.Color(0, 0, 0));
        jLabel30.setText("20");
        jPanel10.add(jLabel30, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 30, -1, -1));

        transaction_panel.add(jPanel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 210, 160, 70));

        jLabel14.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(255, 255, 255));
        jLabel14.setText("TOTAL TRANSACTION");
        transaction_panel.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, -1, -1));

        jPanel60.setBackground(new java.awt.Color(0, 0, 0));

        javax.swing.GroupLayout jPanel60Layout = new javax.swing.GroupLayout(jPanel60);
        jPanel60.setLayout(jPanel60Layout);
        jPanel60Layout.setHorizontalGroup(
            jPanel60Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );
        jPanel60Layout.setVerticalGroup(
            jPanel60Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        transaction_panel.add(jPanel60, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 0, 1, -1));

        totalTransaction_lbl.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        totalTransaction_lbl.setForeground(new java.awt.Color(255, 255, 255));
        transaction_panel.add(totalTransaction_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 50, 150, 40));

        jLabel149.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/card-payment.png"))); // NOI18N
        transaction_panel.add(jLabel149, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 30, -1));

        DashboardPanel.add(transaction_panel, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 140, 210, 100));

        lowStock_Panel.setBackground(new java.awt.Color(164, 88, 15));
        lowStock_Panel.setBorder(javax.swing.BorderFactory.createEtchedBorder(java.awt.Color.yellow, null));
        lowStock_Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lowStock_PanelMouseClicked(evt);
            }
        });
        lowStock_Panel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel20.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel20.setForeground(new java.awt.Color(255, 255, 255));
        jLabel20.setText("LOW STOCK");
        lowStock_Panel.add(jLabel20, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 10, -1, -1));

        jPanel55.setBackground(new java.awt.Color(0, 0, 0));

        javax.swing.GroupLayout jPanel55Layout = new javax.swing.GroupLayout(jPanel55);
        jPanel55.setLayout(jPanel55Layout);
        jPanel55Layout.setHorizontalGroup(
            jPanel55Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );
        jPanel55Layout.setVerticalGroup(
            jPanel55Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 112, Short.MAX_VALUE)
        );

        lowStock_Panel.add(jPanel55, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 0, 1, -1));

        lowstockDashboad_lbl.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        lowstockDashboad_lbl.setForeground(new java.awt.Color(255, 255, 255));
        lowStock_Panel.add(lowstockDashboad_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 50, 150, 40));

        jLabel145.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/stock-market.png"))); // NOI18N
        lowStock_Panel.add(jLabel145, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 30, -1));

        DashboardPanel.add(lowStock_Panel, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 140, 210, 100));

        suppliers_panel.setBackground(new java.awt.Color(55, 73, 144));
        suppliers_panel.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(0, 153, 255), null));
        suppliers_panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                suppliers_panelMouseClicked(evt);
            }
        });
        suppliers_panel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel13.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("TOTAL SUPPLIER");
        suppliers_panel.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 10, -1, -1));

        jPanel8.setBackground(new java.awt.Color(0, 0, 0));

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        suppliers_panel.add(jPanel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 0, 1, -1));

        totalSupplier_lbl.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        totalSupplier_lbl.setForeground(new java.awt.Color(255, 255, 255));
        suppliers_panel.add(totalSupplier_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 50, 160, 40));

        jLabel144.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/supplier (1).png"))); // NOI18N
        suppliers_panel.add(jLabel144, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 30, -1));

        DashboardPanel.add(suppliers_panel, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 140, 210, 100));

        users_panel.setBackground(new java.awt.Color(28, 111, 44));
        users_panel.setBorder(javax.swing.BorderFactory.createEtchedBorder(java.awt.Color.green, null));
        users_panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                users_panelMouseClicked(evt);
            }
        });
        users_panel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel18.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(255, 255, 255));
        jLabel18.setText("TOTAL USERS");
        users_panel.add(jLabel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, -1, -1));

        jPanel56.setBackground(new java.awt.Color(0, 0, 0));

        javax.swing.GroupLayout jPanel56Layout = new javax.swing.GroupLayout(jPanel56);
        jPanel56.setLayout(jPanel56Layout);
        jPanel56Layout.setHorizontalGroup(
            jPanel56Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );
        jPanel56Layout.setVerticalGroup(
            jPanel56Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        users_panel.add(jPanel56, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 0, 1, -1));

        totalUser_lbl.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        totalUser_lbl.setForeground(new java.awt.Color(255, 255, 255));
        users_panel.add(totalUser_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 50, 150, 40));

        jLabel146.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/team.png"))); // NOI18N
        users_panel.add(jLabel146, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 30, -1));

        DashboardPanel.add(users_panel, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 20, 210, 100));

        outOfStock_Panel.setBackground(new java.awt.Color(164, 0, 0));
        outOfStock_Panel.setBorder(javax.swing.BorderFactory.createEtchedBorder(java.awt.Color.blue, null));
        outOfStock_Panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                outOfStock_PanelMouseClicked(evt);
            }
        });
        outOfStock_Panel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel54.setBackground(new java.awt.Color(236, 240, 241));
        jPanel54.setBorder(javax.swing.BorderFactory.createEtchedBorder(java.awt.Color.blue, null));
        jPanel54.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel132.setForeground(new java.awt.Color(0, 0, 0));
        jLabel132.setText("TOTAL PRODUCTS");
        jPanel54.add(jLabel132, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 10, -1, -1));

        jLabel133.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel133.setForeground(new java.awt.Color(0, 0, 0));
        jLabel133.setText("20");
        jPanel54.add(jLabel133, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 30, -1, -1));

        outOfStock_Panel.add(jPanel54, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 210, 160, 70));

        jLabel22.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel22.setForeground(new java.awt.Color(255, 255, 255));
        jLabel22.setText("OUT OF STOCK");
        outOfStock_Panel.add(jLabel22, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, -1, -1));

        jPanel58.setBackground(new java.awt.Color(0, 0, 0));

        javax.swing.GroupLayout jPanel58Layout = new javax.swing.GroupLayout(jPanel58);
        jPanel58.setLayout(jPanel58Layout);
        jPanel58Layout.setHorizontalGroup(
            jPanel58Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );
        jPanel58Layout.setVerticalGroup(
            jPanel58Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        outOfStock_Panel.add(jPanel58, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 0, 1, -1));

        outOfStockdashboard_lbl.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        outOfStockdashboard_lbl.setForeground(new java.awt.Color(255, 255, 255));
        outOfStock_Panel.add(outOfStockdashboard_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 50, 150, 40));

        jLabel148.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/out-of-stock (1).png"))); // NOI18N
        outOfStock_Panel.add(jLabel148, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 30, -1));

        DashboardPanel.add(outOfStock_Panel, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 140, 210, 100));

        contentPanel.add(DashboardPanel, "dashboard");

        posPanel.setBackground(new java.awt.Color(255, 255, 255));
        posPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        headerPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        posPanel.add(headerPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 1170, 70));

        barcodePanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        posPanel.add(barcodePanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 90, 1170, 70));

        cartPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        posPanel.add(cartPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 170, 610, 350));

        paymentPanel.setLayout(new javax.swing.BoxLayout(paymentPanel, javax.swing.BoxLayout.LINE_AXIS));
        posPanel.add(paymentPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(830, 170, 350, 420));

        buttonPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        posPanel.add(buttonPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 530, 440, 60));

        imagePanel.setBackground(new java.awt.Color(153, 153, 153));
        imagePanel.setForeground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout imagePanelLayout = new javax.swing.GroupLayout(imagePanel);
        imagePanel.setLayout(imagePanelLayout);
        imagePanelLayout.setHorizontalGroup(
            imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 190, Short.MAX_VALUE)
        );
        imagePanelLayout.setVerticalGroup(
            imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 270, Short.MAX_VALUE)
        );

        posPanel.add(imagePanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 170, 190, 270));

        contentPanel.add(posPanel, "pos");

        userPanel.setBackground(new java.awt.Color(255, 255, 255));
        userPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel13.setBackground(new java.awt.Color(248, 249, 250));
        jPanel13.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel15.setBackground(new java.awt.Color(204, 204, 204));

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 450, Short.MAX_VALUE)
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        jPanel13.add(jPanel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, 450, 1));

        jLabel36.setBackground(new java.awt.Color(0, 0, 0));
        jLabel36.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel36.setForeground(new java.awt.Color(52, 73, 94));
        jLabel36.setText("USER DETAILS");
        jPanel13.add(jLabel36, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 160, -1));

        jLabel40.setBackground(new java.awt.Color(0, 0, 0));
        jLabel40.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel40.setForeground(new java.awt.Color(0, 0, 0));
        jLabel40.setText("User ID");
        jPanel13.add(jLabel40, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 90, 90, -1));

        jLabel48.setBackground(new java.awt.Color(0, 0, 0));
        jLabel48.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel48.setForeground(new java.awt.Color(0, 0, 0));
        jLabel48.setText("Middle Name");
        jPanel13.add(jLabel48, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 160, 90, 30));

        jLabel49.setBackground(new java.awt.Color(0, 0, 0));
        jLabel49.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel49.setForeground(new java.awt.Color(0, 0, 0));
        jLabel49.setText("Last Name");
        jPanel13.add(jLabel49, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 200, 90, 30));

        jLabel50.setBackground(new java.awt.Color(0, 0, 0));
        jLabel50.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel50.setForeground(new java.awt.Color(0, 0, 0));
        jLabel50.setText("Username");
        jPanel13.add(jLabel50, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 240, 90, 30));

        jLabel51.setBackground(new java.awt.Color(0, 0, 0));
        jLabel51.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel51.setForeground(new java.awt.Color(0, 0, 0));
        jLabel51.setText("Confirm Password");
        jPanel13.add(jLabel51, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 310, 120, 50));

        jLabel52.setBackground(new java.awt.Color(0, 0, 0));
        jLabel52.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel52.setForeground(new java.awt.Color(0, 0, 0));
        jLabel52.setText("Role:");
        jPanel13.add(jLabel52, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 350, 90, 50));

        jLabel53.setBackground(new java.awt.Color(0, 0, 0));
        jLabel53.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel53.setForeground(new java.awt.Color(0, 0, 0));
        jLabel53.setText("Status:");
        jPanel13.add(jLabel53, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 390, 90, 50));

        middlename_txt.setBackground(new java.awt.Color(255, 255, 255));
        middlename_txt.setForeground(new java.awt.Color(0, 0, 0));
        middlename_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                middlename_txtKeyTyped(evt);
            }
        });
        jPanel13.add(middlename_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 160, 300, 30));

        lastname_txt.setBackground(new java.awt.Color(255, 255, 255));
        lastname_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                lastname_txtKeyTyped(evt);
            }
        });
        jPanel13.add(lastname_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 200, 300, 30));

        username_txt.setBackground(new java.awt.Color(255, 255, 255));
        jPanel13.add(username_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 240, 300, 30));

        role_cmb.setBackground(new java.awt.Color(255, 255, 255));
        role_cmb.setForeground(new java.awt.Color(0, 0, 0));
        role_cmb.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Admin", "Manager", "Cashier", "Inventory Clerk" }));
        jPanel13.add(role_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 360, 300, 30));

        status_cmb.setBackground(new java.awt.Color(255, 255, 255));
        status_cmb.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Active", "Inactive" }));
        jPanel13.add(status_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 400, 300, 30));

        userID_txt.setEditable(false);
        userID_txt.setBackground(new java.awt.Color(255, 255, 255));
        userID_txt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        userID_txt.setText("[Auto]");
        jPanel13.add(userID_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 80, 50, 30));

        addUser_btn.setBackground(new java.awt.Color(6, 75, 9));
        addUser_btn.setForeground(new java.awt.Color(255, 255, 255));
        addUser_btn.setText("Add User");
        addUser_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addUser_btnActionPerformed(evt);
            }
        });
        jPanel13.add(addUser_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 480, 100, 40));

        update_btn.setBackground(new java.awt.Color(5, 59, 114));
        update_btn.setText("UPDATE");
        update_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                update_btnActionPerformed(evt);
            }
        });
        jPanel13.add(update_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 480, 100, 40));

        delete_btn.setBackground(new java.awt.Color(128, 0, 16));
        delete_btn.setText("DELETE");
        delete_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delete_btnActionPerformed(evt);
            }
        });
        jPanel13.add(delete_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 480, 100, 40));

        clear_btn.setBackground(new java.awt.Color(154, 154, 147));
        clear_btn.setText("CLEAR");
        clear_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clear_btnActionPerformed(evt);
            }
        });
        jPanel13.add(clear_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 480, 100, 40));

        ConfirmpasswordToggle_btn.setBackground(new java.awt.Color(255, 255, 255));
        ConfirmpasswordToggle_btn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/view.png"))); // NOI18N
        ConfirmpasswordToggle_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ConfirmpasswordToggle_btnActionPerformed(evt);
            }
        });
        jPanel13.add(ConfirmpasswordToggle_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 320, 30, 30));

        passwordToggle_btn.setBackground(new java.awt.Color(255, 255, 255));
        passwordToggle_btn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/view.png"))); // NOI18N
        passwordToggle_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                passwordToggle_btnActionPerformed(evt);
            }
        });
        jPanel13.add(passwordToggle_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 280, 30, 30));

        password_txt.setBackground(new java.awt.Color(255, 255, 255));
        jPanel13.add(password_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 280, 300, 30));

        jLabel55.setBackground(new java.awt.Color(0, 0, 0));
        jLabel55.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel55.setForeground(new java.awt.Color(0, 0, 0));
        jLabel55.setText("First Name");
        jPanel13.add(jLabel55, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 120, 90, 30));

        firstname_txt1.setBackground(new java.awt.Color(255, 255, 255));
        firstname_txt1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                firstname_txt1KeyTyped(evt);
            }
        });
        jPanel13.add(firstname_txt1, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 120, 300, 30));

        confirmPass_txt.setBackground(new java.awt.Color(255, 255, 255));
        confirmPass_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                confirmPass_txtKeyReleased(evt);
            }
        });
        jPanel13.add(confirmPass_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 320, 300, 30));

        jLabel57.setBackground(new java.awt.Color(0, 0, 0));
        jLabel57.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel57.setForeground(new java.awt.Color(0, 0, 0));
        jLabel57.setText("Password");
        jPanel13.add(jLabel57, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 270, 90, 50));

        userPanel.add(jPanel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, 480, 560));

        jPanel14.setBackground(new java.awt.Color(248, 249, 250));
        jPanel14.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel33.setBackground(new java.awt.Color(0, 0, 0));
        jLabel33.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel33.setForeground(new java.awt.Color(52, 73, 94));
        jLabel33.setText("USER LIST");
        jPanel14.add(jLabel33, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 160, -1));

        jPanel16.setBackground(new java.awt.Color(204, 204, 204));

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 640, Short.MAX_VALUE)
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        jPanel14.add(jPanel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, 640, 1));

        role_cmb1.setBackground(new java.awt.Color(255, 255, 255));
        role_cmb1.setForeground(new java.awt.Color(0, 0, 0));
        role_cmb1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ALL", "Super Admin", "Manager", "Cashier", "Inventory Clerk" }));
        role_cmb1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                role_cmb1ActionPerformed(evt);
            }
        });
        jPanel14.add(role_cmb1, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 60, 190, 40));

        user_tbl.setBackground(new java.awt.Color(255, 255, 255));
        user_tbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Fullname", "role", "status", "Created At", "Updated At"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        user_tbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                user_tblMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(user_tbl);
        if (user_tbl.getColumnModel().getColumnCount() > 0) {
            user_tbl.getColumnModel().getColumn(1).setResizable(false);
            user_tbl.getColumnModel().getColumn(2).setResizable(false);
            user_tbl.getColumnModel().getColumn(3).setResizable(false);
            user_tbl.getColumnModel().getColumn(4).setResizable(false);
            user_tbl.getColumnModel().getColumn(5).setResizable(false);
        }

        jPanel14.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 120, 640, 410));

        jLabel47.setBackground(new java.awt.Color(0, 0, 0));
        jLabel47.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel47.setForeground(new java.awt.Color(0, 0, 0));
        jLabel47.setText("Role");
        jPanel14.add(jLabel47, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 60, 40, 40));

        jLabel150.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/search (2).png"))); // NOI18N
        jPanel14.add(jLabel150, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 60, 30, 40));

        searchUser_txt.setBackground(new java.awt.Color(255, 255, 255));
        searchUser_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchUser_txtKeyReleased(evt);
            }
        });
        jPanel14.add(searchUser_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 60, 320, 40));

        jLabel56.setBackground(new java.awt.Color(0, 0, 0));
        jLabel56.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel56.setForeground(new java.awt.Color(0, 0, 0));
        jLabel56.setText("Search");
        jPanel14.add(jLabel56, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 60, 50, 40));

        userPanel.add(jPanel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 20, 660, 560));

        contentPanel.add(userPanel, "users");

        salesPanel.setBackground(new java.awt.Color(255, 255, 255));
        salesPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        contentPanel.add(salesPanel, "sales");

        categoriesPanel.setBackground(new java.awt.Color(255, 255, 255));
        categoriesPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel17.setBackground(new java.awt.Color(248, 249, 250));
        jPanel17.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        addCategory_btn.setBackground(new java.awt.Color(6, 75, 9));
        addCategory_btn.setForeground(new java.awt.Color(255, 255, 255));
        addCategory_btn.setText("ADD");
        addCategory_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addCategory_btnActionPerformed(evt);
            }
        });
        jPanel17.add(addCategory_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(500, 230, 90, 30));

        clearCategory_btn.setBackground(new java.awt.Color(154, 154, 147));
        clearCategory_btn.setForeground(new java.awt.Color(0, 0, 0));
        clearCategory_btn.setText("CLEAR");
        clearCategory_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearCategory_btnActionPerformed(evt);
            }
        });
        jPanel17.add(clearCategory_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(830, 230, 90, 30));

        updateCategory_btn.setBackground(new java.awt.Color(5, 59, 114));
        updateCategory_btn.setForeground(new java.awt.Color(255, 255, 255));
        updateCategory_btn.setText("UPDATE");
        updateCategory_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateCategory_btnActionPerformed(evt);
            }
        });
        jPanel17.add(updateCategory_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 230, 90, 30));

        deleteCategory_btn.setBackground(new java.awt.Color(128, 0, 16));
        deleteCategory_btn.setForeground(new java.awt.Color(255, 255, 255));
        deleteCategory_btn.setText("DELETE");
        deleteCategory_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteCategory_btnActionPerformed(evt);
            }
        });
        jPanel17.add(deleteCategory_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(720, 230, 90, 30));

        categoryName_txt.setBackground(new java.awt.Color(255, 255, 255));
        jPanel17.add(categoryName_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 50, 310, 30));

        jLabel37.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel37.setForeground(new java.awt.Color(0, 0, 0));
        jLabel37.setText("Category Details");
        jPanel17.add(jLabel37, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, -1, -1));

        jLabel38.setForeground(new java.awt.Color(0, 0, 0));
        jLabel38.setText("Category Name");
        jPanel17.add(jLabel38, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 60, 90, 20));

        jLabel39.setForeground(new java.awt.Color(0, 0, 0));
        jLabel39.setText("Description");
        jPanel17.add(jLabel39, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 76, 90, 40));

        jLabel41.setForeground(new java.awt.Color(0, 0, 0));
        jLabel41.setText("Udpated At");
        jPanel17.add(jLabel41, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 230, 80, 30));

        Description_txt.setBackground(new java.awt.Color(255, 255, 255));
        Description_txt.setColumns(20);
        Description_txt.setRows(5);
        jScrollPane3.setViewportView(Description_txt);

        jPanel17.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 90, 310, 90));

        jLabel43.setForeground(new java.awt.Color(0, 0, 0));
        jLabel43.setText("Created At");
        jPanel17.add(jLabel43, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 200, 110, 20));

        updatedat_txt1.setEditable(false);
        updatedat_txt1.setBackground(new java.awt.Color(255, 255, 255));
        updatedat_txt1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        updatedat_txt1.setText("[Auto]");
        jPanel17.add(updatedat_txt1, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 230, 310, 30));

        createdAt_txt1.setEditable(false);
        createdAt_txt1.setBackground(new java.awt.Color(255, 255, 255));
        createdAt_txt1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        createdAt_txt1.setText("[Auto]");
        jPanel17.add(createdAt_txt1, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 190, 310, 30));

        categoriesPanel.add(jPanel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 10, 1080, 270));

        jPanel18.setBackground(new java.awt.Color(248, 249, 250));
        jPanel18.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        Category_tbl.setBackground(new java.awt.Color(245, 247, 251));
        Category_tbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Category ID", "Category Name", "Description", "Created At", "Updated At"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane4.setViewportView(Category_tbl);
        if (Category_tbl.getColumnModel().getColumnCount() > 0) {
            Category_tbl.getColumnModel().getColumn(0).setResizable(false);
            Category_tbl.getColumnModel().getColumn(1).setResizable(false);
            Category_tbl.getColumnModel().getColumn(2).setResizable(false);
            Category_tbl.getColumnModel().getColumn(3).setResizable(false);
            Category_tbl.getColumnModel().getColumn(4).setResizable(false);
        }

        jPanel18.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 60, 1060, 220));

        jLabel42.setForeground(new java.awt.Color(0, 0, 0));
        jLabel42.setText("Search:");
        jPanel18.add(jLabel42, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, 50, 30));

        jLabel151.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/search (2).png"))); // NOI18N
        jPanel18.add(jLabel151, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 10, 30, 30));

        categoriesSearch_txt.setBackground(new java.awt.Color(255, 255, 255));
        categoriesSearch_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                categoriesSearch_txtKeyReleased(evt);
            }
        });
        jPanel18.add(categoriesSearch_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 10, 290, 30));

        categoriesPanel.add(jPanel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 300, 1090, 290));

        contentPanel.add(categoriesPanel, "categories");

        supplierPanel.setBackground(new java.awt.Color(255, 255, 255));
        supplierPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel19.setBackground(new java.awt.Color(245, 247, 251));
        jPanel19.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        supplierName_txt.setBackground(new java.awt.Color(255, 255, 255));
        jPanel19.add(supplierName_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 70, 390, 40));

        jLabel44.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel44.setForeground(new java.awt.Color(0, 0, 0));
        jLabel44.setText("Supplier Details");
        jPanel19.add(jLabel44, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, -1, 20));

        jLabel45.setForeground(new java.awt.Color(0, 0, 0));
        jLabel45.setText("Supplier Name:");
        jPanel19.add(jLabel45, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, 90, -1));

        contactPerson_txt.setBackground(new java.awt.Color(255, 255, 255));
        jPanel19.add(contactPerson_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 140, 390, 40));

        jLabel46.setForeground(new java.awt.Color(0, 0, 0));
        jLabel46.setText("Contact Person:");
        jPanel19.add(jLabel46, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 120, 90, -1));

        contactNumber_txt.setBackground(new java.awt.Color(255, 255, 255));
        contactNumber_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                contactNumber_txtKeyTyped(evt);
            }
        });
        jPanel19.add(contactNumber_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 220, 390, 40));

        jLabel58.setForeground(new java.awt.Color(0, 0, 0));
        jLabel58.setText("Contact Number:");
        jPanel19.add(jLabel58, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 200, 110, -1));

        email_txt.setBackground(new java.awt.Color(255, 255, 255));
        jPanel19.add(email_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 300, 390, 40));

        jLabel59.setForeground(new java.awt.Color(0, 0, 0));
        jLabel59.setText("Email:");
        jPanel19.add(jLabel59, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 280, 110, -1));

        address_txt.setBackground(new java.awt.Color(255, 255, 255));
        jPanel19.add(address_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 380, 390, 40));

        jLabel60.setForeground(new java.awt.Color(0, 0, 0));
        jLabel60.setText("Address:");
        jPanel19.add(jLabel60, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 360, 110, -1));

        ClearSupplier_btn.setBackground(new java.awt.Color(154, 154, 147));
        ClearSupplier_btn.setForeground(new java.awt.Color(255, 255, 255));
        ClearSupplier_btn.setText("CLEAR");
        ClearSupplier_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClearSupplier_btnActionPerformed(evt);
            }
        });
        jPanel19.add(ClearSupplier_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 480, 90, 30));

        addSupplier_btn.setBackground(new java.awt.Color(6, 75, 9));
        addSupplier_btn.setForeground(new java.awt.Color(255, 255, 255));
        addSupplier_btn.setText("ADD");
        addSupplier_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSupplier_btnActionPerformed(evt);
            }
        });
        jPanel19.add(addSupplier_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 480, 90, 30));

        UpdateSupplier_btn.setBackground(new java.awt.Color(5, 59, 114));
        UpdateSupplier_btn.setText("UPDATE");
        UpdateSupplier_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UpdateSupplier_btnActionPerformed(evt);
            }
        });
        jPanel19.add(UpdateSupplier_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 480, 90, 30));

        deleteSupplier_btn.setBackground(new java.awt.Color(128, 0, 16));
        deleteSupplier_btn.setForeground(new java.awt.Color(255, 255, 255));
        deleteSupplier_btn.setText("DELETE");
        deleteSupplier_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteSupplier_btnActionPerformed(evt);
            }
        });
        jPanel19.add(deleteSupplier_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 480, 90, 30));

        supplierPanel.add(jPanel19, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, 420, 570));

        jPanel20.setBackground(new java.awt.Color(245, 247, 251));
        jPanel20.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        supplier_tbl.setBackground(new java.awt.Color(245, 247, 251));
        supplier_tbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Supplier Name", "Contact Person", "Contact Number", "Email", "Address", "Created At", "Updated At"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        supplier_tbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                supplier_tblMouseClicked(evt);
            }
        });
        jScrollPane5.setViewportView(supplier_tbl);
        if (supplier_tbl.getColumnModel().getColumnCount() > 0) {
            supplier_tbl.getColumnModel().getColumn(0).setResizable(false);
            supplier_tbl.getColumnModel().getColumn(1).setResizable(false);
            supplier_tbl.getColumnModel().getColumn(2).setResizable(false);
            supplier_tbl.getColumnModel().getColumn(3).setResizable(false);
            supplier_tbl.getColumnModel().getColumn(4).setResizable(false);
            supplier_tbl.getColumnModel().getColumn(5).setResizable(false);
            supplier_tbl.getColumnModel().getColumn(6).setResizable(false);
        }

        jPanel20.add(jScrollPane5, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 80, 690, 450));

        jLabel152.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/search (2).png"))); // NOI18N
        jPanel20.add(jLabel152, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 30, 30, 30));

        supplierSearch_txt.setBackground(new java.awt.Color(255, 255, 255));
        supplierSearch_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                supplierSearch_txtKeyReleased(evt);
            }
        });
        jPanel20.add(supplierSearch_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 30, 430, 30));

        jLabel63.setForeground(new java.awt.Color(0, 0, 0));
        jLabel63.setText("Search:");
        jPanel20.add(jLabel63, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 20, 50, 50));

        supplierPanel.add(jPanel20, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 10, 720, 560));

        contentPanel.add(supplierPanel, "suppliers");

        reportsPanel.setBackground(new java.awt.Color(255, 255, 255));
        reportsPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel48.setBackground(new java.awt.Color(228, 228, 228));
        jPanel48.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel49.setBackground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout jPanel49Layout = new javax.swing.GroupLayout(jPanel49);
        jPanel49.setLayout(jPanel49Layout);
        jPanel49Layout.setHorizontalGroup(
            jPanel49Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1110, Short.MAX_VALUE)
        );
        jPanel49Layout.setVerticalGroup(
            jPanel49Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        jPanel48.add(jPanel49, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 50, 1110, 1));

        jLabel104.setForeground(new java.awt.Color(0, 0, 0));
        jLabel104.setText("End Date:");
        jPanel48.add(jLabel104, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 70, 80, 30));

        jPanel50.setBackground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout jPanel50Layout = new javax.swing.GroupLayout(jPanel50);
        jPanel50.setLayout(jPanel50Layout);
        jPanel50Layout.setHorizontalGroup(
            jPanel50Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1110, Short.MAX_VALUE)
        );
        jPanel50Layout.setVerticalGroup(
            jPanel50Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        jPanel48.add(jPanel50, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 110, 1110, 1));

        jPanel51.setBackground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout jPanel51Layout = new javax.swing.GroupLayout(jPanel51);
        jPanel51.setLayout(jPanel51Layout);
        jPanel51Layout.setHorizontalGroup(
            jPanel51Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1110, Short.MAX_VALUE)
        );
        jPanel51Layout.setVerticalGroup(
            jPanel51Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        jPanel48.add(jPanel51, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 170, 1110, 1));

        jLabel110.setForeground(new java.awt.Color(0, 0, 0));
        jLabel110.setText("Report Type:");
        jPanel48.add(jLabel110, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 20, 80, -1));

        jLabel111.setForeground(new java.awt.Color(0, 0, 0));
        jLabel111.setText("Payment Method:");
        jPanel48.add(jLabel111, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 140, 100, -1));

        jLabel114.setForeground(new java.awt.Color(0, 0, 0));
        jLabel114.setText("Start Date:");
        jPanel48.add(jLabel114, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 70, 80, 30));

        jLabel129.setForeground(new java.awt.Color(0, 0, 0));
        jLabel129.setText("Supplier:");
        jPanel48.add(jLabel129, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 130, 80, 30));
        jPanel48.add(endDateReport_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 70, 340, 30));
        jPanel48.add(startDateRepor_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 70, 340, 30));

        payment_cmb.setBackground(new java.awt.Color(255, 255, 255));
        jPanel48.add(payment_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(840, 130, 230, 30));

        categoryReport_cmb.setBackground(new java.awt.Color(255, 255, 255));
        jPanel48.add(categoryReport_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 130, 230, 30));

        supplierReport_cmb.setBackground(new java.awt.Color(255, 255, 255));
        jPanel48.add(supplierReport_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 130, 230, 30));

        exportExel_btn.setBackground(new java.awt.Color(17, 104, 17));
        exportExel_btn.setText("EXPORT ECXEL");
        exportExel_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportExel_btnActionPerformed(evt);
            }
        });
        jPanel48.add(exportExel_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 190, 160, 40));

        generateReport_btn.setBackground(new java.awt.Color(36, 63, 90));
        generateReport_btn.setText("GENERATE REPORT");
        generateReport_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateReport_btnActionPerformed(evt);
            }
        });
        jPanel48.add(generateReport_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 190, 160, 40));

        exportPdf_btn.setBackground(new java.awt.Color(203, 41, 41));
        exportPdf_btn.setText("EXPORT PDF");
        exportPdf_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPdf_btnActionPerformed(evt);
            }
        });
        jPanel48.add(exportPdf_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 190, 160, 40));

        jPanel48.add(ReportType_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 10, 790, 30));

        jLabel130.setForeground(new java.awt.Color(0, 0, 0));
        jLabel130.setText("Category:");
        jPanel48.add(jLabel130, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 126, 80, 30));

        reportsPanel.add(jPanel48, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 10, 1110, 250));

        report_tbl.setBackground(new java.awt.Color(255, 255, 255));
        report_tbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane14.setViewportView(report_tbl);

        reportsPanel.add(jScrollPane14, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 320, 1020, 270));

        jPanel52.setBackground(new java.awt.Color(0, 0, 153));
        jPanel52.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel128.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel128.setForeground(new java.awt.Color(255, 255, 255));
        jLabel128.setText("Report Preview");
        jPanel52.add(jLabel128, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 150, 20));

        reportsPanel.add(jPanel52, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 280, 1020, 40));

        contentPanel.add(reportsPanel, "reports");

        productsPanel.setBackground(new java.awt.Color(255, 255, 255));
        productsPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel21.setBackground(new java.awt.Color(245, 247, 251));
        jPanel21.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        product_tbl.setBackground(new java.awt.Color(238, 242, 255));
        product_tbl.setForeground(new java.awt.Color(17, 24, 39));
        product_tbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Product ID", "Barcode", "Name", "Category", "Supplier", "Cost Price", "Selling Price", "Image", "Stock Quantity", "Reorder Level", "Stock Status", "Created At", "Updated At"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        product_tbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                product_tblMouseClicked(evt);
            }
        });
        jScrollPane6.setViewportView(product_tbl);
        if (product_tbl.getColumnModel().getColumnCount() > 0) {
            product_tbl.getColumnModel().getColumn(0).setResizable(false);
            product_tbl.getColumnModel().getColumn(1).setResizable(false);
            product_tbl.getColumnModel().getColumn(2).setResizable(false);
            product_tbl.getColumnModel().getColumn(3).setResizable(false);
            product_tbl.getColumnModel().getColumn(4).setResizable(false);
            product_tbl.getColumnModel().getColumn(5).setResizable(false);
            product_tbl.getColumnModel().getColumn(6).setResizable(false);
            product_tbl.getColumnModel().getColumn(7).setResizable(false);
            product_tbl.getColumnModel().getColumn(8).setResizable(false);
            product_tbl.getColumnModel().getColumn(9).setResizable(false);
            product_tbl.getColumnModel().getColumn(10).setResizable(false);
            product_tbl.getColumnModel().getColumn(11).setResizable(false);
            product_tbl.getColumnModel().getColumn(12).setResizable(false);
        }

        jPanel21.add(jScrollPane6, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 150, 760, 400));

        jLabel153.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/search (2).png"))); // NOI18N
        jPanel21.add(jLabel153, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 20, 30, 30));

        searchProduct_txt.setBackground(new java.awt.Color(255, 255, 255));
        searchProduct_txt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchProduct_txtActionPerformed(evt);
            }
        });
        searchProduct_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchProduct_txtKeyReleased(evt);
            }
        });
        jPanel21.add(searchProduct_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 20, 510, 30));

        jLabel74.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel74.setForeground(new java.awt.Color(0, 0, 0));
        jLabel74.setText("Search:");
        jPanel21.add(jLabel74, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 60, 50));

        jLabel75.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel75.setForeground(new java.awt.Color(0, 0, 0));
        jLabel75.setText("Status:");
        jPanel21.add(jLabel75, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 60, 50, 30));

        statusProduct_cmb.setBackground(new java.awt.Color(255, 255, 255));
        statusProduct_cmb.setEditable(true);
        statusProduct_cmb.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ALL", "IN STOCK", "LOW STOCK", "OUT OF STOCK" }));
        statusProduct_cmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statusProduct_cmbActionPerformed(evt);
            }
        });
        jPanel21.add(statusProduct_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 60, 190, 30));

        jLabel76.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel76.setForeground(new java.awt.Color(0, 0, 0));
        jLabel76.setText("Category:");
        jPanel21.add(jLabel76, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 60, 60, 30));

        categoryProduct_cmb.setBackground(new java.awt.Color(255, 255, 255));
        categoryProduct_cmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                categoryProduct_cmbActionPerformed(evt);
            }
        });
        jPanel21.add(categoryProduct_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 60, 190, 30));

        jLabel77.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel77.setForeground(new java.awt.Color(0, 0, 0));
        jLabel77.setText("Supplier:");
        jPanel21.add(jLabel77, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 100, 60, 30));

        supplierProduct_cmb.setBackground(new java.awt.Color(255, 255, 255));
        supplierProduct_cmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                supplierProduct_cmbActionPerformed(evt);
            }
        });
        jPanel21.add(supplierProduct_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 100, 190, 30));

        productsPanel.add(jPanel21, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 10, 780, 580));

        jPanel22.setBackground(new java.awt.Color(245, 247, 251));
        jPanel22.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel64.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel64.setForeground(new java.awt.Color(0, 0, 0));
        jLabel64.setText("PRODUCT INFORMATION");
        jPanel22.add(jLabel64, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 230, 20));

        jLabel65.setForeground(new java.awt.Color(0, 0, 0));
        jLabel65.setText("Product Name:");
        jPanel22.add(jLabel65, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 90, 90, 30));

        jLabel66.setForeground(new java.awt.Color(0, 0, 0));
        jLabel66.setText("Barcode:");
        jPanel22.add(jLabel66, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, 70, 30));

        jLabel67.setForeground(new java.awt.Color(0, 0, 0));
        jLabel67.setText("Category:");
        jPanel22.add(jLabel67, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 130, 70, 30));

        category_cmb.setBackground(new java.awt.Color(255, 255, 255));
        jPanel22.add(category_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 130, 250, 30));

        barcode_txt.setBackground(new java.awt.Color(255, 255, 255));
        barcode_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                barcode_txtKeyReleased(evt);
            }
        });
        jPanel22.add(barcode_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 50, 120, 30));

        productName_txt.setBackground(new java.awt.Color(255, 255, 255));
        jPanel22.add(productName_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 90, 250, 30));

        jLabel68.setForeground(new java.awt.Color(0, 0, 0));
        jLabel68.setText("Supplier:");
        jPanel22.add(jLabel68, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 170, 100, 30));

        supplier_cmb.setBackground(new java.awt.Color(255, 255, 255));
        jPanel22.add(supplier_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 170, 250, 30));

        jLabel69.setForeground(new java.awt.Color(0, 0, 0));
        jLabel69.setText("Cost Price:");
        jPanel22.add(jLabel69, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 210, 80, 30));

        costPrice_txt.setBackground(new java.awt.Color(255, 255, 255));
        costPrice_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                costPrice_txtKeyTyped(evt);
            }
        });
        jPanel22.add(costPrice_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 210, 250, 30));

        jLabel70.setForeground(new java.awt.Color(0, 0, 0));
        jLabel70.setText("Selling Price:");
        jPanel22.add(jLabel70, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 250, 80, 30));

        sellingPrice_txt.setBackground(new java.awt.Color(255, 255, 255));
        sellingPrice_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                sellingPrice_txtKeyTyped(evt);
            }
        });
        jPanel22.add(sellingPrice_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 250, 250, 30));

        jLabel71.setForeground(new java.awt.Color(0, 0, 0));
        jLabel71.setText("Stock Quantity:");
        jPanel22.add(jLabel71, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 330, 100, 30));

        stockquantity_txt.setBackground(new java.awt.Color(255, 255, 255));
        stockquantity_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                stockquantity_txtKeyTyped(evt);
            }
        });
        jPanel22.add(stockquantity_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 330, 250, 30));

        jLabel72.setForeground(new java.awt.Color(0, 0, 0));
        jLabel72.setText("Reorder Level:");
        jPanel22.add(jLabel72, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 290, 100, 30));

        reorderLevel_txt.setBackground(new java.awt.Color(255, 255, 255));
        reorderLevel_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                reorderLevel_txtKeyTyped(evt);
            }
        });
        jPanel22.add(reorderLevel_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 290, 250, 30));

        jLabel73.setForeground(new java.awt.Color(0, 0, 0));
        jLabel73.setText("Image Preview");
        jPanel22.add(jLabel73, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 370, 90, 20));

        clearProduct_btn.setBackground(new java.awt.Color(154, 154, 147));
        clearProduct_btn.setText("CLEAR");
        clearProduct_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearProduct_btnActionPerformed(evt);
            }
        });
        jPanel22.add(clearProduct_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 540, 90, 30));

        addProduct_btn.setBackground(new java.awt.Color(6, 75, 9));
        addProduct_btn.setText("ADD");
        addProduct_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addProduct_btnActionPerformed(evt);
            }
        });
        jPanel22.add(addProduct_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 420, 90, 30));

        updateProduct_btn.setBackground(new java.awt.Color(5, 59, 114));
        updateProduct_btn.setText("UPDATE");
        updateProduct_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateProduct_btnActionPerformed(evt);
            }
        });
        jPanel22.add(updateProduct_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 460, 90, 30));

        deleteProduct_btn.setBackground(new java.awt.Color(128, 0, 16));
        deleteProduct_btn.setText("DELETE");
        deleteProduct_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteProduct_btnActionPerformed(evt);
            }
        });
        jPanel22.add(deleteProduct_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 500, 90, 30));

        jPanel23.setBackground(new java.awt.Color(204, 204, 204));
        jPanel23.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        removeImage_btn.setBackground(new java.awt.Color(153, 0, 0));
        removeImage_btn.setText("Remove Img");
        removeImage_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeImage_btnActionPerformed(evt);
            }
        });
        jPanel23.add(removeImage_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 160, 120, 30));

        chooseImage_btn.setBackground(new java.awt.Color(51, 153, 0));
        chooseImage_btn.setText("Choose Img");
        chooseImage_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooseImage_btnActionPerformed(evt);
            }
        });
        jPanel23.add(chooseImage_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 160, 120, 30));

        Image_txt.setForeground(new java.awt.Color(0, 0, 0));
        jPanel23.add(Image_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 240, 160));

        jPanel22.add(jPanel23, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 390, 240, 190));

        autoGenerateBarcode_rbt.setForeground(new java.awt.Color(0, 0, 0));
        autoGenerateBarcode_rbt.setText("auto-generate");
        autoGenerateBarcode_rbt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoGenerateBarcode_rbtActionPerformed(evt);
            }
        });
        jPanel22.add(autoGenerateBarcode_rbt, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 50, 100, 30));

        productsPanel.add(jPanel22, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 380, 580));

        contentPanel.add(productsPanel, "products");

        inventoryPanel.setBackground(new java.awt.Color(255, 255, 255));
        inventoryPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel78.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel78.setForeground(new java.awt.Color(0, 0, 0));
        jLabel78.setText("STOCK TRANSACTION DETAILS");
        inventoryPanel.add(jLabel78, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 270, -1));

        jPanel24.setBackground(new java.awt.Color(204, 204, 204));

        javax.swing.GroupLayout jPanel24Layout = new javax.swing.GroupLayout(jPanel24);
        jPanel24.setLayout(jPanel24Layout);
        jPanel24Layout.setHorizontalGroup(
            jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1160, Short.MAX_VALUE)
        );
        jPanel24Layout.setVerticalGroup(
            jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        inventoryPanel.add(jPanel24, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, 1160, 1));

        InventoryContentPanel.setBackground(new java.awt.Color(204, 204, 204));
        InventoryContentPanel.setForeground(new java.awt.Color(204, 204, 255));
        InventoryContentPanel.setLayout(new java.awt.CardLayout());

        StockIN.setBackground(new java.awt.Color(204, 204, 204));
        StockIN.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel79.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel79.setForeground(new java.awt.Color(0, 0, 0));
        jLabel79.setText("Remarks");
        StockIN.add(jLabel79, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 290, 70, -1));

        jPanel25.setBackground(new java.awt.Color(153, 153, 153));
        jPanel25.setForeground(new java.awt.Color(204, 204, 255));

        javax.swing.GroupLayout jPanel25Layout = new javax.swing.GroupLayout(jPanel25);
        jPanel25.setLayout(jPanel25Layout);
        jPanel25Layout.setHorizontalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jPanel25Layout.setVerticalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        StockIN.add(jPanel25, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 35, 400, 1));

        jLabel82.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel82.setForeground(new java.awt.Color(0, 0, 0));
        jLabel82.setText("Barcode");
        StockIN.add(jLabel82, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 90, 60, 30));

        jLabel83.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel83.setForeground(new java.awt.Color(0, 0, 0));
        jLabel83.setText("Quantity");
        StockIN.add(jLabel83, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 170, 60, 30));

        jLabel84.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel84.setForeground(new java.awt.Color(0, 0, 0));
        jLabel84.setText("Ref No.");
        StockIN.add(jLabel84, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 210, 50, 30));

        jLabel85.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel85.setForeground(new java.awt.Color(0, 0, 0));
        jLabel85.setText("Date");
        StockIN.add(jLabel85, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 250, 60, 30));

        productStockIN_cmb.setBackground(new java.awt.Color(255, 255, 255));
        productStockIN_cmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                productStockIN_cmbActionPerformed(evt);
            }
        });
        StockIN.add(productStockIN_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 50, 150, 30));

        ReferenceNo_txt.setBackground(new java.awt.Color(255, 255, 255));
        StockIN.add(ReferenceNo_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 210, 150, 30));

        supplierIN_btn.setBackground(new java.awt.Color(255, 255, 255));
        StockIN.add(supplierIN_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 130, 150, 30));

        jDateChooser1.setBackground(new java.awt.Color(255, 255, 255));
        StockIN.add(jDateChooser1, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 250, 150, 30));

        remarksIn_txt.setBackground(new java.awt.Color(255, 255, 255));
        remarksIn_txt.setColumns(20);
        remarksIn_txt.setRows(5);
        jScrollPane7.setViewportView(remarksIn_txt);

        StockIN.add(jScrollPane7, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 320, 270, 120));

        jLabel86.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel86.setForeground(new java.awt.Color(0, 0, 0));
        jLabel86.setText("Supplier");
        StockIN.add(jLabel86, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 130, 60, 30));

        quantityIn_txt.setBackground(new java.awt.Color(255, 255, 255));
        quantityIn_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                quantityIn_txtKeyTyped(evt);
            }
        });
        StockIN.add(quantityIn_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 170, 150, 30));

        jPanel27.setBackground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout jPanel27Layout = new javax.swing.GroupLayout(jPanel27);
        jPanel27.setLayout(jPanel27Layout);
        jPanel27Layout.setHorizontalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );
        jPanel27Layout.setVerticalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 230, Short.MAX_VALUE)
        );

        StockIN.add(jPanel27, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 50, 1, 230));

        jPanel28.setBackground(new java.awt.Color(153, 153, 153));
        jPanel28.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jTextField5.setEditable(false);
        jTextField5.setBackground(new java.awt.Color(204, 204, 204));
        jTextField5.setForeground(new java.awt.Color(0, 0, 0));
        jTextField5.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField5.setText("Product Preview");
        jPanel28.add(jTextField5, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 160, -1));
        jPanel28.add(ImageIn_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 20, 160, 150));

        jLabel88.setForeground(new java.awt.Color(0, 0, 0));
        jLabel88.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel88.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/barcode.png"))); // NOI18N
        jPanel28.add(jLabel88, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 170, 20, -1));

        jLabel89.setForeground(new java.awt.Color(0, 0, 0));
        jLabel89.setText("Current Stock ");
        jPanel28.add(jLabel89, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 200, -1, 20));

        currentStock_lbl.setForeground(new java.awt.Color(0, 0, 0));
        jPanel28.add(currentStock_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 200, -1, 20));

        barcodeinGen_lbl.setForeground(new java.awt.Color(0, 0, 0));
        jPanel28.add(barcodeinGen_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 170, 120, 20));

        StockIN.add(jPanel28, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 50, 160, 220));

        statusIdentifier_txt.setBackground(new java.awt.Color(51, 204, 0));
        statusIdentifier_txt.setForeground(new java.awt.Color(255, 255, 255));
        statusIdentifier_txt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        StockIN.add(statusIdentifier_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 270, 160, 40));

        barcodeIN_txt.setBackground(new java.awt.Color(255, 255, 255));
        StockIN.add(barcodeIN_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 90, 150, 30));

        jLabel105.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel105.setForeground(new java.awt.Color(0, 0, 0));
        jLabel105.setText("Product");
        StockIN.add(jLabel105, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, 90, 30));

        jLabel107.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel107.setForeground(new java.awt.Color(0, 0, 0));
        jLabel107.setText("STOCK IN DETAILS");
        StockIN.add(jLabel107, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 160, -1));

        InventoryContentPanel.add(StockIN, "StockIN");

        StockOut.setBackground(new java.awt.Color(204, 204, 204));
        StockOut.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel92.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel92.setForeground(new java.awt.Color(0, 0, 0));
        jLabel92.setText("Quantity");
        StockOut.add(jLabel92, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 130, 60, 30));

        jLabel93.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel93.setForeground(new java.awt.Color(0, 0, 0));
        jLabel93.setText("Remarks");
        StockOut.add(jLabel93, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 290, 70, -1));

        ReferenceNoOUT_txt.setBackground(new java.awt.Color(255, 255, 255));
        StockOut.add(ReferenceNoOUT_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 210, 150, 30));

        jLabel94.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel94.setForeground(new java.awt.Color(0, 0, 0));
        jLabel94.setText("Date");
        StockOut.add(jLabel94, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 250, 60, 30));

        jLabel95.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel95.setForeground(new java.awt.Color(0, 0, 0));
        jLabel95.setText("Barcode");
        StockOut.add(jLabel95, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 90, 60, 30));

        reason_cmb.setBackground(new java.awt.Color(255, 255, 255));
        reason_cmb.setEditable(true);
        reason_cmb.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Damage", "Expired", "Adjustment" }));
        StockOut.add(reason_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 170, 150, 30));

        jLabel96.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel96.setForeground(new java.awt.Color(0, 0, 0));
        jLabel96.setText("Ref No.");
        StockOut.add(jLabel96, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 210, 50, 30));

        jPanel30.setBackground(new java.awt.Color(153, 153, 153));
        jPanel30.setForeground(new java.awt.Color(204, 204, 255));

        javax.swing.GroupLayout jPanel30Layout = new javax.swing.GroupLayout(jPanel30);
        jPanel30.setLayout(jPanel30Layout);
        jPanel30Layout.setHorizontalGroup(
            jPanel30Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jPanel30Layout.setVerticalGroup(
            jPanel30Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        StockOut.add(jPanel30, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 35, 400, 1));

        jLabel97.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel97.setForeground(new java.awt.Color(0, 0, 0));
        jLabel97.setText("Product");
        StockOut.add(jLabel97, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, 90, 30));

        jLabel98.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel98.setForeground(new java.awt.Color(0, 0, 0));
        jLabel98.setText("STOCK OUT DETAILS");
        StockOut.add(jLabel98, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 190, -1));

        jDateChooser2.setBackground(new java.awt.Color(255, 255, 255));
        StockOut.add(jDateChooser2, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 250, 150, 30));

        remarksOUT_txt.setBackground(new java.awt.Color(255, 255, 255));
        remarksOUT_txt.setColumns(20);
        remarksOUT_txt.setRows(5);
        jScrollPane9.setViewportView(remarksOUT_txt);

        StockOut.add(jScrollPane9, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 320, 270, 120));

        jLabel99.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel99.setForeground(new java.awt.Color(0, 0, 0));
        jLabel99.setText("Reason");
        StockOut.add(jLabel99, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 170, 60, 30));

        quantityOUT_txt.setBackground(new java.awt.Color(255, 255, 255));
        quantityOUT_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                quantityOUT_txtKeyTyped(evt);
            }
        });
        StockOut.add(quantityOUT_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 130, 150, 30));

        jPanel31.setBackground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout jPanel31Layout = new javax.swing.GroupLayout(jPanel31);
        jPanel31.setLayout(jPanel31Layout);
        jPanel31Layout.setHorizontalGroup(
            jPanel31Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );
        jPanel31Layout.setVerticalGroup(
            jPanel31Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 230, Short.MAX_VALUE)
        );

        StockOut.add(jPanel31, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 50, 1, 230));

        jPanel32.setBackground(new java.awt.Color(153, 153, 153));
        jPanel32.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jTextField11.setEditable(false);
        jTextField11.setBackground(new java.awt.Color(204, 204, 204));
        jTextField11.setForeground(new java.awt.Color(0, 0, 0));
        jTextField11.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField11.setText("Product Preview");
        jPanel32.add(jTextField11, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 160, -1));
        jPanel32.add(ImageOUT_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 20, 160, 150));

        jLabel101.setForeground(new java.awt.Color(0, 0, 0));
        jLabel101.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel101.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/barcode.png"))); // NOI18N
        jPanel32.add(jLabel101, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 170, 20, -1));

        jLabel102.setForeground(new java.awt.Color(0, 0, 0));
        jLabel102.setText("Current Stock ");
        jPanel32.add(jLabel102, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 200, -1, 20));

        currentStockOUT_lbl.setForeground(new java.awt.Color(0, 0, 0));
        jPanel32.add(currentStockOUT_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 200, -1, 20));

        barcodeOUTGen_lbl.setForeground(new java.awt.Color(0, 0, 0));
        jPanel32.add(barcodeOUTGen_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 170, 120, 20));

        StockOut.add(jPanel32, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 50, 160, 220));

        statusIdentifierOUT_txt.setBackground(new java.awt.Color(51, 204, 0));
        statusIdentifierOUT_txt.setForeground(new java.awt.Color(255, 255, 255));
        statusIdentifierOUT_txt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        StockOut.add(statusIdentifierOUT_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 270, 160, 40));

        barcodeOUT_txt.setBackground(new java.awt.Color(255, 255, 255));
        StockOut.add(barcodeOUT_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 90, 150, 30));

        ProductOut_cmb.setBackground(new java.awt.Color(255, 255, 255));
        ProductOut_cmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ProductOut_cmbActionPerformed(evt);
            }
        });
        StockOut.add(ProductOut_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 50, 150, 30));

        InventoryContentPanel.add(StockOut, "StockOut");

        StockMovement.setBackground(new java.awt.Color(204, 204, 204));
        StockMovement.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel35.setBackground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout jPanel35Layout = new javax.swing.GroupLayout(jPanel35);
        jPanel35.setLayout(jPanel35Layout);
        jPanel35Layout.setHorizontalGroup(
            jPanel35Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jPanel35Layout.setVerticalGroup(
            jPanel35Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        StockMovement.add(jPanel35, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, 400, 1));

        jLabel115.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel115.setForeground(new java.awt.Color(0, 0, 0));
        jLabel115.setText("STOCK MOVEMENT");
        StockMovement.add(jLabel115, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 290, -1));

        jLabel112.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel112.setForeground(new java.awt.Color(0, 0, 0));
        jLabel112.setText("Status:");
        StockMovement.add(jLabel112, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 340, 60, 30));

        jLabel117.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel117.setForeground(new java.awt.Color(0, 0, 0));
        jLabel117.setText("Product:");
        StockMovement.add(jLabel117, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, 60, 30));

        jLabel118.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel118.setForeground(new java.awt.Color(0, 0, 0));
        jLabel118.setText("Category:");
        StockMovement.add(jLabel118, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 280, 60, 30));

        jLabel119.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel119.setForeground(new java.awt.Color(0, 0, 0));
        jLabel119.setText("Supplier:");
        StockMovement.add(jLabel119, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 310, 60, 30));

        jLabel120.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel120.setForeground(new java.awt.Color(0, 0, 0));
        jLabel120.setText("Stock Qty:");
        StockMovement.add(jLabel120, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 400, 80, 30));

        jLabel121.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel121.setForeground(new java.awt.Color(0, 0, 0));
        jLabel121.setText("Reorder Level:");
        StockMovement.add(jLabel121, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 370, 90, 30));

        jPanel37.setBackground(new java.awt.Color(153, 153, 153));
        jPanel37.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        jPanel37.add(ImageMovementDisplay_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 250, 190));

        StockMovement.add(jPanel37, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 90, 250, 190));

        StockQTYMovement_lbl.setForeground(new java.awt.Color(0, 0, 0));
        StockMovement.add(StockQTYMovement_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 410, 250, 10));

        ProductMovement_lbl.setForeground(new java.awt.Color(0, 0, 0));
        StockMovement.add(ProductMovement_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 50, 250, 30));

        categoryMovement_lbl.setForeground(new java.awt.Color(0, 0, 0));
        StockMovement.add(categoryMovement_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 290, 250, 10));

        supplierMovement_lbl.setForeground(new java.awt.Color(0, 0, 0));
        StockMovement.add(supplierMovement_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 320, 250, 10));

        statusMovement_lbl.setForeground(new java.awt.Color(0, 0, 0));
        StockMovement.add(statusMovement_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 350, 250, 10));

        reorderLevel_lbl.setForeground(new java.awt.Color(0, 0, 0));
        StockMovement.add(reorderLevel_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 380, 250, 10));

        InventoryContentPanel.add(StockMovement, "StockMovement");

        inventoryPanel.add(InventoryContentPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 90, 420, 450));

        jtablePanel.setBackground(new java.awt.Color(204, 204, 204));
        jtablePanel.setLayout(new java.awt.CardLayout());

        StockinJ.setBackground(new java.awt.Color(204, 204, 204));
        StockinJ.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        StockIn_tbl.setBackground(new java.awt.Color(255, 255, 255));
        StockIn_tbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Product", "User", "Type", "Qty", "Ref No", "Remarks"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        StockIn_tbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                StockIn_tblMouseClicked(evt);
            }
        });
        jScrollPane8.setViewportView(StockIn_tbl);
        if (StockIn_tbl.getColumnModel().getColumnCount() > 0) {
            StockIn_tbl.getColumnModel().getColumn(0).setResizable(false);
            StockIn_tbl.getColumnModel().getColumn(1).setResizable(false);
            StockIn_tbl.getColumnModel().getColumn(2).setResizable(false);
            StockIn_tbl.getColumnModel().getColumn(3).setResizable(false);
            StockIn_tbl.getColumnModel().getColumn(4).setResizable(false);
            StockIn_tbl.getColumnModel().getColumn(5).setResizable(false);
            StockIn_tbl.getColumnModel().getColumn(6).setResizable(false);
        }

        StockinJ.add(jScrollPane8, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 107, 720, 380));

        jLabel81.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel81.setForeground(new java.awt.Color(0, 0, 0));
        jLabel81.setText("Type:");
        StockinJ.add(jLabel81, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 70, 50, 30));

        jLabel154.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/search (2).png"))); // NOI18N
        StockinJ.add(jLabel154, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 70, 30, 30));

        searchIN_txt.setBackground(new java.awt.Color(255, 255, 255));
        searchIN_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchIN_txtKeyReleased(evt);
            }
        });
        StockinJ.add(searchIN_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 70, 350, 30));

        jLabel106.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel106.setForeground(new java.awt.Color(0, 0, 0));
        jLabel106.setText("Search:");
        StockinJ.add(jLabel106, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 70, 50, 30));

        typeIn_cmb.setBackground(new java.awt.Color(255, 255, 255));
        typeIn_cmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeIn_cmbActionPerformed(evt);
            }
        });
        StockinJ.add(typeIn_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 70, 220, 30));

        jLabel80.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel80.setForeground(new java.awt.Color(0, 0, 0));
        jLabel80.setText("RECENT STOCK IN TRANSACTIONS");
        StockinJ.add(jLabel80, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 290, -1));

        jPanel26.setBackground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout jPanel26Layout = new javax.swing.GroupLayout(jPanel26);
        jPanel26.setLayout(jPanel26Layout);
        jPanel26Layout.setHorizontalGroup(
            jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 720, Short.MAX_VALUE)
        );
        jPanel26Layout.setVerticalGroup(
            jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        StockinJ.add(jPanel26, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, 720, 1));

        jtablePanel.add(StockinJ, "StockinJ");

        stockOutJ.setBackground(new java.awt.Color(204, 204, 204));
        stockOutJ.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel108.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel108.setForeground(new java.awt.Color(0, 0, 0));
        jLabel108.setText("Search:");
        stockOutJ.add(jLabel108, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 70, 50, 30));

        jLabel109.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel109.setForeground(new java.awt.Color(0, 0, 0));
        jLabel109.setText("RECENT STOCK OUT TRANSACTIONS");
        stockOutJ.add(jLabel109, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 310, -1));

        jLabel155.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/search (2).png"))); // NOI18N
        stockOutJ.add(jLabel155, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 70, 30, 30));

        searchOUT_txt.setBackground(new java.awt.Color(255, 255, 255));
        searchOUT_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchOUT_txtKeyReleased(evt);
            }
        });
        stockOutJ.add(searchOUT_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 70, 350, 30));

        StockOUT_tbl.setBackground(new java.awt.Color(255, 255, 255));
        StockOUT_tbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Product", "User", "Qty", "Reference Number", "Date", "Reason", "Remarks"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        StockOUT_tbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                StockOUT_tblMouseClicked(evt);
            }
        });
        jScrollPane10.setViewportView(StockOUT_tbl);
        if (StockOUT_tbl.getColumnModel().getColumnCount() > 0) {
            StockOUT_tbl.getColumnModel().getColumn(0).setResizable(false);
            StockOUT_tbl.getColumnModel().getColumn(1).setResizable(false);
            StockOUT_tbl.getColumnModel().getColumn(2).setResizable(false);
            StockOUT_tbl.getColumnModel().getColumn(3).setResizable(false);
            StockOUT_tbl.getColumnModel().getColumn(4).setResizable(false);
            StockOUT_tbl.getColumnModel().getColumn(5).setResizable(false);
            StockOUT_tbl.getColumnModel().getColumn(6).setResizable(false);
            StockOUT_tbl.getColumnModel().getColumn(7).setResizable(false);
        }

        stockOutJ.add(jScrollPane10, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 107, 720, 380));

        jPanel34.setBackground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout jPanel34Layout = new javax.swing.GroupLayout(jPanel34);
        jPanel34.setLayout(jPanel34Layout);
        jPanel34Layout.setHorizontalGroup(
            jPanel34Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 720, Short.MAX_VALUE)
        );
        jPanel34Layout.setVerticalGroup(
            jPanel34Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        stockOutJ.add(jPanel34, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, 720, 1));

        jtablePanel.add(stockOutJ, "StockOutJ");

        StockMovementJ.setBackground(new java.awt.Color(204, 204, 204));
        StockMovementJ.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        stockMovement_tbl.setBackground(new java.awt.Color(255, 255, 255));
        stockMovement_tbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Product", "Category", "Supplier", "Stock Qty", "Reorder", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        stockMovement_tbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                stockMovement_tblMouseClicked(evt);
            }
        });
        jScrollPane11.setViewportView(stockMovement_tbl);
        if (stockMovement_tbl.getColumnModel().getColumnCount() > 0) {
            stockMovement_tbl.getColumnModel().getColumn(0).setResizable(false);
            stockMovement_tbl.getColumnModel().getColumn(1).setResizable(false);
            stockMovement_tbl.getColumnModel().getColumn(2).setResizable(false);
            stockMovement_tbl.getColumnModel().getColumn(3).setResizable(false);
            stockMovement_tbl.getColumnModel().getColumn(4).setResizable(false);
            stockMovement_tbl.getColumnModel().getColumn(5).setResizable(false);
        }

        StockMovementJ.add(jScrollPane11, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 107, 720, 380));

        jLabel156.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pointofsale.ICONS/search (2).png"))); // NOI18N
        StockMovementJ.add(jLabel156, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 60, 30, 30));

        stockMovementSearch_txt.setBackground(new java.awt.Color(255, 255, 255));
        stockMovementSearch_txt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                stockMovementSearch_txtKeyReleased(evt);
            }
        });
        StockMovementJ.add(stockMovementSearch_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 60, 350, 30));

        jLabel113.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel113.setForeground(new java.awt.Color(0, 0, 0));
        jLabel113.setText("STOCK MOVEMENT");
        StockMovementJ.add(jLabel113, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 290, -1));

        jPanel36.setBackground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout jPanel36Layout = new javax.swing.GroupLayout(jPanel36);
        jPanel36.setLayout(jPanel36Layout);
        jPanel36Layout.setHorizontalGroup(
            jPanel36Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 720, Short.MAX_VALUE)
        );
        jPanel36Layout.setVerticalGroup(
            jPanel36Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        StockMovementJ.add(jPanel36, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, 720, 1));

        StockMovementJ.add(stockMovementType_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 60, 190, -1));

        jLabel116.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel116.setForeground(new java.awt.Color(0, 0, 0));
        jLabel116.setText("Stock:");
        StockMovementJ.add(jLabel116, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 60, 50, 30));

        jLabel158.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel158.setForeground(new java.awt.Color(0, 0, 0));
        jLabel158.setText("Search:");
        StockMovementJ.add(jLabel158, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 60, 50, 30));

        stocksType_cmb.setBackground(new java.awt.Color(255, 255, 255));
        stocksType_cmb.setForeground(new java.awt.Color(0, 0, 0));
        stocksType_cmb.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "Low Stock", "Out Of Stock", "In Stock" }));
        StockMovementJ.add(stocksType_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 60, 190, 30));

        jtablePanel.add(StockMovementJ, "StockMovementJ");

        inventoryPanel.add(jtablePanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 90, 740, 500));

        stockmovement_btn.setBackground(new java.awt.Color(0, 0, 204));
        stockmovement_btn.setForeground(new java.awt.Color(255, 255, 255));
        stockmovement_btn.setText("STOCK MONITORING");
        stockmovement_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stockmovement_btnActionPerformed(evt);
            }
        });
        inventoryPanel.add(stockmovement_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 50, 190, 30));

        clearInv_btn.setBackground(new java.awt.Color(153, 153, 153));
        clearInv_btn.setForeground(new java.awt.Color(255, 255, 255));
        clearInv_btn.setText("Clear");
        clearInv_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearInv_btnActionPerformed(evt);
            }
        });
        inventoryPanel.add(clearInv_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 550, 100, 30));

        stockout_btn.setBackground(new java.awt.Color(255, 102, 0));
        stockout_btn.setForeground(new java.awt.Color(255, 255, 255));
        stockout_btn.setText("STOCK OUT");
        stockout_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stockout_btnActionPerformed(evt);
            }
        });
        inventoryPanel.add(stockout_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 50, 190, 30));

        stockin_btn.setBackground(new java.awt.Color(51, 153, 0));
        stockin_btn.setForeground(new java.awt.Color(255, 255, 255));
        stockin_btn.setText("STOCK IN");
        stockin_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stockin_btnActionPerformed(evt);
            }
        });
        inventoryPanel.add(stockin_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 50, 190, 30));

        saveInventory_btn.setBackground(new java.awt.Color(51, 153, 0));
        saveInventory_btn.setForeground(new java.awt.Color(255, 255, 255));
        saveInventory_btn.setText("Save");
        saveInventory_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveInventory_btnActionPerformed(evt);
            }
        });
        inventoryPanel.add(saveInventory_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 550, 200, 30));

        contentPanel.add(inventoryPanel, "inventory");

        salesHistoryPanel.setBackground(new java.awt.Color(255, 255, 255));
        salesHistoryPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        Header.setBackground(new java.awt.Color(228, 228, 228));
        Header.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel87.setForeground(new java.awt.Color(0, 0, 0));
        jLabel87.setText("Invoice No:");
        Header.add(jLabel87, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 60, 70, 30));

        jLabel90.setForeground(new java.awt.Color(0, 0, 0));
        jLabel90.setText("Cashier");
        Header.add(jLabel90, new org.netbeans.lib.awtextra.AbsoluteConstraints(810, 10, 50, 30));

        InvoiceNoSalesHistory_txt.setBackground(new java.awt.Color(255, 255, 255));
        Header.add(InvoiceNoSalesHistory_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 60, 260, 30));

        jLabel91.setForeground(new java.awt.Color(0, 0, 0));
        jLabel91.setText("Start Date:");
        Header.add(jLabel91, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 6, 70, 40));

        jLabel100.setForeground(new java.awt.Color(0, 0, 0));
        jLabel100.setText("End Date");
        Header.add(jLabel100, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 10, 60, 30));

        cashierSalesHistory_cmb.setBackground(new java.awt.Color(255, 255, 255));
        cashierSalesHistory_cmb.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        Header.add(cashierSalesHistory_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(860, 10, 200, 30));

        jLabel103.setForeground(new java.awt.Color(0, 0, 0));
        jLabel103.setText("Payment Method:");
        Header.add(jLabel103, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 60, 110, 30));

        paymentMethodSAlesHistory_cmb.setBackground(new java.awt.Color(255, 255, 255));
        paymentMethodSAlesHistory_cmb.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        Header.add(paymentMethodSAlesHistory_cmb, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 60, 250, 30));

        clearsalesHistory_btn.setBackground(new java.awt.Color(18, 72, 126));
        clearsalesHistory_btn.setForeground(new java.awt.Color(255, 255, 255));
        clearsalesHistory_btn.setText("CLEAR");
        clearsalesHistory_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearsalesHistory_btnActionPerformed(evt);
            }
        });
        Header.add(clearsalesHistory_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(1040, 60, 120, 30));

        searchSalesHistory_btn.setBackground(new java.awt.Color(55, 146, 48));
        searchSalesHistory_btn.setForeground(new java.awt.Color(255, 255, 255));
        searchSalesHistory_btn.setText("SEARCH");
        searchSalesHistory_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchSalesHistory_btnActionPerformed(evt);
            }
        });
        Header.add(searchSalesHistory_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(910, 60, 120, 30));

        jPanel33.setBackground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout jPanel33Layout = new javax.swing.GroupLayout(jPanel33);
        jPanel33.setLayout(jPanel33Layout);
        jPanel33Layout.setHorizontalGroup(
            jPanel33Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1190, Short.MAX_VALUE)
        );
        jPanel33Layout.setVerticalGroup(
            jPanel33Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        Header.add(jPanel33, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 50, 1190, 1));

        StartDateSalesHistory_txt.setBackground(new java.awt.Color(255, 255, 255));
        StartDateSalesHistory_txt.setForeground(new java.awt.Color(0, 0, 0));
        Header.add(StartDateSalesHistory_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 10, 260, 30));

        endDateSalesHistory_txt.setBackground(new java.awt.Color(255, 255, 255));
        endDateSalesHistory_txt.setForeground(new java.awt.Color(0, 0, 0));
        Header.add(endDateSalesHistory_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 10, 250, 30));

        salesHistoryPanel.add(Header, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1190, 100));

        jPanel38.setBackground(new java.awt.Color(28, 122, 28));
        jPanel38.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel42.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel42Layout = new javax.swing.GroupLayout(jPanel42);
        jPanel42.setLayout(jPanel42Layout);
        jPanel42Layout.setHorizontalGroup(
            jPanel42Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 230, Short.MAX_VALUE)
        );
        jPanel42Layout.setVerticalGroup(
            jPanel42Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        jPanel38.add(jPanel42, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 50, 230, 1));

        totalSales_lbl.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        totalSales_lbl.setForeground(new java.awt.Color(255, 255, 255));
        totalSales_lbl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel38.add(totalSales_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 60, 230, 30));

        jLabel126.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel126.setForeground(new java.awt.Color(255, 255, 255));
        jLabel126.setText("TOTAL SALES");
        jPanel38.add(jLabel126, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, -1, 30));

        salesHistoryPanel.add(jPanel38, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 120, 230, 100));

        jPanel39.setBackground(new java.awt.Color(1, 55, 91));
        jPanel39.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel44.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel44Layout = new javax.swing.GroupLayout(jPanel44);
        jPanel44.setLayout(jPanel44Layout);
        jPanel44Layout.setHorizontalGroup(
            jPanel44Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 230, Short.MAX_VALUE)
        );
        jPanel44Layout.setVerticalGroup(
            jPanel44Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        jPanel39.add(jPanel44, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 50, 230, 1));

        itemSold_lbl.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        itemSold_lbl.setForeground(new java.awt.Color(255, 255, 255));
        itemSold_lbl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel39.add(itemSold_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 60, 230, 30));

        jLabel124.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel124.setForeground(new java.awt.Color(255, 255, 255));
        jLabel124.setText("ITEMS SOLD");
        jPanel39.add(jLabel124, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, -1, 30));

        salesHistoryPanel.add(jPanel39, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 120, 230, 100));

        jPanel40.setBackground(new java.awt.Color(156, 75, 16));
        jPanel40.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel43.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel43Layout = new javax.swing.GroupLayout(jPanel43);
        jPanel43.setLayout(jPanel43Layout);
        jPanel43Layout.setHorizontalGroup(
            jPanel43Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 230, Short.MAX_VALUE)
        );
        jPanel43Layout.setVerticalGroup(
            jPanel43Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        jPanel40.add(jPanel43, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 50, 230, 1));

        transactionHsitory_lbl.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        transactionHsitory_lbl.setForeground(new java.awt.Color(255, 255, 255));
        transactionHsitory_lbl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel40.add(transactionHsitory_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 60, 230, 30));

        jLabel123.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel123.setForeground(new java.awt.Color(255, 255, 255));
        jLabel123.setText("TRANSACTIONS");
        jPanel40.add(jLabel123, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 10, -1, 30));

        salesHistoryPanel.add(jPanel40, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 120, 230, 100));

        jPanel41.setBackground(new java.awt.Color(69, 7, 111));
        jPanel41.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel45.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel45Layout = new javax.swing.GroupLayout(jPanel45);
        jPanel45.setLayout(jPanel45Layout);
        jPanel45Layout.setHorizontalGroup(
            jPanel45Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 230, Short.MAX_VALUE)
        );
        jPanel45Layout.setVerticalGroup(
            jPanel45Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1, Short.MAX_VALUE)
        );

        jPanel41.add(jPanel45, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 50, 230, 1));

        totalDiscount_lbl.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        totalDiscount_lbl.setForeground(new java.awt.Color(255, 255, 255));
        totalDiscount_lbl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel41.add(totalDiscount_lbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 60, 230, 30));

        jLabel125.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel125.setForeground(new java.awt.Color(255, 255, 255));
        jLabel125.setText("TOTAL DISCOUNT");
        jPanel41.add(jLabel125, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 10, -1, 30));

        salesHistoryPanel.add(jPanel41, new org.netbeans.lib.awtextra.AbsoluteConstraints(910, 120, 230, 100));

        salesList_tbl.setBackground(new java.awt.Color(255, 255, 255));
        salesList_tbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Sales ID", "Invoice", "Date", "Cashier", "Subtotal", "VAT", "Discount", "Total", "Payment", "Change"
            }
        ));
        salesList_tbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                salesList_tblMouseClicked(evt);
            }
        });
        jScrollPane12.setViewportView(salesList_tbl);

        salesHistoryPanel.add(jScrollPane12, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 290, 580, 290));

        selectedSaleItems_tbl.setBackground(new java.awt.Color(255, 255, 255));
        selectedSaleItems_tbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Product", "Qty", "Unit Price", "Discount", "Total Price"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane13.setViewportView(selectedSaleItems_tbl);
        if (selectedSaleItems_tbl.getColumnModel().getColumnCount() > 0) {
            selectedSaleItems_tbl.getColumnModel().getColumn(0).setResizable(false);
            selectedSaleItems_tbl.getColumnModel().getColumn(1).setResizable(false);
            selectedSaleItems_tbl.getColumnModel().getColumn(2).setResizable(false);
            selectedSaleItems_tbl.getColumnModel().getColumn(3).setResizable(false);
            selectedSaleItems_tbl.getColumnModel().getColumn(4).setResizable(false);
        }

        salesHistoryPanel.add(jScrollPane13, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 290, 580, 290));

        jPanel46.setBackground(new java.awt.Color(26, 112, 169));
        jPanel46.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel127.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel127.setForeground(new java.awt.Color(255, 255, 255));
        jLabel127.setText("SALES LIST");
        jPanel46.add(jLabel127, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 90, 30));

        salesHistoryPanel.add(jPanel46, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 260, 580, 30));

        jPanel47.setBackground(new java.awt.Color(26, 112, 169));
        jPanel47.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel122.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel122.setForeground(new java.awt.Color(255, 255, 255));
        jLabel122.setText("SELECTED SALE ITEMS");
        jPanel47.add(jLabel122, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 220, 30));

        salesHistoryPanel.add(jPanel47, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 260, 580, 30));

        contentPanel.add(salesHistoryPanel, "salesHistory");

        MainPanel.add(contentPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 130, 1190, 600));

        getContentPane().add(MainPanel, java.awt.BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void DashboardPanel1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_DashboardPanel1MouseClicked
        // TODO add your handling code here:
        showCard("dashboard");
        refreshDashboard();
        
    }//GEN-LAST:event_DashboardPanel1MouseClicked

    private void PosPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_PosPanelMouseClicked
        // TODO add your handling code here:
        showCard("pos");
        
    }//GEN-LAST:event_PosPanelMouseClicked

    private void SalesHistoryPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SalesHistoryPanelMouseClicked
        // TODO add your handling code here:
        showCard("salesHistory");
        applySalesHistoryFilters();
    }//GEN-LAST:event_SalesHistoryPanelMouseClicked

    private void InventoryPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_InventoryPanelMouseClicked
        // TODO add your handling code here:
        showCard("inventory");
    }//GEN-LAST:event_InventoryPanelMouseClicked

    private void ProductsPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ProductsPanelMouseClicked
        // TODO add your handling code here:
        showCard("products");
    }//GEN-LAST:event_ProductsPanelMouseClicked

    private void CategoriesPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_CategoriesPanelMouseClicked
        // TODO add your handling code here:
        showCard("categories");
    }//GEN-LAST:event_CategoriesPanelMouseClicked

    private void SuppliersPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SuppliersPanelMouseClicked
        // TODO add your handling code here:
        showCard("suppliers");
    }//GEN-LAST:event_SuppliersPanelMouseClicked

    private void ReportsPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ReportsPanelMouseClicked
        // TODO add your handling code here:
        showCard("reports");
    }//GEN-LAST:event_ReportsPanelMouseClicked

    private void UsersPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_UsersPanelMouseClicked
        // TODO add your handling code here:
        showCard("users");
    }//GEN-LAST:event_UsersPanelMouseClicked

    private void DashboardPanel1MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_DashboardPanel1MouseEntered
        // TODO add your handling code here:
        menuHover(DashboardPanel1);
    }//GEN-LAST:event_DashboardPanel1MouseEntered

    private void DashboardPanel1MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_DashboardPanel1MouseExited
        // TODO add your handling code here:
        menuExit(DashboardPanel1);
    }//GEN-LAST:event_DashboardPanel1MouseExited

    private void PosPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_PosPanelMouseEntered
        // TODO add your handling code here:
        menuHover(PosPanel);
    }//GEN-LAST:event_PosPanelMouseEntered

    private void PosPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_PosPanelMouseExited
        // TODO add your handling code here:
        menuExit(PosPanel);
    }//GEN-LAST:event_PosPanelMouseExited

    private void SalesHistoryPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SalesHistoryPanelMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_SalesHistoryPanelMouseEntered

    private void SalesHistoryPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SalesHistoryPanelMouseExited
        // TODO add your handling code here:
    }//GEN-LAST:event_SalesHistoryPanelMouseExited

    private void InventoryPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_InventoryPanelMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_InventoryPanelMouseEntered

    private void InventoryPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_InventoryPanelMouseExited
        // TODO add your handling code here:
    }//GEN-LAST:event_InventoryPanelMouseExited

    private void ProductsPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ProductsPanelMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_ProductsPanelMouseEntered

    private void ProductsPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ProductsPanelMouseExited
        // TODO add your handling code here:
    }//GEN-LAST:event_ProductsPanelMouseExited

    private void CategoriesPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_CategoriesPanelMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_CategoriesPanelMouseEntered

    private void CategoriesPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_CategoriesPanelMouseExited
        // TODO add your handling code here:
    }//GEN-LAST:event_CategoriesPanelMouseExited

    private void SuppliersPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SuppliersPanelMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_SuppliersPanelMouseEntered

    private void SuppliersPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SuppliersPanelMouseExited
        // TODO add your handling code here:
    }//GEN-LAST:event_SuppliersPanelMouseExited

    private void ReportsPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ReportsPanelMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_ReportsPanelMouseEntered

    private void ReportsPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ReportsPanelMouseExited
        // TODO add your handling code here:
    }//GEN-LAST:event_ReportsPanelMouseExited

    private void UsersPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_UsersPanelMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_UsersPanelMouseEntered

    private void UsersPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_UsersPanelMouseExited
        // TODO add your handling code here:
    }//GEN-LAST:event_UsersPanelMouseExited

    private void addUser_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addUser_btnActionPerformed
        String idText = userID_txt.getText().trim();
        String first = firstname_txt1.getText().trim();
        String middle = middlename_txt.getText().trim();
        String last = lastname_txt.getText().trim();
        String username = username_txt.getText().trim();
        String password = String.valueOf(password_txt.getPassword()).trim();
        String confirm = String.valueOf(confirmPass_txt.getPassword()).trim();
        String role = role_cmb.getSelectedItem().toString();
        String status = status_cmb.getSelectedItem().toString();

        if (isEditMode || (!idText.isEmpty() && !idText.equalsIgnoreCase("[Auto]"))) {
            JOptionPane.showMessageDialog(
                    this,
                    "You selected an existing user. Click CLEAR first if you want to add a new user.",
                    "Add Blocked",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (first.isEmpty() || last.isEmpty() || username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please fill in all required user fields first.",
                    "Missing Input",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Password does not match!",
                    "Password Error",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Users user = new Users();

        if (user.isUsernameExists(username, -1)) {
            JOptionPane.showMessageDialog(this,
                    "Username already exists. Please use another username.",
                    "Duplicate Username",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (user.isFullNameExists(first, middle, last, -1)) {
            JOptionPane.showMessageDialog(this,
                    "User already exists with the same full name.",
                    "Duplicate User",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (user.isFirstLastNameExists(first, last, -1)) {
            int proceed = JOptionPane.showConfirmDialog(
                    this,
                    "The first name and last name already exist. Do you want to proceed?",
                    "Possible Duplicate Name",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (proceed != JOptionPane.YES_OPTION) {
                return;
            }
        }

        if ("Super Admin".equals(role) && user.isSuperAdminExists()) {
            JOptionPane.showMessageDialog(this,
                    "Only one Super Admin account is allowed in the system.",
                    "Super Admin Restricted",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean added = user.addUser(first, middle, last, username, password, role, status);

        if (added) {
            user.loadUsers(user_tbl);
            clearFields();
        }
    }//GEN-LAST:event_addUser_btnActionPerformed

    private void update_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_update_btnActionPerformed
        int row = user_tbl.getSelectedRow();

        Users user = new Users();

        if (!isEditMode) {
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a user first.");
                return;
            }

            selectedUserId = Integer.parseInt(user_tbl.getValueAt(row, 0).toString());
            Users.UserDetails details = user.getUserById(selectedUserId);

            if (details == null) {
                JOptionPane.showMessageDialog(this, "Unable to load the selected user.");
                return;
            }

            userID_txt.setText(String.valueOf(details.userId));
            firstname_txt1.setText(details.firstName == null ? "" : details.firstName);
            middlename_txt.setText(details.middleName == null ? "" : details.middleName);
            lastname_txt.setText(details.lastName == null ? "" : details.lastName);
            username_txt.setText(details.username == null ? "" : details.username);
            password_txt.setText("");
            confirmPass_txt.setText("");
            confirmPass_txt.setForeground(java.awt.Color.BLACK);
            status_cmb.setSelectedItem(details.status);
            createdAt_txt1.setText(details.createdAt == null ? "[Auto]" : details.createdAt);
            updatedat_txt1.setText(details.updatedAt == null ? "[Auto]" : details.updatedAt);

            isEditMode = true;
            addUser_btn.setEnabled(false);
            update_btn.setText("SAVE");
            firstname_txt1.requestFocus();

            if ("Super Admin".equals(loggedInRole) && "Super Admin".equals(details.role)) {
                role_cmb.removeAllItems();
                role_cmb.addItem("Super Admin");
                role_cmb.setEnabled(false);
            } else {
                role_cmb.setEnabled(true);
                role_cmb.removeAllItems();
                role_cmb.addItem("Super Admin");
                role_cmb.addItem("Admin");
                role_cmb.addItem("Cashier");
                role_cmb.addItem("Inventory Clerk");
                role_cmb.addItem("Manager");
                role_cmb.setSelectedItem(details.role);
            }
            return;
        }

        if (selectedUserId <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a user first.");
            return;
        }

        String first = firstname_txt1.getText().trim();
        String middle = middlename_txt.getText().trim();
        String last = lastname_txt.getText().trim();
        String username = username_txt.getText().trim();
        String password = String.valueOf(password_txt.getPassword()).trim();
        String confirm = String.valueOf(confirmPass_txt.getPassword()).trim();
        String role = role_cmb.getSelectedItem().toString();
        String status = status_cmb.getSelectedItem().toString();

        if (first.isEmpty() || last.isEmpty() || username.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "First name, last name, and username are required.");
            return;
        }

        if (!password.isEmpty() && !password.equals(confirm)) {
            JOptionPane.showMessageDialog(this,
                    "Password and confirm password do not match.");
            return;
        }

        if (user.isUsernameExists(username, selectedUserId)) {
            JOptionPane.showMessageDialog(this,
                    "Username already exists. Please use another username.",
                    "Duplicate Username",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (user.isFullNameExists(first, middle, last, selectedUserId)) {
            JOptionPane.showMessageDialog(this,
                    "User already exists with the same full name.",
                    "Duplicate User",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (user.isFirstLastNameExists(first, last, selectedUserId)) {
            int proceed = JOptionPane.showConfirmDialog(
                    this,
                    "The first name and last name already exist. Do you want to proceed?",
                    "Possible Duplicate Name",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (proceed != JOptionPane.YES_OPTION) {
                return;
            }
        }

        Users.UserDetails currentDetails = user.getUserById(selectedUserId);
        if ("Super Admin".equals(role)
                && currentDetails != null
                && !"Super Admin".equals(currentDetails.role)
                && user.isSuperAdminExists()) {
            JOptionPane.showMessageDialog(this,
                    "Only one Super Admin account is allowed in the system.",
                    "Super Admin Restricted",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if ("Admin".equals(loggedInRole) && "Super Admin".equals(currentDetails.role)) {
            JOptionPane.showMessageDialog(this,
                    "Admins are not allowed to edit Super Admin credentials.",
                    "Access Denied",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean updated = user.updateUser(selectedUserId, first, middle, last, username, password, role, status);

        if (updated) {
            user.loadUsers(user_tbl);
            clearFields();
        }
    }//GEN-LAST:event_update_btnActionPerformed

    private void delete_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delete_btnActionPerformed
        int row = user_tbl.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select user first");
            return;
        }

        int id = Integer.parseInt(user_tbl.getValueAt(row, 0).toString());
        Users user = new Users();
        Users.UserDetails details = user.getUserById(id);

        if (details != null && "Super Admin".equals(details.role)) {
            JOptionPane.showMessageDialog(this,
                    "Super Admin account cannot be deleted.",
                    "Delete Blocked",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if ("Admin".equals(loggedInRole) && "Super Admin".equals(details.role)) {
            JOptionPane.showMessageDialog(this,
                    "Admins are not allowed to delete Super Admin accounts.",
                    "Access Denied",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this user?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (user.deleteUser(id)) {
                user.loadUsers(user_tbl);
                clearFields();
            }
        }
    }//GEN-LAST:event_delete_btnActionPerformed

    private void clear_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clear_btnActionPerformed
        // TODO add your handling code here:
            clearFields();
           JOptionPane.showMessageDialog(this, "Fields cleared.");
        
    }//GEN-LAST:event_clear_btnActionPerformed

    private void firstname_txt1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_firstname_txt1KeyTyped
        // TODO add your handling code here:
         char c = evt.getKeyChar();

        if (Character.isDigit(c)) {
            evt.consume();
        }

    }//GEN-LAST:event_firstname_txt1KeyTyped

    private void middlename_txtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_middlename_txtKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();

        if (Character.isDigit(c)) {
            evt.consume();
        }

    }//GEN-LAST:event_middlename_txtKeyTyped

    private void lastname_txtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_lastname_txtKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();

        if (Character.isDigit(c)) {
            evt.consume();
        }

    }//GEN-LAST:event_lastname_txtKeyTyped

    private void confirmPass_txtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_confirmPass_txtKeyReleased
        // TODO add your handling code here:
        String pass = password_txt.getText();
        String confirm = confirmPass_txt.getText();

        if (!pass.equals(confirm)) {

            confirmPass_txt.setForeground(java.awt.Color.RED);

        } else {

            confirmPass_txt.setForeground(java.awt.Color.BLACK);

        }
    }//GEN-LAST:event_confirmPass_txtKeyReleased

    private void user_tblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_user_tblMouseClicked
        if (isEditMode) {
            return;
        }

    }//GEN-LAST:event_user_tblMouseClicked

    private void searchUser_txtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchUser_txtKeyReleased
        // TODO add your handling code here:
        String search = searchUser_txt.getText();
        String role = role_cmb1.getSelectedItem().toString();

        Users user = new Users();
        user.filterUsers(user_tbl, search, role);
    }//GEN-LAST:event_searchUser_txtKeyReleased

    private void role_cmb1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_role_cmb1ActionPerformed
        // TODO add your handling code here:
        String search = searchUser_txt.getText();
        String role = role_cmb1.getSelectedItem().toString();

        Users user = new Users();
        user.filterUsers(user_tbl, search, role);
    }//GEN-LAST:event_role_cmb1ActionPerformed

    private void addCategory_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addCategory_btnActionPerformed
        // TODO add your handling code here:
        String name = categoryName_txt.getText().trim();
        String desc = Description_txt.getText().trim();

        if (name.isEmpty() || desc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields first.");
            return;
        }

        Category cat = new Category();
        
        // Check for duplicate category name
        if (cat.isCategoryNameExists(name)) {
            JOptionPane.showMessageDialog(this, "Category name already exists!");
            categoryName_txt.setBackground(new Color(255, 102, 102));
            return;
        }

        cat.addCategory(name, desc);
        cat.loadCategories(Category_tbl);

        refreshProductCombos(); // very important

        categoryName_txt.setText("");
        Description_txt.setText("");
        categoryName_txt.setBackground(Color.white);
    }//GEN-LAST:event_addCategory_btnActionPerformed

    private void clearCategory_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearCategory_btnActionPerformed
        // TODO add your handling code here:
        categoryName_txt.setText("");
        Description_txt.setText("");
        categoryName_txt.setBackground(Color.white);

        // Reset button states
        addCategory_btn.setEnabled(true);
        deleteCategory_btn.setEnabled(true);
        categoryEditMode = false;

        JOptionPane.showMessageDialog(this, "Fields cleared!");
    }//GEN-LAST:event_clearCategory_btnActionPerformed

    private void updateCategory_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateCategory_btnActionPerformed
        // TODO add your handling code here:
        
        int row = Category_tbl.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select category first");
            return;
        }

        if (!categoryEditMode) {

            categoryName_txt.setText(Category_tbl.getValueAt(row, 1).toString());
            Description_txt.setText(Category_tbl.getValueAt(row, 2).toString());

            categoryEditMode = true;

            // Disable add and delete buttons when in update mode
            addCategory_btn.setEnabled(false);
            deleteCategory_btn.setEnabled(false);

            JOptionPane.showMessageDialog(this, "Edit mode enabled. You can now edit the category.");

        } else {

            int id = Integer.parseInt(Category_tbl.getValueAt(row, 0).toString());

            String name = categoryName_txt.getText().trim();
            String desc = Description_txt.getText().trim();

            if (name.isEmpty() || desc.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all required fields.");
                return;
            }

            Category cat = new Category();
            
            // Check for duplicate category name (excluding current category)
            String oldName = Category_tbl.getValueAt(row, 1).toString();
            if (cat.isCategoryNameExists(name, oldName)) {
                JOptionPane.showMessageDialog(this, "Category name already exists!");
                categoryName_txt.setBackground(new Color(255, 102, 102));
                return;
            }

            cat.updateCategory(id, name, desc);

            cat.loadCategories(Category_tbl);

            categoryEditMode = false;

            // Re-enable add and delete buttons
            addCategory_btn.setEnabled(true);
            deleteCategory_btn.setEnabled(true);
            categoryName_txt.setBackground(Color.white);

        }

    }//GEN-LAST:event_updateCategory_btnActionPerformed

    private void deleteCategory_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteCategory_btnActionPerformed
        // TODO add your handling code here:
        int row = Category_tbl.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select category first");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete this category?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {

            int id = Integer.parseInt(Category_tbl.getValueAt(row, 0).toString());

            Category cat = new Category();
            cat.deleteCategory(id);
            cat.loadCategories(Category_tbl);
        }
        
    }//GEN-LAST:event_deleteCategory_btnActionPerformed

    private void categoriesSearch_txtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_categoriesSearch_txtKeyReleased
        // TODO add your handling code here:
        String keyword = categoriesSearch_txt.getText();

        Category cat = new Category();
        cat.searchCategory(Category_tbl, keyword);
    }//GEN-LAST:event_categoriesSearch_txtKeyReleased

    private void ClearSupplier_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClearSupplier_btnActionPerformed
        // TODO add your handling code here:
        supplierName_txt.setText("");
        contactPerson_txt.setText("");
        contactNumber_txt.setText("");
        email_txt.setText("");
        address_txt.setText("");
        supplierSearch_txt.setText("");
        
        // Reset background color and button states
        supplierName_txt.setBackground(Color.white);
        addSupplier_btn.setEnabled(true);
        deleteSupplier_btn.setEnabled(true);
        supplierEditMode = false;

        JOptionPane.showMessageDialog(this, "Fields cleared.");
        
    }//GEN-LAST:event_ClearSupplier_btnActionPerformed

    private void addSupplier_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSupplier_btnActionPerformed
        // TODO add your handling code here:
        
        String name = supplierName_txt.getText().trim();
        String contactPerson = contactPerson_txt.getText().trim();
        String contactNumber = contactNumber_txt.getText().trim();
        String email = email_txt.getText().trim();
        String address = address_txt.getText().trim();

        if (name.isEmpty() || contactPerson.isEmpty() || contactNumber.isEmpty() || email.isEmpty() || address.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }

        if (!isValidPhilippineContactNumber(contactNumber)) {
            JOptionPane.showMessageDialog(this, "Contact number must be exactly 11 digits starting with '09' (e.g., 09123456789).");
            return;
        }

        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address (e.g., user@domain.com).");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to add this supplier?",
                "Confirm Add",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            Supplier sup = new Supplier();
            
            // Check for duplicate supplier name
            if (sup.isSupplierNameExists(name)) {
                JOptionPane.showMessageDialog(this, "Supplier name already exists!");
                supplierName_txt.setBackground(new Color(255, 102, 102));
                return;
            }
            
            sup.addSupplier(name, contactPerson, contactNumber, email, address);
            sup.loadSuppliers(supplier_tbl);

            supplierName_txt.setText("");
            contactPerson_txt.setText("");
            contactNumber_txt.setText("");
            email_txt.setText("");
            address_txt.setText("");
            supplierName_txt.setBackground(Color.white);
        }
        refreshProductCombos();
    }//GEN-LAST:event_addSupplier_btnActionPerformed

    private void UpdateSupplier_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UpdateSupplier_btnActionPerformed
        // TODO add your handling code here:
        int row = supplier_tbl.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select supplier first.");
            return;
        }

        if (!supplierEditMode) {

            oldSupplierName = supplier_tbl.getValueAt(row, 0).toString();
            oldContactPerson = supplier_tbl.getValueAt(row, 1).toString();
            oldContactNumber = supplier_tbl.getValueAt(row, 2).toString();
            oldEmail = supplier_tbl.getValueAt(row, 3).toString();
            oldAddress = supplier_tbl.getValueAt(row, 4).toString();

            supplierName_txt.setText(oldSupplierName);
            contactPerson_txt.setText(oldContactPerson);
            contactNumber_txt.setText(oldContactNumber);
            email_txt.setText(oldEmail);
            address_txt.setText(oldAddress);


            supplierEditMode = true;

            // Disable add and delete buttons when in update mode
            addSupplier_btn.setEnabled(false);
            deleteSupplier_btn.setEnabled(false);

            JOptionPane.showMessageDialog(this, "Edit mode enabled. You can now edit the supplier.");

        } else {

            String name = supplierName_txt.getText().trim();
            String contactPerson = contactPerson_txt.getText().trim();
            String contactNumber = contactNumber_txt.getText().trim();
            String email = email_txt.getText().trim();
            String address = address_txt.getText().trim();

            if (name.isEmpty() || contactPerson.isEmpty() || contactNumber.isEmpty() || email.isEmpty() || address.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.");
                return;
            }

            if (!isValidPhilippineContactNumber(contactNumber)) {
                JOptionPane.showMessageDialog(this, "Contact number must be exactly 11 digits starting with '09' (e.g., 09123456789).");
                return;
            }

            if (!isValidEmail(email)) {
                JOptionPane.showMessageDialog(this, "Please enter a valid email address (e.g., user@domain.com).");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to update this supplier?",
                    "Confirm Update",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                Supplier sup = new Supplier();
                
                // Check for duplicate supplier name (excluding current supplier)
                if (sup.isSupplierNameExists(name, oldSupplierName)) {
                    JOptionPane.showMessageDialog(this, "Supplier name already exists!");
                    supplierName_txt.setBackground(new Color(255, 102, 102));
                    return;
                }
                
                sup.updateSupplier(
                        oldSupplierName, oldContactPerson, oldContactNumber, oldEmail, oldAddress,
                        name, contactPerson, contactNumber, email, address
                );
                sup.loadSuppliers(supplier_tbl);

                supplierEditMode = false;

                // Re-enable add and delete buttons
                addSupplier_btn.setEnabled(true);
                deleteSupplier_btn.setEnabled(true);
                supplierName_txt.setBackground(Color.white);

                supplierName_txt.setText("");
                contactPerson_txt.setText("");
                contactNumber_txt.setText("");
                email_txt.setText("");
                address_txt.setText("");


                oldSupplierName = "";
                oldContactPerson = "";
                oldContactNumber = "";
                oldEmail = "";
                oldAddress = "";
            }
        }
    }//GEN-LAST:event_UpdateSupplier_btnActionPerformed

    private void deleteSupplier_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteSupplier_btnActionPerformed
        // TODO add your handling code here:
        
        int row = supplier_tbl.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select supplier first.");
            return;
        }

        String name = supplier_tbl.getValueAt(row, 0).toString();
        String contactPerson = supplier_tbl.getValueAt(row, 1).toString();
        String contactNumber = supplier_tbl.getValueAt(row, 2).toString();
        String email = supplier_tbl.getValueAt(row, 3).toString();
        String address = supplier_tbl.getValueAt(row, 4).toString();

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this supplier?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            Supplier sup = new Supplier();
            sup.deleteSupplier(name, contactPerson, contactNumber, email, address);
            sup.loadSuppliers(supplier_tbl);

            supplierName_txt.setText("");
            contactPerson_txt.setText("");
            contactNumber_txt.setText("");
            email_txt.setText("");
            address_txt.setText("");
       
            supplierSearch_txt.setText("");

            supplierEditMode = false;
        }
    }//GEN-LAST:event_deleteSupplier_btnActionPerformed

    private void supplierSearch_txtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_supplierSearch_txtKeyReleased
        // TODO add your handling code here:
        String keyword = supplierSearch_txt.getText().trim();

        Supplier sup = new Supplier();
        sup.searchSupplier(supplier_tbl, keyword);
    }//GEN-LAST:event_supplierSearch_txtKeyReleased

    private void contactNumber_txtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_contactNumber_txtKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();
        String currentText = contactNumber_txt.getText();

        // Only allow digits and backspace
        if (!Character.isDigit(c) && c != '\b') {
            evt.consume();
            return;
        }

        // Limit to exactly 11 digits
        if (currentText.length() >= 11 && c != '\b') {
            evt.consume();
            return;
        }

        // Auto-add '09' prefix if field is empty and user starts with '9'
        if (currentText.isEmpty() && c == '9') {
            contactNumber_txt.setText("09");
            return;
        }
    }//GEN-LAST:event_contactNumber_txtKeyTyped

    private void supplier_tblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_supplier_tblMouseClicked
        // TODO add your handling code here:
        if (evt.getClickCount() == 2) {
            int row = supplier_tbl.getSelectedRow();

            if (row != -1) {
                supplierName_txt.setText(supplier_tbl.getValueAt(row, 0).toString());
                contactPerson_txt.setText(supplier_tbl.getValueAt(row, 1).toString());
                contactNumber_txt.setText(supplier_tbl.getValueAt(row, 2).toString());
                email_txt.setText(supplier_tbl.getValueAt(row, 3).toString());
                address_txt.setText(supplier_tbl.getValueAt(row, 4).toString());

         

                oldSupplierName = supplier_tbl.getValueAt(row, 0).toString();
                oldContactPerson = supplier_tbl.getValueAt(row, 1).toString();
                oldContactNumber = supplier_tbl.getValueAt(row, 2).toString();
                oldEmail = supplier_tbl.getValueAt(row, 3).toString();
                oldAddress = supplier_tbl.getValueAt(row, 4).toString();

                supplierEditMode = true;
            }
        }
    }//GEN-LAST:event_supplier_tblMouseClicked

    private void clearProduct_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearProduct_btnActionPerformed
         clearProductFields();
    }//GEN-LAST:event_clearProduct_btnActionPerformed

    private void addProduct_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addProduct_btnActionPerformed
        // TODO add your handling code here:
        String barcode = barcode_txt.getText().trim();
        String name = productName_txt.getText().trim();
        String category = category_cmb.getSelectedItem().toString();
        String supplier = supplier_cmb.getSelectedItem().toString();
        String cost = costPrice_txt.getText().trim();
        String selling = sellingPrice_txt.getText().trim();
        String stock = stockquantity_txt.getText().trim();
        String reorder = reorderLevel_txt.getText().trim();

        if (barcode.isEmpty() || name.isEmpty() || cost.isEmpty() || selling.isEmpty() || stock.isEmpty() || reorder.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields.");
            return;
        }

        if (category.equals("SELECT CATEGORY") || category.equals("ALL")) {
            JOptionPane.showMessageDialog(this, "Please select a valid category.\nAdd categories first if none are available.");
            return;
        }

        if (supplier.equals("SELECT SUPPLIER") || supplier.equals("ALL")) {
            JOptionPane.showMessageDialog(this, "Please select a valid supplier.\nAdd suppliers first if none are available.");
            return;
        }

        if (!isDecimalNumber(cost)) {
            JOptionPane.showMessageDialog(this, "Cost price must be a valid number.");
            return;
        }

        if (!isDecimalNumber(selling)) {
            JOptionPane.showMessageDialog(this, "Selling price must be a valid number.");
            return;
        }

        if (!isIntegerOnly(stock)) {
            JOptionPane.showMessageDialog(this, "Stock quantity must be a whole number.");
            return;
        }

        if (!isIntegerOnly(reorder)) {
            JOptionPane.showMessageDialog(this, "Reorder level must be a whole number.");
            return;
        }

        Products prod = new Products();

        if (prod.isBarcodeExists(barcode)) {
            JOptionPane.showMessageDialog(this, "Barcode already exists.");
            barcode_txt.setBackground(new Color(255, 102, 102));
            return;
        }

        if (prod.isProductNameExists(name)) {
            JOptionPane.showMessageDialog(this, "Product name already exists.");
            productName_txt.setBackground(new Color(255, 102, 102));
            return;
        }

        double costPrice = Double.parseDouble(cost);
        double sellingPrice = Double.parseDouble(selling);

        if (costPrice > sellingPrice) {
            JOptionPane.showMessageDialog(this, "Cost price cannot be higher than selling price.");
            costPrice_txt.setBackground(new Color(255, 102, 102));
            sellingPrice_txt.setBackground(new Color(255, 102, 102));
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to add this product?",
                "Confirm Add",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            prod.addProduct(
                    barcode,
                    name,
                    category,
                    supplier,
                    costPrice,
                    sellingPrice,
                    Integer.parseInt(stock),
                    Integer.parseInt(reorder),
                    selectedProductImagePath
            );

            JOptionPane.showMessageDialog(this, "Product added successfully.");

            prod.loadProducts(product_tbl);
            clearProductFields();

            // this one already works
            refreshDashboard();
            refreshInventoryModule();
        }
    }//GEN-LAST:event_addProduct_btnActionPerformed

    private void updateProduct_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateProduct_btnActionPerformed
        // TODO add your handling code here:
        
        int row = product_tbl.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select product first.");
            return;
        }

        if (!productEditMode) {

            selectedProductId = Integer.parseInt(product_tbl.getValueAt(row, 0).toString());
            oldBarcode = product_tbl.getValueAt(row, 1).toString();

            barcode_txt.setText(oldBarcode);
            productName_txt.setText(product_tbl.getValueAt(row, 2).toString());
            category_cmb.setSelectedItem(product_tbl.getValueAt(row, 3).toString());
            supplier_cmb.setSelectedItem(product_tbl.getValueAt(row, 4).toString());
            costPrice_txt.setText(product_tbl.getValueAt(row, 5).toString());
            sellingPrice_txt.setText(product_tbl.getValueAt(row, 6).toString());

            Object imageObj = product_tbl.getValueAt(row, 7);
            selectedProductImagePath = (imageObj == null) ? "" : imageObj.toString();

            stockquantity_txt.setText(product_tbl.getValueAt(row, 8).toString());
            reorderLevel_txt.setText(product_tbl.getValueAt(row, 9).toString());

            Object createdObj = product_tbl.getValueAt(row, 11);
            Object updatedObj = product_tbl.getValueAt(row, 12);

            if (!selectedProductImagePath.isEmpty()) {
                java.io.File imgFile = new java.io.File(selectedProductImagePath);

                if (imgFile.exists()) {
                    javax.swing.ImageIcon icon = new javax.swing.ImageIcon(selectedProductImagePath);
                    java.awt.Image img = icon.getImage().getScaledInstance(
                            Image_txt.getWidth(),
                            Image_txt.getHeight(),
                            java.awt.Image.SCALE_SMOOTH
                    );
                    Image_txt.setIcon(new javax.swing.ImageIcon(img));
                    Image_txt.setText("");
                } else {
                    Image_txt.setIcon(null);
                    Image_txt.setText("No Image");
                }
            } else {
                Image_txt.setIcon(null);
                Image_txt.setText("No Image");
            }

            productEditMode = true;

            // Disable add and delete buttons when in update mode
            addProduct_btn.setEnabled(false);
            deleteProduct_btn.setEnabled(false);

            JOptionPane.showMessageDialog(this, "Edit mode enabled. You can now edit the product and click Update again to save.");

        } else {

            String newBarcode = barcode_txt.getText().trim();
            String name = productName_txt.getText().trim();
            String category = category_cmb.getSelectedItem() == null ? "" : category_cmb.getSelectedItem().toString();
            String supplier = supplier_cmb.getSelectedItem() == null ? "" : supplier_cmb.getSelectedItem().toString();
            String cost = costPrice_txt.getText().trim();
            String selling = sellingPrice_txt.getText().trim();
            String stock = stockquantity_txt.getText().trim();
            String reorder = reorderLevel_txt.getText().trim();

            if (newBarcode.isEmpty() || name.isEmpty() || cost.isEmpty() || selling.isEmpty() || stock.isEmpty() || reorder.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all required fields.");
                return;
            }

            if (category.equals("SELECT CATEGORY") || category.equals("ALL") || category.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a valid category.\nAdd categories first if none are available.");
                return;
            }

            if (supplier.equals("SELECT SUPPLIER") || supplier.equals("ALL") || supplier.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a valid supplier.\nAdd suppliers first if none are available.");
                return;
            }

            if (!isDecimalNumber(cost)) {
                JOptionPane.showMessageDialog(this, "Cost price must be a valid number.");
                return;
            }

            if (!isDecimalNumber(selling)) {
                JOptionPane.showMessageDialog(this, "Selling price must be a valid number.");
                return;
            }

            if (!isIntegerOnly(stock)) {
                JOptionPane.showMessageDialog(this, "Stock quantity must be a whole number.");
                return;
            }

            if (!isIntegerOnly(reorder)) {
                JOptionPane.showMessageDialog(this, "Reorder level must be a whole number.");
                return;
            }

            Products prod = new Products();

            // Only check duplicate if barcode was changed
            if (!newBarcode.equals(oldBarcode) && prod.isBarcodeExists(newBarcode)) {
                JOptionPane.showMessageDialog(this, "Barcode already exists.");
                barcode_txt.setBackground(new java.awt.Color(255, 102, 102));
                return;
            } else {
                barcode_txt.setBackground(java.awt.Color.WHITE);
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to update this product?",
                    "Confirm Update",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                prod.updateProduct(
                        selectedProductId,
                        newBarcode,
                        name,
                        category,
                        supplier,
                        Double.parseDouble(cost),
                        Double.parseDouble(selling),
                        Integer.parseInt(stock),
                        Integer.parseInt(reorder),
                        selectedProductImagePath
                );

                prod.loadProducts(product_tbl);
                clearProductFields();

                productEditMode = false;
                selectedProductId = -1;
                oldBarcode = "";
            }
        }
    }//GEN-LAST:event_updateProduct_btnActionPerformed

    private void deleteProduct_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteProduct_btnActionPerformed
        // TODO add your handling code here:
        
        int row = product_tbl.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select product first.");
            return;
        }

        int productId = Integer.parseInt(product_tbl.getValueAt(row, 0).toString());

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Do you want to delete this product?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            Products prod = new Products();
            prod.deleteProduct(productId);
            prod.loadProducts(product_tbl);

            clearProductFields();
            productEditMode = false;
            selectedProductId = -1;
        }
    }//GEN-LAST:event_deleteProduct_btnActionPerformed

    private void removeImage_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeImage_btnActionPerformed
        // TODO add your handling code here:
        
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to remove the image?",
                "Confirm Remove Image",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            selectedProductImagePath = "";
            Image_txt.setIcon(null);
            Image_txt.setText("");
        }
    }//GEN-LAST:event_removeImage_btnActionPerformed

    private void chooseImage_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooseImage_btnActionPerformed
        // TODO add your handling code here:
        
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif"));

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            selectedProductImagePath = file.getAbsolutePath();

            ImageIcon icon = new ImageIcon(selectedProductImagePath);
            Image img = icon.getImage().getScaledInstance(
                    Image_txt.getWidth(),
                    Image_txt.getHeight(),
                    Image.SCALE_SMOOTH
            );
            Image_txt.setIcon(new ImageIcon(img));
            Image_txt.setText("");
        }
    }//GEN-LAST:event_chooseImage_btnActionPerformed

    private void searchProduct_txtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchProduct_txtActionPerformed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_searchProduct_txtActionPerformed

    private void statusProduct_cmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statusProduct_cmbActionPerformed
        // TODO add your handling code here:
        filterProductsNow();
    }//GEN-LAST:event_statusProduct_cmbActionPerformed

    private void categoryProduct_cmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_categoryProduct_cmbActionPerformed
        // TODO add your handling code here:
        filterProductsNow();
    }//GEN-LAST:event_categoryProduct_cmbActionPerformed

    private void supplierProduct_cmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_supplierProduct_cmbActionPerformed
        // TODO add your handling code here:
        filterProductsNow();
    }//GEN-LAST:event_supplierProduct_cmbActionPerformed

    private void barcode_txtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_barcode_txtKeyReleased
        // TODO add your handling code here:
        checkBarcodeDuplicate();

    }//GEN-LAST:event_barcode_txtKeyReleased

    private void searchProduct_txtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchProduct_txtKeyReleased
        // TODO add your handling code here:
        filterProductsNow();
    }//GEN-LAST:event_searchProduct_txtKeyReleased

    private void product_tblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_product_tblMouseClicked
        // TODO add your handling code here:
        if (evt.getClickCount() == 2) {
            int row = product_tbl.getSelectedRow();

            if (row != -1) {
                selectedProductId = Integer.parseInt(product_tbl.getValueAt(row, 0).toString());

                barcode_txt.setText(product_tbl.getValueAt(row, 1).toString());
                productName_txt.setText(product_tbl.getValueAt(row, 2).toString());
                category_cmb.setSelectedItem(product_tbl.getValueAt(row, 3).toString());
                supplier_cmb.setSelectedItem(product_tbl.getValueAt(row, 4).toString());
                costPrice_txt.setText(product_tbl.getValueAt(row, 5).toString());
                sellingPrice_txt.setText(product_tbl.getValueAt(row, 6).toString());

                Object imageObj = product_tbl.getValueAt(row, 7);
                selectedProductImagePath = imageObj == null ? "" : imageObj.toString();

                stockquantity_txt.setText(product_tbl.getValueAt(row, 8).toString());
                reorderLevel_txt.setText(product_tbl.getValueAt(row, 9).toString());

                Object createdObj = product_tbl.getValueAt(row, 11);
                Object updatedObj = product_tbl.getValueAt(row, 12);

                if (!selectedProductImagePath.isEmpty()) {
                    ImageIcon icon = new ImageIcon(selectedProductImagePath);
                    Image img = icon.getImage().getScaledInstance(
                            Image_txt.getWidth(),
                            Image_txt.getHeight(),
                            Image.SCALE_SMOOTH
                    );
                    Image_txt.setIcon(new ImageIcon(img));
                    Image_txt.setText("");
                } else {
                    Image_txt.setIcon(null);
                    Image_txt.setText("");
                }

                // Enable view-only mode: disable all buttons except clear, make fields read-only
                setProductViewMode(true);
            }
        }
    }//GEN-LAST:event_product_tblMouseClicked

    private void costPrice_txtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_costPrice_txtKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();
        if (!Character.isDigit(c) && c != '.' && c != '\b' && c != '\u007F') {
            evt.consume();
        }
    }//GEN-LAST:event_costPrice_txtKeyTyped

    private void costPrice_txtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_costPrice_txtKeyReleased
        // TODO add your handling code here:
        calculateProfitMargin();
    }//GEN-LAST:event_costPrice_txtKeyReleased

    private void sellingPrice_txtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_sellingPrice_txtKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();
        if (!Character.isDigit(c) && c != '.' && c != '\b' && c != '\u007F') {
            evt.consume();
        }
    }//GEN-LAST:event_sellingPrice_txtKeyTyped

    private void sellingPrice_txtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_sellingPrice_txtKeyReleased
        // TODO add your handling code here:
        calculateProfitMargin();
    }//GEN-LAST:event_sellingPrice_txtKeyReleased

    private void calculateProfitMargin() {
        String costText = costPrice_txt.getText().trim();
        String sellingText = sellingPrice_txt.getText().trim();
        
        if (!costText.isEmpty() && !sellingText.isEmpty()) {
            try {
                double cost = Double.parseDouble(costText);
                double selling = Double.parseDouble(sellingText);
                
                if (cost > 0 && selling > 0) {
                    double margin = ((selling - cost) / selling) * 100;
                    double profit = selling - cost;
                    
                    // Update a label or show tooltip with profit info
                    costPrice_txt.setToolTipText("Cost: ₱" + String.format("%.2f", cost));
                    sellingPrice_txt.setToolTipText("Selling: ₱" + String.format("%.2f", selling) + 
                                                  "\nProfit: ₱" + String.format("%.2f", profit) + 
                                                  "\nMargin: " + String.format("%.1f", margin) + "%");
                    
                    // Color code based on margin
                    if (margin < 10) {
                        sellingPrice_txt.setBackground(new Color(255, 230, 230)); // Light red for low margin
                    } else if (margin < 20) {
                        sellingPrice_txt.setBackground(new Color(255, 255, 230)); // Light yellow for medium margin
                    } else {
                        sellingPrice_txt.setBackground(new Color(230, 255, 230)); // Light green for good margin
                    }
                }
            } catch (NumberFormatException e) {
                // Invalid input, reset colors
                sellingPrice_txt.setBackground(Color.white);
            }
        } else {
            sellingPrice_txt.setBackground(Color.white);
        }
    }

    private void reorderLevel_txtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_reorderLevel_txtKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();
        if (!Character.isDigit(c) && c != '\b') {
            evt.consume();
        }
    }//GEN-LAST:event_reorderLevel_txtKeyTyped

    private void stockquantity_txtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_stockquantity_txtKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();
        if (!Character.isDigit(c) && c != '\b' && c != '\u007F') {
            evt.consume();
        }
    }//GEN-LAST:event_stockquantity_txtKeyTyped

    private void stockquantity_txtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_stockquantity_txtKeyReleased
        // TODO add your handling code here:
        checkStockLevel();
    }//GEN-LAST:event_stockquantity_txtKeyReleased

    private void checkStockLevel() {
        String stockText = stockquantity_txt.getText().trim();
        String reorderText = reorderLevel_txt.getText().trim();
        
        if (!stockText.isEmpty() && !reorderText.isEmpty()) {
            try {
                int stock = Integer.parseInt(stockText);
                int reorder = Integer.parseInt(reorderText);
                
                if (stock <= 0) {
                    stockquantity_txt.setBackground(new Color(255, 200, 200)); // Red for out of stock
                    stockquantity_txt.setToolTipText("Out of Stock!");
                } else if (stock <= reorder) {
                    stockquantity_txt.setBackground(new Color(255, 230, 150)); // Orange for low stock
                    stockquantity_txt.setToolTipText("Low Stock! Reorder when stock reaches " + reorder);
                } else {
                    stockquantity_txt.setBackground(new Color(200, 255, 200)); // Green for good stock
                    stockquantity_txt.setToolTipText("Stock Level: " + stock + " (Reorder at: " + reorder + ")");
                }
            } catch (NumberFormatException e) {
                stockquantity_txt.setBackground(Color.white);
            }
        } else {
            stockquantity_txt.setBackground(Color.white);
        }
    }

    private void productName_txtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_productName_txtKeyReleased
        // TODO add your handling code here:
        checkProductDuplicate();
    }//GEN-LAST:event_productName_txtKeyReleased

    private void checkProductDuplicate() {
        String name = productName_txt.getText().trim();
        
        if (!name.isEmpty()) {
            try {
                Products prod = new Products();
                if (prod.isProductNameExists(name)) {
                    productName_txt.setBackground(new Color(255, 200, 200));
                    productName_txt.setToolTipText("Product name already exists!");
                } else {
                    productName_txt.setBackground(new Color(200, 255, 200));
                    productName_txt.setToolTipText("Product name available");
                }
            } catch (Exception e) {
                // If there's any error, reset to default state
                productName_txt.setBackground(Color.white);
                productName_txt.setToolTipText("Error checking product name");
                System.err.println("Error in checkProductDuplicate: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            productName_txt.setBackground(Color.white);
            productName_txt.setToolTipText("");
        }
    }

    private void autoGenerateBarcode_rbtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoGenerateBarcode_rbtActionPerformed
        // TODO add your handling code here:
        
        if (autoGenerateBarcode_rbt.isSelected()) {
            Products prod = new Products();
            String generatedBarcode = prod.generateUniqueBarcode();
            barcode_txt.setText(generatedBarcode);
            barcode_txt.setEditable(false);
        } else {
            barcode_txt.setText("");
            barcode_txt.setEditable(true);
        }
    }//GEN-LAST:event_autoGenerateBarcode_rbtActionPerformed

    private void stockin_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stockin_btnActionPerformed
        // TODO add your handling code here:
        CardLayout cl1 = (CardLayout) InventoryContentPanel.getLayout();
        cl1.show(InventoryContentPanel, "StockIN");

        CardLayout cl2 = (CardLayout) jtablePanel.getLayout();
        cl2.show(jtablePanel, "StockinJ");

        inventoryMode = "STOCK IN";
        saveInventory_btn.setText("Save Stock In");
    }//GEN-LAST:event_stockin_btnActionPerformed

    private void stockout_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stockout_btnActionPerformed
        // TODO add your handling code here:
        
        CardLayout cl1 = (CardLayout) InventoryContentPanel.getLayout();
        cl1.show(InventoryContentPanel, "StockOut");

        CardLayout cl2 = (CardLayout) jtablePanel.getLayout();
        cl2.show(jtablePanel, "StockOutJ");

        InventoryContentPanel.revalidate();
        InventoryContentPanel.repaint();
        jtablePanel.revalidate();
        jtablePanel.repaint();

        inventoryMode = "STOCK OUT";
        saveInventory_btn.setText("Save Stock Out");
    }//GEN-LAST:event_stockout_btnActionPerformed

    private void stockmovement_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stockmovement_btnActionPerformed
        // TODO add your handling code here:
        
        CardLayout c13 = (CardLayout) InventoryContentPanel.getLayout();
        c13.show(InventoryContentPanel, "StockMovement");
        
        
        CardLayout cl2 = (CardLayout) jtablePanel.getLayout();
        cl2.show(jtablePanel, "StockMovementJ");

        inventoryMode = "STOCK MOVEMENT";
        saveInventory_btn.setText("Save Inventory");
    }//GEN-LAST:event_stockmovement_btnActionPerformed

    private void productStockIN_cmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_productStockIN_cmbActionPerformed
        // TODO add your handling code here:
        
        if (productStockIN_cmb.getSelectedItem() == null) {
            return;
        }

        String productName = productStockIN_cmb.getSelectedItem().toString();

        if (productName.equals("SELECT PRODUCT")) {
            return;
        }

        InventoryMovement inv = new InventoryMovement();
        InventoryMovement.ProductDetails d = inv.getProductDetailsByName(productName);

        if (d != null) {
            barcodeIN_txt.setText(d.barcode);
            supplierIN_btn.setText(d.supplierName); // if this is really a button
            barcodeinGen_lbl.setText(d.barcode);
            currentStock_lbl.setText(String.valueOf(d.stockQty));
            statusIdentifier_txt.setText(d.status);
            applyStatusColor(statusIdentifier_txt, d.status);

            if (d.imagePath != null && !d.imagePath.trim().isEmpty()) {
                File imgFile = new File(d.imagePath);
                if (imgFile.exists()) {
                    ImageIcon icon = new ImageIcon(d.imagePath);
                    Image img = icon.getImage().getScaledInstance(
                            ImageIn_lbl.getWidth(),
                            ImageIn_lbl.getHeight(),
                            Image.SCALE_SMOOTH
                    );
                    ImageIn_lbl.setIcon(new ImageIcon(img));
                    ImageIn_lbl.setText("");
                } else {
                    ImageIn_lbl.setIcon(null);
                    ImageIn_lbl.setText("No Image");
                }
            } else {
                ImageIn_lbl.setIcon(null);
                ImageIn_lbl.setText("No Image");
            }
        }
    }//GEN-LAST:event_productStockIN_cmbActionPerformed

    private void ProductOut_cmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ProductOut_cmbActionPerformed
        // TODO add your handling code here:
        
        if (ProductOut_cmb.getSelectedItem() == null) {
            return;
        }

        String productName = ProductOut_cmb.getSelectedItem().toString();

        if (productName.equals("SELECT PRODUCT")) {
            return;
        }

        InventoryMovement inv = new InventoryMovement();
        InventoryMovement.ProductDetails d = inv.getProductDetailsByName(productName);

        if (d != null) {
            barcodeOUT_txt.setText(d.barcode);
            barcodeOUTGen_lbl.setText(d.barcode);
            currentStockOUT_lbl.setText(String.valueOf(d.stockQty));
            statusIdentifierOUT_txt.setText(d.status);
            applyStatusColor(statusIdentifierOUT_txt, d.status);

            if (d.imagePath != null && !d.imagePath.trim().isEmpty()) {
                File imgFile = new File(d.imagePath);
                if (imgFile.exists()) {
                    ImageIcon icon = new ImageIcon(d.imagePath);
                    Image img = icon.getImage().getScaledInstance(
                            ImageOUT_lbl.getWidth(),
                            ImageOUT_lbl.getHeight(),
                            Image.SCALE_SMOOTH
                    );
                    ImageOUT_lbl.setIcon(new ImageIcon(img));
                    ImageOUT_lbl.setText("");
                } else {
                    ImageOUT_lbl.setIcon(null);
                    ImageOUT_lbl.setText("No Image");
                }
            } else {
                ImageOUT_lbl.setIcon(null);
                ImageOUT_lbl.setText("No Image");
            }
        }
    }//GEN-LAST:event_ProductOut_cmbActionPerformed

    private void saveInventory_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveInventory_btnActionPerformed
        // TODO add your handling code here:
        
        InventoryMovement inv = new InventoryMovement();

        if (inventoryMode.equals("STOCK IN")) {

            String productName = productStockIN_cmb.getSelectedItem() == null ? "" : productStockIN_cmb.getSelectedItem().toString();
            String qtyText = quantityIn_txt.getText().trim();
            String refNo = ReferenceNo_txt.getText().trim();
            String remarks = remarksIn_txt.getText().trim();
            java.util.Date date = jDateChooser1.getDate();

            if (productName.equals("") || productName.equals("SELECT PRODUCT")) {
                JOptionPane.showMessageDialog(this, "Please select a product.");
                return;
            }

            if (qtyText.isEmpty() || !qtyText.matches("\\d+")) {
                JOptionPane.showMessageDialog(this, "Quantity must be numbers only.");
                return;
            }

            if (date == null) {
                JOptionPane.showMessageDialog(this, "Please select a date.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to save Stock In?",
                    "Confirm Save",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                int productId = inv.getProductIdByName(productName);

                inv.saveStockIn(
                        productId,
                        currentUserId,
                        Integer.parseInt(qtyText),
                        refNo,
                        date,
                        remarks
                );

                inv.loadStockInTable(StockIn_tbl, searchIN_txt.getText().trim(), typeIn_cmb.getSelectedItem().toString());
                reloadStockMovementTable();
                refreshSelectedStockInProduct();
                clearInventoryFields();
            }

        } else if (inventoryMode.equals("STOCK OUT")) {

            String productName = ProductOut_cmb.getSelectedItem() == null ? "" : ProductOut_cmb.getSelectedItem().toString();
            String qtyText = quantityOUT_txt.getText().trim();
            String reason = reason_cmb.getSelectedItem() == null ? "" : reason_cmb.getSelectedItem().toString();
            String refNo = ReferenceNoOUT_txt.getText().trim();
            String remarks = remarksOUT_txt.getText().trim();
            java.util.Date date = jDateChooser2.getDate();

            if (productName.equals("") || productName.equals("SELECT PRODUCT")) {
                JOptionPane.showMessageDialog(this, "Please select a product.");
                return;
            }

            if (qtyText.isEmpty() || !qtyText.matches("\\d+")) {
                JOptionPane.showMessageDialog(this, "Quantity must be numbers only.");
                return;
            }

            if (date == null) {
                JOptionPane.showMessageDialog(this, "Please select a date.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to save Stock Out?",
                    "Confirm Save",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                int productId = inv.getProductIdByName(productName);

                boolean ok = inv.saveStockOut(
                        productId,
                        currentUserId,
                        Integer.parseInt(qtyText),
                        refNo,
                        date,
                        reason,
                        remarks
                );

                if (ok) {
                    inv.loadStockOutTable(StockOUT_tbl, searchOUT_txt.getText().trim());
                    reloadStockMovementTable();
                    refreshSelectedStockOutProduct();
                    clearInventoryFields();
                }
            }
        }
    }//GEN-LAST:event_saveInventory_btnActionPerformed

    private void clearInv_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearInv_btnActionPerformed
        // TODO add your handling code here:
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Clear all fields?",
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {

            clearInventoryFields();

            JOptionPane.showMessageDialog(this, "Fields cleared successfully!");

        }
    }//GEN-LAST:event_clearInv_btnActionPerformed

    private void searchIN_txtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchIN_txtKeyReleased
        // TODO add your handling code here:
        InventoryMovement inv = new InventoryMovement();
        String keyword = searchIN_txt.getText().trim();
        String type = typeIn_cmb.getSelectedItem() == null ? "ALL" : typeIn_cmb.getSelectedItem().toString();
        inv.loadStockInTable(StockIn_tbl, keyword, type);
    }//GEN-LAST:event_searchIN_txtKeyReleased

    private void typeIn_cmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeIn_cmbActionPerformed
        // TODO add your handling code here:
        InventoryMovement inv = new InventoryMovement();
        String keyword = searchIN_txt.getText().trim();
        String type = typeIn_cmb.getSelectedItem() == null ? "ALL" : typeIn_cmb.getSelectedItem().toString();
        inv.loadStockInTable(StockIn_tbl, keyword, type);
    }//GEN-LAST:event_typeIn_cmbActionPerformed

    private void searchOUT_txtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchOUT_txtKeyReleased
        // TODO add your handling code here:
        InventoryMovement inv = new InventoryMovement();
        String keyword = searchOUT_txt.getText().trim();
        inv.loadStockOutTable(StockOUT_tbl, keyword);
    }//GEN-LAST:event_searchOUT_txtKeyReleased

    private void stockMovementSearch_txtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_stockMovementSearch_txtKeyReleased
        // TODO add your handling code here:
        reloadStockMovementTable();
    }//GEN-LAST:event_stockMovementSearch_txtKeyReleased

    private void quantityIn_txtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_quantityIn_txtKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();
        if (!Character.isDigit(c) && c != '\b') {
            evt.consume();
        }
    }//GEN-LAST:event_quantityIn_txtKeyTyped

    private void quantityOUT_txtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_quantityOUT_txtKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();
        if (!Character.isDigit(c) && c != '\b') {
            evt.consume();
        }
    }//GEN-LAST:event_quantityOUT_txtKeyTyped

    private void stockMovement_tblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_stockMovement_tblMouseClicked
        // TODO add your handling code here:
        if (evt.getClickCount() == 2) {

            int row = stockMovement_tbl.getSelectedRow();

            if (row == -1) {
                return;
            }

            String productName = stockMovement_tbl.getValueAt(row, 0).toString();

            InventoryMovement inv = new InventoryMovement();
            InventoryMovement.ProductDetails d = inv.getProductDetailsByName(productName);

            if (d != null) {

                ProductMovement_lbl.setText(d.productName);
                categoryMovement_lbl.setText(d.categoryName);
                supplierMovement_lbl.setText(d.supplierName);
                statusMovement_lbl.setText(d.status);
                reorderLevel_lbl.setText(String.valueOf(d.reorderLevel));
                StockQTYMovement_lbl.setText(String.valueOf(d.stockQty));

                applyMovementStatusColor(d.status);

                if (d.imagePath != null && !d.imagePath.trim().isEmpty()) {
                    java.io.File imgFile = new java.io.File(d.imagePath);

                    if (imgFile.exists()) {
                        javax.swing.ImageIcon icon = new javax.swing.ImageIcon(d.imagePath);
                        java.awt.Image img = icon.getImage().getScaledInstance(
                                ImageMovementDisplay_lbl.getWidth(),
                                ImageMovementDisplay_lbl.getHeight(),
                                java.awt.Image.SCALE_SMOOTH
                        );
                        ImageMovementDisplay_lbl.setIcon(new javax.swing.ImageIcon(img));
                        ImageMovementDisplay_lbl.setText("");
                    } else {
                        ImageMovementDisplay_lbl.setIcon(null);
                        ImageMovementDisplay_lbl.setText("No Image");
                    }
                } else {
                    ImageMovementDisplay_lbl.setIcon(null);
                    ImageMovementDisplay_lbl.setText("No Image");
                }
            }
        }
    }//GEN-LAST:event_stockMovement_tblMouseClicked

    private void StockOUT_tblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_StockOUT_tblMouseClicked
        // TODO add your handling code here:
         if (evt.getClickCount() == 2) {

            int row = StockOUT_tbl.getSelectedRow();
            if (row == -1) {
                return;
            }

            int transactionId = Integer.parseInt(StockOUT_tbl.getValueAt(row, 0).toString());

            InventoryMovement inv = new InventoryMovement();
            InventoryMovement.StockOutDetails d = inv.getStockOutDetailsByTransactionId(transactionId);

            if (d != null) {
                ProductOut_cmb.setSelectedItem(d.productName);
                barcodeOUT_txt.setText(d.barcode);
                quantityOUT_txt.setText(String.valueOf(d.quantity));
                reason_cmb.setSelectedItem(d.reason);
                ReferenceNoOUT_txt.setText(d.referenceNumber);
                jDateChooser2.setDate(d.transactionDate);
                remarksOUT_txt.setText(d.remarks == null ? "" : d.remarks);

                barcodeOUTGen_lbl.setText(d.barcode);
                currentStockOUT_lbl.setText(String.valueOf(d.currentStock));
                statusIdentifierOUT_txt.setText(d.status);
                applyStatusColor(statusIdentifierOUT_txt, d.status);

                if (d.imagePath != null && !d.imagePath.trim().isEmpty()) {
                    java.io.File imgFile = new java.io.File(d.imagePath);

                    if (imgFile.exists()) {
                        javax.swing.ImageIcon icon = new javax.swing.ImageIcon(d.imagePath);
                        java.awt.Image img = icon.getImage().getScaledInstance(
                                ImageOUT_lbl.getWidth(),
                                ImageOUT_lbl.getHeight(),
                                java.awt.Image.SCALE_SMOOTH
                        );
                        ImageOUT_lbl.setIcon(new javax.swing.ImageIcon(img));
                        ImageOUT_lbl.setText("");
                    } else {
                        ImageOUT_lbl.setIcon(null);
                        ImageOUT_lbl.setText("No Image");
                    }
                } else {
                    ImageOUT_lbl.setIcon(null);
                    ImageOUT_lbl.setText("No Image");
                }
            }
        }
        
    }//GEN-LAST:event_StockOUT_tblMouseClicked

    private void StockIn_tblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_StockIn_tblMouseClicked
        // TODO add your handling code here:
        if (evt.getClickCount() == 2) {

            int row = StockIn_tbl.getSelectedRow();
            if (row == -1) {
                return;
            }

            int transactionId = Integer.parseInt(StockIn_tbl.getValueAt(row, 0).toString());

            InventoryMovement inv = new InventoryMovement();
            InventoryMovement.StockInDetails d = inv.getStockInDetailsByTransactionId(transactionId);

            if (d != null) {
                productStockIN_cmb.setSelectedItem(d.productName);
                barcodeIN_txt.setText(d.barcode);
                supplierIN_btn.setText(d.supplierName); // change if not button
                quantityIn_txt.setText(String.valueOf(d.quantity));
                ReferenceNo_txt.setText(d.referenceNumber);
                jDateChooser1.setDate(d.transactionDate);
                remarksIn_txt.setText(d.remarks == null ? "" : d.remarks);

                barcodeinGen_lbl.setText(d.barcode);
                currentStock_lbl.setText(String.valueOf(d.currentStock));
                statusIdentifier_txt.setText(d.status);
                applyStatusColor(statusIdentifier_txt, d.status);

                if (d.imagePath != null && !d.imagePath.trim().isEmpty()) {
                    java.io.File imgFile = new java.io.File(d.imagePath);

                    if (imgFile.exists()) {
                        javax.swing.ImageIcon icon = new javax.swing.ImageIcon(d.imagePath);
                        java.awt.Image img = icon.getImage().getScaledInstance(
                                ImageIn_lbl.getWidth(),
                                ImageIn_lbl.getHeight(),
                                java.awt.Image.SCALE_SMOOTH
                        );
                        ImageIn_lbl.setIcon(new javax.swing.ImageIcon(img));
                        ImageIn_lbl.setText("");
                    } else {
                        ImageIn_lbl.setIcon(null);
                        ImageIn_lbl.setText("No Image");
                    }
                } else {
                    ImageIn_lbl.setIcon(null);
                    ImageIn_lbl.setText("No Image");
                }
            }
        }
    }//GEN-LAST:event_StockIn_tblMouseClicked

    private void searchSalesHistory_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchSalesHistory_btnActionPerformed
        salesHistoryQuickPeriod = "";
        applySalesHistoryFilters();
    }//GEN-LAST:event_searchSalesHistory_btnActionPerformed

    private void clearsalesHistory_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearsalesHistory_btnActionPerformed
        StartDateSalesHistory_txt.setDate(null);
        endDateSalesHistory_txt.setDate(null);

        InvoiceNoSalesHistory_txt.setText("");

        cashierSalesHistory_cmb.setSelectedIndex(0);
        paymentMethodSAlesHistory_cmb.setSelectedIndex(0);

        ((DefaultTableModel) selectedSaleItems_tbl.getModel()).setRowCount(0);
        applySalesQuickPeriod("TODAY");
    }//GEN-LAST:event_clearsalesHistory_btnActionPerformed

    private void salesList_tblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_salesList_tblMouseClicked
        // TODO add your handling code here:
         int row = salesList_tbl.getSelectedRow();

        if (row != -1) {
            int saleId = Integer.parseInt(salesList_tbl.getValueAt(row, 0).toString());
            salesHistory.loadSelectedSaleItems(saleId, selectedSaleItems_tbl);
        }
    }//GEN-LAST:event_salesList_tblMouseClicked

    private void generateReport_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateReport_btnActionPerformed
        // TODO add your handling code here:
         String reportType = ReportType_txt.getSelectedItem().toString();
        String category = categoryReport_cmb.getSelectedItem().toString();
        String supplier = supplierReport_cmb.getSelectedItem().toString();
        String payment = payment_cmb.getSelectedItem().toString();

        String startDate = reports.formatDateChooser(startDateRepor_txt.getDate());
        String endDate = reports.formatDateChooser(endDateReport_txt.getDate());

        if (!startDate.isEmpty() && !endDate.isEmpty() && startDate.compareTo(endDate) > 0) {
            JOptionPane.showMessageDialog(this, "Start date must not be later than end date.");
            return;
        }

        reports.generateReport(
                report_tbl,
                reportType,
                startDate,
                endDate,
                category,
                supplier,
                payment
        );
    }//GEN-LAST:event_generateReport_btnActionPerformed

    private void exportPdf_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPdf_btnActionPerformed
        // TODO add your handling code here:
        String reportType = ReportType_txt.getSelectedItem().toString();
        reports.exportTableToPDF(report_tbl, reportType);
    }//GEN-LAST:event_exportPdf_btnActionPerformed

    private void exportExel_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportExel_btnActionPerformed
        // TODO add your handling code here:
         String reportType = ReportType_txt.getSelectedItem().toString();
        reports.exportTableToExcel(report_tbl, reportType);
    }//GEN-LAST:event_exportExel_btnActionPerformed

    private void logout_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logout_btnActionPerformed
        // TODO add your handling code here:
        
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Logout Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            LoginScreen db = new LoginScreen();
            db.setVisible(true);
            this.dispose();
        }

    }//GEN-LAST:event_logout_btnActionPerformed

    private void dashboard_tblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_dashboard_tblMouseClicked
        // TODO add your handling code here:
        
    }//GEN-LAST:event_dashboard_tblMouseClicked

    private void passwordToggle_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_passwordToggle_btnActionPerformed
        if (password_txt.getEchoChar() == '\u2022') {
            password_txt.setEchoChar((char) 0);
        } else {
            password_txt.setEchoChar('\u2022');
        }
    }//GEN-LAST:event_passwordToggle_btnActionPerformed

    private void ConfirmpasswordToggle_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ConfirmpasswordToggle_btnActionPerformed
        if (confirmPass_txt.getEchoChar() == '\u2022') {
            confirmPass_txt.setEchoChar((char) 0);
        } else {
            confirmPass_txt.setEchoChar('\u2022');
        }
    }//GEN-LAST:event_ConfirmpasswordToggle_btnActionPerformed

    private void products_panelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_products_panelMouseClicked
        openDashboardModule("products", "products", "productCard");
    }//GEN-LAST:event_products_panelMouseClicked

    private void suppliers_panelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_suppliers_panelMouseClicked
        openDashboardModule("suppliers", "suppliers", "supplierCard");
    }//GEN-LAST:event_suppliers_panelMouseClicked

    private void users_panelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_users_panelMouseClicked
        openDashboardModule("users", "users", "userCard");
    }//GEN-LAST:event_users_panelMouseClicked

    private void lowStock_PanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lowStock_PanelMouseClicked
        openInventoryStockMonitoring();
    }//GEN-LAST:event_lowStock_PanelMouseClicked

    private void totalSales_PanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_totalSales_PanelMouseClicked
        openDashboardModule("salesHistory", "salesHistory", "salesCard");
    }//GEN-LAST:event_totalSales_PanelMouseClicked

    private void outOfStock_PanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_outOfStock_PanelMouseClicked
        openInventoryStockMonitoring();
    }//GEN-LAST:event_outOfStock_PanelMouseClicked

    private void totalCategory_PanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_totalCategory_PanelMouseClicked
        openDashboardModule("categories", "categories", "categoryCard");
    }//GEN-LAST:event_totalCategory_PanelMouseClicked

    private void transaction_panelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_transaction_panelMouseClicked
        openDashboardModule("salesHistory", "salesHistory", "salesCard");
    }//GEN-LAST:event_transaction_panelMouseClicked

   
    public static void main(String args[]) {
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
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new MainPanel().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel CategoriesPanel;
    private javax.swing.JPanel CategoryPanelHeader;
    private javax.swing.JTable Category_tbl;
    private javax.swing.JButton ClearSupplier_btn;
    private javax.swing.JToggleButton ConfirmpasswordToggle_btn;
    private javax.swing.JPanel DashboardPanel;
    private javax.swing.JPanel DashboardPanel1;
    private javax.swing.JPanel DashboardPanelHeader;
    private javax.swing.JTextArea Description_txt;
    private javax.swing.JPanel Header;
    private javax.swing.JPanel HeaderPanel;
    private javax.swing.JPanel HeaderTitle;
    private javax.swing.JLabel ImageIn_lbl;
    private javax.swing.JLabel ImageMovementDisplay_lbl;
    private javax.swing.JLabel ImageOUT_lbl;
    private javax.swing.JLabel Image_txt;
    private javax.swing.JPanel InventoryContentPanel;
    private javax.swing.JPanel InventoryPanel;
    private javax.swing.JPanel InventoryPanelHeader;
    private javax.swing.JTextField InvoiceNoSalesHistory_txt;
    private javax.swing.JPanel MainPanel;
    private javax.swing.JPanel PosPanel;
    private javax.swing.JPanel PosPanelHeader;
    private javax.swing.JLabel ProductMovement_lbl;
    private javax.swing.JComboBox<String> ProductOut_cmb;
    private javax.swing.JPanel ProductPanelHeader;
    private javax.swing.JPanel ProductsPanel;
    private javax.swing.JTextField ReferenceNoOUT_txt;
    private javax.swing.JTextField ReferenceNo_txt;
    private javax.swing.JComboBox<String> ReportType_txt;
    private javax.swing.JPanel ReportsPanel;
    private javax.swing.JPanel ReportsPanelHeader;
    private javax.swing.JPanel SalesHistoryPanel;
    private javax.swing.JPanel SalesHistoryPanelHeader;
    private javax.swing.JPanel SideBarPanel;
    private com.toedter.calendar.JDateChooser StartDateSalesHistory_txt;
    private javax.swing.JPanel StockIN;
    private javax.swing.JTable StockIn_tbl;
    private javax.swing.JPanel StockMovement;
    private javax.swing.JPanel StockMovementJ;
    private javax.swing.JTable StockOUT_tbl;
    private javax.swing.JPanel StockOut;
    private javax.swing.JLabel StockQTYMovement_lbl;
    private javax.swing.JPanel StockinJ;
    private javax.swing.JPanel SupplierPanelHeader;
    private javax.swing.JPanel SuppliersPanel;
    private javax.swing.JButton UpdateSupplier_btn;
    private javax.swing.JPanel UsersPanel;
    private javax.swing.JPanel UsersPanelHeader;
    private javax.swing.JButton addCategory_btn;
    private javax.swing.JButton addProduct_btn;
    private javax.swing.JButton addSupplier_btn;
    private javax.swing.JButton addUser_btn;
    private javax.swing.JTextField address_txt;
    private javax.swing.JCheckBox autoGenerateBarcode_rbt;
    private javax.swing.JTextField barcodeIN_txt;
    private javax.swing.JLabel barcodeOUTGen_lbl;
    private javax.swing.JTextField barcodeOUT_txt;
    private javax.swing.JPanel barcodePanel;
    private javax.swing.JTextField barcode_txt;
    private javax.swing.JLabel barcodeinGen_lbl;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JPanel cartPanel;
    private javax.swing.JComboBox<String> cashierSalesHistory_cmb;
    private javax.swing.JPanel categoriesPanel;
    private javax.swing.JTextField categoriesSearch_txt;
    private javax.swing.JLabel categoryMovement_lbl;
    private javax.swing.JTextField categoryName_txt;
    private javax.swing.JComboBox<String> categoryProduct_cmb;
    private javax.swing.JComboBox<String> categoryReport_cmb;
    private javax.swing.JComboBox<String> category_cmb;
    private javax.swing.JButton chooseImage_btn;
    private javax.swing.JButton clearCategory_btn;
    private javax.swing.JButton clearInv_btn;
    private javax.swing.JButton clearProduct_btn;
    private javax.swing.JButton clear_btn;
    private javax.swing.JButton clearsalesHistory_btn;
    private javax.swing.JPasswordField confirmPass_txt;
    private javax.swing.JTextField contactNumber_txt;
    private javax.swing.JTextField contactPerson_txt;
    private javax.swing.JPanel contentPanel;
    private javax.swing.JTextField costPrice_txt;
    private javax.swing.JTextField createdAt_txt1;
    private javax.swing.JLabel currentStockOUT_lbl;
    private javax.swing.JLabel currentStock_lbl;
    private javax.swing.JTable dashboard_tbl;
    private javax.swing.JLabel dateTime_lbl;
    private javax.swing.JButton deleteCategory_btn;
    private javax.swing.JButton deleteProduct_btn;
    private javax.swing.JButton deleteSupplier_btn;
    private javax.swing.JButton delete_btn;
    private javax.swing.JTextField email_txt;
    private com.toedter.calendar.JDateChooser endDateReport_txt;
    private com.toedter.calendar.JDateChooser endDateSalesHistory_txt;
    private javax.swing.JButton exportExel_btn;
    private javax.swing.JButton exportPdf_btn;
    private javax.swing.JTextField firstname_txt1;
    private javax.swing.JLabel fullnameofUser_lbl;
    private javax.swing.JButton generateReport_btn;
    private javax.swing.JPanel headerPanel;
    private javax.swing.JPanel imagePanel;
    private javax.swing.JPanel inventoryPanel;
    private javax.swing.JLabel itemSold_lbl;
    private com.toedter.calendar.JDateChooser jDateChooser1;
    private com.toedter.calendar.JDateChooser jDateChooser2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel100;
    private javax.swing.JLabel jLabel101;
    private javax.swing.JLabel jLabel102;
    private javax.swing.JLabel jLabel103;
    private javax.swing.JLabel jLabel104;
    private javax.swing.JLabel jLabel105;
    private javax.swing.JLabel jLabel106;
    private javax.swing.JLabel jLabel107;
    private javax.swing.JLabel jLabel108;
    private javax.swing.JLabel jLabel109;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel110;
    private javax.swing.JLabel jLabel111;
    private javax.swing.JLabel jLabel112;
    private javax.swing.JLabel jLabel113;
    private javax.swing.JLabel jLabel114;
    private javax.swing.JLabel jLabel115;
    private javax.swing.JLabel jLabel116;
    private javax.swing.JLabel jLabel117;
    private javax.swing.JLabel jLabel118;
    private javax.swing.JLabel jLabel119;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel120;
    private javax.swing.JLabel jLabel121;
    private javax.swing.JLabel jLabel122;
    private javax.swing.JLabel jLabel123;
    private javax.swing.JLabel jLabel124;
    private javax.swing.JLabel jLabel125;
    private javax.swing.JLabel jLabel126;
    private javax.swing.JLabel jLabel127;
    private javax.swing.JLabel jLabel128;
    private javax.swing.JLabel jLabel129;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel130;
    private javax.swing.JLabel jLabel131;
    private javax.swing.JLabel jLabel132;
    private javax.swing.JLabel jLabel133;
    private javax.swing.JLabel jLabel134;
    private javax.swing.JLabel jLabel135;
    private javax.swing.JLabel jLabel136;
    private javax.swing.JLabel jLabel137;
    private javax.swing.JLabel jLabel138;
    private javax.swing.JLabel jLabel139;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel140;
    private javax.swing.JLabel jLabel141;
    private javax.swing.JLabel jLabel142;
    private javax.swing.JLabel jLabel143;
    private javax.swing.JLabel jLabel144;
    private javax.swing.JLabel jLabel145;
    private javax.swing.JLabel jLabel146;
    private javax.swing.JLabel jLabel147;
    private javax.swing.JLabel jLabel148;
    private javax.swing.JLabel jLabel149;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel150;
    private javax.swing.JLabel jLabel151;
    private javax.swing.JLabel jLabel152;
    private javax.swing.JLabel jLabel153;
    private javax.swing.JLabel jLabel154;
    private javax.swing.JLabel jLabel155;
    private javax.swing.JLabel jLabel156;
    private javax.swing.JLabel jLabel157;
    private javax.swing.JLabel jLabel158;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel63;
    private javax.swing.JLabel jLabel64;
    private javax.swing.JLabel jLabel65;
    private javax.swing.JLabel jLabel66;
    private javax.swing.JLabel jLabel67;
    private javax.swing.JLabel jLabel68;
    private javax.swing.JLabel jLabel69;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel70;
    private javax.swing.JLabel jLabel71;
    private javax.swing.JLabel jLabel72;
    private javax.swing.JLabel jLabel73;
    private javax.swing.JLabel jLabel74;
    private javax.swing.JLabel jLabel75;
    private javax.swing.JLabel jLabel76;
    private javax.swing.JLabel jLabel77;
    private javax.swing.JLabel jLabel78;
    private javax.swing.JLabel jLabel79;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel80;
    private javax.swing.JLabel jLabel81;
    private javax.swing.JLabel jLabel82;
    private javax.swing.JLabel jLabel83;
    private javax.swing.JLabel jLabel84;
    private javax.swing.JLabel jLabel85;
    private javax.swing.JLabel jLabel86;
    private javax.swing.JLabel jLabel87;
    private javax.swing.JLabel jLabel88;
    private javax.swing.JLabel jLabel89;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabel90;
    private javax.swing.JLabel jLabel91;
    private javax.swing.JLabel jLabel92;
    private javax.swing.JLabel jLabel93;
    private javax.swing.JLabel jLabel94;
    private javax.swing.JLabel jLabel95;
    private javax.swing.JLabel jLabel96;
    private javax.swing.JLabel jLabel97;
    private javax.swing.JLabel jLabel98;
    private javax.swing.JLabel jLabel99;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel29;
    private javax.swing.JPanel jPanel30;
    private javax.swing.JPanel jPanel31;
    private javax.swing.JPanel jPanel32;
    private javax.swing.JPanel jPanel33;
    private javax.swing.JPanel jPanel34;
    private javax.swing.JPanel jPanel35;
    private javax.swing.JPanel jPanel36;
    private javax.swing.JPanel jPanel37;
    private javax.swing.JPanel jPanel38;
    private javax.swing.JPanel jPanel39;
    private javax.swing.JPanel jPanel40;
    private javax.swing.JPanel jPanel41;
    private javax.swing.JPanel jPanel42;
    private javax.swing.JPanel jPanel43;
    private javax.swing.JPanel jPanel44;
    private javax.swing.JPanel jPanel45;
    private javax.swing.JPanel jPanel46;
    private javax.swing.JPanel jPanel47;
    private javax.swing.JPanel jPanel48;
    private javax.swing.JPanel jPanel49;
    private javax.swing.JPanel jPanel50;
    private javax.swing.JPanel jPanel51;
    private javax.swing.JPanel jPanel52;
    private javax.swing.JPanel jPanel54;
    private javax.swing.JPanel jPanel55;
    private javax.swing.JPanel jPanel56;
    private javax.swing.JPanel jPanel57;
    private javax.swing.JPanel jPanel58;
    private javax.swing.JPanel jPanel59;
    private javax.swing.JPanel jPanel60;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane11;
    private javax.swing.JScrollPane jScrollPane12;
    private javax.swing.JScrollPane jScrollPane13;
    private javax.swing.JScrollPane jScrollPane14;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JPanel jtablePanel;
    private javax.swing.JTextField lastname_txt;
    private javax.swing.JButton logout_btn;
    private javax.swing.JPanel lowStock_Panel;
    private javax.swing.JLabel lowstockDashboad_lbl;
    private javax.swing.JTextField middlename_txt;
    private javax.swing.JLabel onlineOffline_lbl;
    private javax.swing.JPanel outOfStock_Panel;
    private javax.swing.JLabel outOfStockdashboard_lbl;
    private javax.swing.JToggleButton passwordToggle_btn;
    private javax.swing.JPasswordField password_txt;
    private javax.swing.JComboBox<String> paymentMethodSAlesHistory_cmb;
    private javax.swing.JPanel paymentPanel;
    private javax.swing.JComboBox<String> payment_cmb;
    private javax.swing.JPanel pieChart_panel;
    private javax.swing.JPanel posPanel;
    private javax.swing.JTextField productName_txt;
    private javax.swing.JComboBox<String> productStockIN_cmb;
    private javax.swing.JTable product_tbl;
    private javax.swing.JPanel productsPanel;
    private javax.swing.JPanel products_panel;
    private javax.swing.JTextField quantityIn_txt;
    private javax.swing.JTextField quantityOUT_txt;
    private javax.swing.JComboBox<String> reason_cmb;
    private javax.swing.JTextArea remarksIn_txt;
    private javax.swing.JTextArea remarksOUT_txt;
    private javax.swing.JButton removeImage_btn;
    private javax.swing.JLabel reorderLevel_lbl;
    private javax.swing.JTextField reorderLevel_txt;
    private javax.swing.JTable report_tbl;
    private javax.swing.JPanel reportsPanel;
    private javax.swing.JLabel roleUser_lbl;
    private javax.swing.JComboBox<String> role_cmb;
    private javax.swing.JComboBox<String> role_cmb1;
    private javax.swing.JPanel salesHistoryPanel;
    private javax.swing.JTable salesList_tbl;
    private javax.swing.JPanel salesPanel;
    private javax.swing.JButton saveInventory_btn;
    private javax.swing.JTextField searchIN_txt;
    private javax.swing.JTextField searchOUT_txt;
    private javax.swing.JTextField searchProduct_txt;
    private javax.swing.JButton searchSalesHistory_btn;
    private javax.swing.JTextField searchUser_txt;
    private javax.swing.JTable selectedSaleItems_tbl;
    private javax.swing.JTextField sellingPrice_txt;
    private com.toedter.calendar.JDateChooser startDateRepor_txt;
    private javax.swing.JTextField statusIdentifierOUT_txt;
    private javax.swing.JTextField statusIdentifier_txt;
    private javax.swing.JLabel statusMovement_lbl;
    private javax.swing.JComboBox<String> statusProduct_cmb;
    private javax.swing.JComboBox<String> status_cmb;
    private javax.swing.JTextField stockMovementSearch_txt;
    private javax.swing.JComboBox<String> stockMovementType_cmb;
    private javax.swing.JTable stockMovement_tbl;
    private javax.swing.JPanel stockOutJ;
    private javax.swing.JButton stockin_btn;
    private javax.swing.JButton stockmovement_btn;
    private javax.swing.JButton stockout_btn;
    private javax.swing.JTextField stockquantity_txt;
    private javax.swing.JComboBox<String> stocksType_cmb;
    private javax.swing.JTextField supplierIN_btn;
    private javax.swing.JLabel supplierMovement_lbl;
    private javax.swing.JTextField supplierName_txt;
    private javax.swing.JPanel supplierPanel;
    private javax.swing.JComboBox<String> supplierProduct_cmb;
    private javax.swing.JComboBox<String> supplierReport_cmb;
    private javax.swing.JTextField supplierSearch_txt;
    private javax.swing.JComboBox<String> supplier_cmb;
    private javax.swing.JTable supplier_tbl;
    private javax.swing.JPanel suppliers_panel;
    private javax.swing.JPanel totalCategory_Panel;
    private javax.swing.JLabel totalCategory_lbl;
    private javax.swing.JLabel totalDiscount_lbl;
    private javax.swing.JLabel totalProduct_lbl;
    private javax.swing.JLabel totalSalesDashboard;
    private javax.swing.JPanel totalSales_Panel;
    private javax.swing.JLabel totalSales_lbl;
    private javax.swing.JLabel totalSupplier_lbl;
    private javax.swing.JLabel totalTransaction_lbl;
    private javax.swing.JLabel totalUser_lbl;
    private javax.swing.JLabel transactionHsitory_lbl;
    private javax.swing.JPanel transaction_panel;
    private javax.swing.JComboBox<String> typeIn_cmb;
    private javax.swing.JButton updateCategory_btn;
    private javax.swing.JButton updateProduct_btn;
    private javax.swing.JButton update_btn;
    private javax.swing.JTextField updatedat_txt1;
    private javax.swing.JTextField userID_txt;
    private javax.swing.JLabel userNameWelcome_lbl;
    private javax.swing.JPanel userPanel;
    private javax.swing.JTable user_tbl;
    private javax.swing.JTextField username_txt;
    private javax.swing.JPanel users_panel;
    // End of variables declaration//GEN-END:variables
}
