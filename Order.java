import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.border.TitledBorder;
/**
 *
 * @author Administrator
 */
public class Order extends javax.swing.JFrame {
    
    public Order() {
        initComponents();
        setupComponents();
    }
    
   private void setupComponents() {
        loadProductTable();
        setupActionListeners();
        setupNavigation();
        initializeCartTable();
    }
    
    private void setupActionListeners() {
        AddtoCartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addToCart();
            }
        });
        
        SaveOrderDetailsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveOrder();
            }
        });
        
        ResetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetForm();
            }
        });
        
        // Product selection listener
        ProductListTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedProduct();
            }
        });
    }
    
        private void setupNavigation() {
            GoToUser.addActionListener(e -> {
                new User().setVisible(true);
                this.dispose();
            });

            GoToCategory.addActionListener(e -> {
                new Category().setVisible(true);
                this.dispose();
            });

            GoToProduct.addActionListener(e -> {
                new Product().setVisible(true);
                this.dispose();
            });

            GoToOrder.addActionListener(e -> {
                new Order().setVisible(true);
                this.dispose();
            });
        }
    
    private void initializeCartTable() {
        DefaultTableModel cartModel = new DefaultTableModel();
        cartModel.setColumnIdentifiers(new String[]{"ID", "Name", "Quantity", "Price", "Total", "Category"});
        CartTable.setModel(cartModel);
    }
    
    private void loadProductTable() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            try {
                String query = "SELECT p.id, p.name, p.quantity, p.price, p.description, " +
                              "COALESCE(c.name, 'No Category') as category " +
                              "FROM products p LEFT JOIN categories c ON p.category_id = c.id " +
                              "WHERE p.quantity > 0 ORDER BY p.name";
                PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery();

                DefaultTableModel model = new DefaultTableModel();
                model.setColumnIdentifiers(new String[]{"ID", "Name", "Quantity", "Price", "Description", "Category"});

                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        rs.getString("description"),
                        rs.getString("category")
                    });
                }

                ProductListTable.setModel(model);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage(), 
                                            "Database Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                DatabaseConnection.closeConnection(conn);
            }
        }
    }
    
   private void loadSelectedProduct() {
        int selectedRow = ProductListTable.getSelectedRow();
        if (selectedRow >= 0) {
            ProductNameTextfield.setText(ProductListTable.getValueAt(selectedRow, 1).toString());
            ProductPriceTextfield.setText(ProductListTable.getValueAt(selectedRow, 3).toString());
            ProductDescriptionTextfield.setText(ProductListTable.getValueAt(selectedRow, 4).toString());
            OrderQuantityTextfield.setText("1");
        }
    }
    
    private void addToCart() {
        int selectedRow = ProductListTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a product", 
                                        "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String quantityStr = OrderQuantityTextfield.getText().trim();
        if (quantityStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter quantity", 
                                        "Missing Quantity", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            int orderQuantity = Integer.parseInt(quantityStr);
            if (orderQuantity <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than 0", 
                                            "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int availableQuantity = Integer.parseInt(ProductListTable.getValueAt(selectedRow, 2).toString());
            
            if (orderQuantity > availableQuantity) {
                JOptionPane.showMessageDialog(this, "Not enough stock available. Available: " + availableQuantity, 
                                            "Insufficient Stock", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            DefaultTableModel cartModel = (DefaultTableModel) CartTable.getModel();
            
            Object[] rowData = {
                ProductListTable.getValueAt(selectedRow, 0), // ID
                ProductListTable.getValueAt(selectedRow, 1), // Name
                orderQuantity, // Quantity
                ProductListTable.getValueAt(selectedRow, 3), // Price
                orderQuantity * Double.parseDouble(ProductListTable.getValueAt(selectedRow, 3).toString()), // Total
                ProductListTable.getValueAt(selectedRow, 5)  // Category
            };
            
            cartModel.addRow(rowData);
            resetForm();
            JOptionPane.showMessageDialog(this, "Product added to cart successfully!", 
                                        "Success", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid quantity", 
                                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Save order to database and update product quantities
     */
    private void saveOrder() {
        DefaultTableModel cartModel = (DefaultTableModel) CartTable.getModel();
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart is empty", 
                                        "Empty Cart", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            try {
                conn.setAutoCommit(false);
                
                for (int i = 0; i < cartModel.getRowCount(); i++) {
                    int productId = Integer.parseInt(cartModel.getValueAt(i, 0).toString());
                    int quantity = Integer.parseInt(cartModel.getValueAt(i, 2).toString());
                    double total = Double.parseDouble(cartModel.getValueAt(i, 4).toString());
                    
                    // Insert order
                    String orderQuery = "INSERT INTO orders (product_id, quantity, total_price, order_date) VALUES (?, ?, ?, NOW())";
                    PreparedStatement orderStmt = conn.prepareStatement(orderQuery);
                    orderStmt.setInt(1, productId);
                    orderStmt.setInt(2, quantity);
                    orderStmt.setDouble(3, total);
                    orderStmt.executeUpdate();
                    
                    // Update product quantity
                    String updateQuery = "UPDATE products SET quantity = quantity - ? WHERE id = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                    updateStmt.setInt(1, quantity);
                    updateStmt.setInt(2, productId);
                    updateStmt.executeUpdate();
                }
                
                conn.commit();
                JOptionPane.showMessageDialog(this, "Order saved successfully!", 
                                            "Success", JOptionPane.INFORMATION_MESSAGE);
                
                // Clear cart and refresh product table
                cartModel.setRowCount(0);
                loadProductTable();
                
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                JOptionPane.showMessageDialog(this, "Error saving order: " + e.getMessage(), 
                                            "Database Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                DatabaseConnection.closeConnection(conn);
            }
        }
    }
    
    /**
     * Reset form fields to empty state
     */
    private void resetForm() {
        ProductNameTextfield.setText("");
        ProductPriceTextfield.setText("");
        ProductDescriptionTextfield.setText("");
        OrderQuantityTextfield.setText("");
        ProductListTable.clearSelection();
    }
    
    /**
     * Generate and print purchase order receipt
     */
    private void printPurchaseOrder() {
        DefaultTableModel cartModel = (DefaultTableModel) CartTable.getModel();
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart is empty. Cannot print receipt.", 
                                        "Empty Cart", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Create receipt dialog
        JDialog receiptDialog = new JDialog(this, "Purchase Order Receipt", true);
        receiptDialog.setSize(600, 800);
        receiptDialog.setLocationRelativeTo(this);
        
        // Create receipt content
        JPanel receiptPanel = createReceiptPanel(cartModel);
        JScrollPane scrollPane = new JScrollPane(receiptPanel);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton printButton = new JButton("Print Receipt");
        JButton closeButton = new JButton("Close");
        
        printButton.addActionListener(e -> {
            printReceipt(receiptPanel);
        });
        
        closeButton.addActionListener(e -> receiptDialog.dispose());
        
        buttonPanel.add(printButton);
        buttonPanel.add(closeButton);
        
        receiptDialog.setLayout(new BorderLayout());
        receiptDialog.add(scrollPane, BorderLayout.CENTER);
        receiptDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        receiptDialog.setVisible(true);
    }

    /**
     * Create the receipt panel with all order details
     */
    private JPanel createReceiptPanel(DefaultTableModel cartModel) {
        JPanel receiptPanel = new JPanel();
        receiptPanel.setLayout(new BoxLayout(receiptPanel, BoxLayout.Y_AXIS));
        receiptPanel.setBackground(Color.WHITE);
        receiptPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JLabel titleLabel = new JLabel("PURCHASE ORDER RECEIPT", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        receiptPanel.add(titleLabel);
        
        receiptPanel.add(Box.createVerticalStrut(10));
        
        // Company Info
        JPanel companyPanel = new JPanel(new GridLayout(3, 1));
        companyPanel.setBorder(new TitledBorder("Company Information"));
        companyPanel.add(new JLabel("School Warehouse System"));
        companyPanel.add(new JLabel("Address: 123 Education Street, Learning City"));
        companyPanel.add(new JLabel("Phone: (555) 123-4567 | Email: info@schoolwarehouse.com"));
        receiptPanel.add(companyPanel);
        
        receiptPanel.add(Box.createVerticalStrut(15));
        
        // Order Info
        JPanel orderInfoPanel = new JPanel(new GridLayout(3, 2));
        orderInfoPanel.setBorder(new TitledBorder("Order Information"));
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDate = dateFormat.format(new Date());
        String orderNumber = "PO-" + System.currentTimeMillis();
        
        orderInfoPanel.add(new JLabel("Order Number:"));
        orderInfoPanel.add(new JLabel(orderNumber));
        orderInfoPanel.add(new JLabel("Order Date:"));
        orderInfoPanel.add(new JLabel(currentDate));
        orderInfoPanel.add(new JLabel("Status:"));
        orderInfoPanel.add(new JLabel("Pending"));
        
        receiptPanel.add(orderInfoPanel);
        
        receiptPanel.add(Box.createVerticalStrut(15));
        
        // Items Table
        JPanel itemsPanel = new JPanel(new BorderLayout());
        itemsPanel.setBorder(new TitledBorder("Order Items"));
        
        String[] columns = {"Item", "Quantity", "Unit Price", "Total"};
        Object[][] data = new Object[cartModel.getRowCount()][4];
        double grandTotal = 0.0;
        
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            data[i][0] = cartModel.getValueAt(i, 1); // Name
            data[i][1] = cartModel.getValueAt(i, 2); // Quantity
            data[i][2] = String.format("$%.2f", Double.parseDouble(cartModel.getValueAt(i, 3).toString())); // Price
            double total = Double.parseDouble(cartModel.getValueAt(i, 4).toString());
            data[i][3] = String.format("$%.2f", total); // Total
            grandTotal += total;
        }
        
        JTable itemsTable = new JTable(data, columns);
        itemsTable.setEnabled(false);
        itemsTable.setRowHeight(25);
        itemsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        JScrollPane tableScrollPane = new JScrollPane(itemsTable);
        tableScrollPane.setPreferredSize(new Dimension(500, 200));
        itemsPanel.add(tableScrollPane, BorderLayout.CENTER);
        
        receiptPanel.add(itemsPanel);
        
        receiptPanel.add(Box.createVerticalStrut(15));
        
        // Total Section
        JPanel totalPanel = new JPanel(new GridLayout(4, 2));
        totalPanel.setBorder(new TitledBorder("Order Summary"));
        
        double subtotal = grandTotal;
        double tax = subtotal * 0.08; // 8% tax
        double finalTotal = subtotal + tax;
        
        totalPanel.add(new JLabel("Subtotal:"));
        totalPanel.add(new JLabel(String.format("$%.2f", subtotal)));
        totalPanel.add(new JLabel("Tax (8%):"));
        totalPanel.add(new JLabel(String.format("$%.2f", tax)));
        totalPanel.add(new JLabel(""));
        totalPanel.add(new JLabel(""));
        
        JLabel totalLabel = new JLabel("TOTAL:");
        JLabel totalAmountLabel = new JLabel(String.format("$%.2f", finalTotal));
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalAmountLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        totalPanel.add(totalLabel);
        totalPanel.add(totalAmountLabel);
        
        receiptPanel.add(totalPanel);
        
        receiptPanel.add(Box.createVerticalStrut(20));
        
        // Footer
        JPanel footerPanel = new JPanel(new GridLayout(3, 1));
        footerPanel.setBorder(new TitledBorder("Terms & Conditions"));
        footerPanel.add(new JLabel("• Payment due within 30 days of receipt"));
        footerPanel.add(new JLabel("• All items subject to availability"));
        footerPanel.add(new JLabel("• Thank you for your business!"));
        
        receiptPanel.add(footerPanel);
        
        return receiptPanel;
    }

    /**
     * Print the receipt using Java's printing API
     */
    private void printReceipt(JPanel receiptPanel) {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        
        printerJob.setPrintable(new Printable() {
            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                if (pageIndex > 0) {
                    return NO_SUCH_PAGE;
                }
                
                Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                
                // Scale to fit page
                double scaleX = pageFormat.getImageableWidth() / receiptPanel.getWidth();
                double scaleY = pageFormat.getImageableHeight() / receiptPanel.getHeight();
                double scale = Math.min(scaleX, scaleY);
                
                g2d.scale(scale, scale);
                
                // Print the panel
                receiptPanel.printAll(graphics);
                
                return PAGE_EXISTS;
            }
        });
        
        // Show print dialog
        if (printerJob.printDialog()) {
            try {
                printerJob.print();
                JOptionPane.showMessageDialog(this, "Receipt printed successfully!", 
                                            "Print Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(this, "Error printing receipt: " + e.getMessage(), 
                                            "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Save receipt as PDF (alternative method)
     */
    private void saveReceiptAsPDF() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Receipt as PDF");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(java.io.File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".pdf");
            }
            
            @Override
            public String getDescription() {
                return "PDF Files (*.pdf)";
            }
        });
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
                fileToSave = new java.io.File(fileToSave.getAbsolutePath() + ".pdf");
            }
            
            JOptionPane.showMessageDialog(this, 
                "PDF save functionality requires additional libraries (iText, etc.)\n" +
                "For now, please use the Print option to print to PDF printer.", 
                "PDF Save", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
  

    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        BackgroundPanel = new javax.swing.JPanel();
        MainPanel = new javax.swing.JPanel();
        HeaderLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        ProductListTable = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        CartTable = new javax.swing.JTable();
        ProductListTableLabel = new javax.swing.JLabel();
        CartTableLable = new javax.swing.JLabel();
        Header2Label = new javax.swing.JLabel();
        ProductNameLabel = new javax.swing.JLabel();
        ProductPriceLabel = new javax.swing.JLabel();
        ProductDescriptionLabel = new javax.swing.JLabel();
        OrderQuantityLabel = new javax.swing.JLabel();
        ProductNameTextfield = new javax.swing.JTextField();
        ProductPriceTextfield = new javax.swing.JTextField();
        ProductDescriptionTextfield = new javax.swing.JTextField();
        OrderQuantityTextfield = new javax.swing.JTextField();
        SaveOrderDetailsButton = new javax.swing.JButton();
        PrintPurchaseOrder = new javax.swing.JButton();
        ResetButton = new javax.swing.JButton();
        PrintLabel = new javax.swing.JLabel();
        AddtoCartButton = new javax.swing.JButton();
        GoToUser = new javax.swing.JButton();
        GoToOrder = new javax.swing.JButton();
        GoToProduct = new javax.swing.JButton();
        GoToCategory = new javax.swing.JButton();
        GoToDashboard = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1366, 768));
        setName("Order"); // NOI18N

        BackgroundPanel.setBackground(new java.awt.Color(24, 65, 127));
        BackgroundPanel.setMaximumSize(new java.awt.Dimension(1366, 768));
        BackgroundPanel.setMinimumSize(new java.awt.Dimension(1366, 768));

        MainPanel.setBackground(new java.awt.Color(255, 255, 255));
        MainPanel.setMaximumSize(new java.awt.Dimension(1050, 570));
        MainPanel.setMinimumSize(new java.awt.Dimension(1050, 570));

        HeaderLabel.setFont(new java.awt.Font("Segoe UI", 1, 48)); // NOI18N
        HeaderLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        HeaderLabel.setText("Manage Order");

        ProductListTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Name", "Quantity", "Price", "Description", "Category"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(ProductListTable);
        if (ProductListTable.getColumnModel().getColumnCount() > 0) {
            ProductListTable.getColumnModel().getColumn(1).setResizable(false);
            ProductListTable.getColumnModel().getColumn(2).setResizable(false);
            ProductListTable.getColumnModel().getColumn(3).setResizable(false);
            ProductListTable.getColumnModel().getColumn(4).setResizable(false);
            ProductListTable.getColumnModel().getColumn(5).setResizable(false);
        }

        CartTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Name", "Quantity", "Price", "Description", "Category"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(CartTable);
        if (CartTable.getColumnModel().getColumnCount() > 0) {
            CartTable.getColumnModel().getColumn(0).setResizable(false);
            CartTable.getColumnModel().getColumn(1).setResizable(false);
            CartTable.getColumnModel().getColumn(2).setResizable(false);
            CartTable.getColumnModel().getColumn(3).setResizable(false);
            CartTable.getColumnModel().getColumn(4).setResizable(false);
            CartTable.getColumnModel().getColumn(5).setResizable(false);
        }

        ProductListTableLabel.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        ProductListTableLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ProductListTableLabel.setText("Product List");

        CartTableLable.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        CartTableLable.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        CartTableLable.setText("Cart");

        Header2Label.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        Header2Label.setText("Select Product :");

        ProductNameLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        ProductNameLabel.setText("Product Name");

        ProductPriceLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        ProductPriceLabel.setText("Product Price");

        ProductDescriptionLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        ProductDescriptionLabel.setText("Product Description");

        OrderQuantityLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        OrderQuantityLabel.setText("Order Quantity");

        SaveOrderDetailsButton.setText("Save Order Details");

        PrintPurchaseOrder.setText("Purchase Order");
        PrintPurchaseOrder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PrintPurchaseOrderActionPerformed(evt);
            }
        });

        ResetButton.setText("Reset");

        PrintLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        PrintLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        PrintLabel.setText("Print ");

        AddtoCartButton.setText("Add to cart");

        javax.swing.GroupLayout MainPanelLayout = new javax.swing.GroupLayout(MainPanel);
        MainPanel.setLayout(MainPanelLayout);
        MainPanelLayout.setHorizontalGroup(
            MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MainPanelLayout.createSequentialGroup()
                .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, MainPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(HeaderLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 520, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(MainPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Header2Label, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(ProductNameLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(OrderQuantityLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(ProductDescriptionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(ProductPriceLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(MainPanelLayout.createSequentialGroup()
                                .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(ProductDescriptionTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(OrderQuantityTextfield)
                                        .addGroup(MainPanelLayout.createSequentialGroup()
                                            .addComponent(AddtoCartButton, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(SaveOrderDetailsButton)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(ResetButton)))
                                    .addComponent(ProductPriceTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(ProductNameTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(PrintPurchaseOrder)
                                    .addComponent(PrintLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 483, Short.MAX_VALUE)
                        .addComponent(jScrollPane1)
                        .addComponent(ProductListTableLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(CartTableLable, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 483, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(41, 41, 41))
        );
        MainPanelLayout.setVerticalGroup(
            MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(MainPanelLayout.createSequentialGroup()
                        .addComponent(ProductListTableLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(MainPanelLayout.createSequentialGroup()
                        .addComponent(HeaderLabel)
                        .addGap(96, 96, 96)
                        .addComponent(Header2Label)
                        .addGap(18, 18, 18)
                        .addComponent(ProductNameLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(ProductNameTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ProductPriceLabel)
                        .addGap(5, 5, 5)))
                .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(CartTableLable, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(MainPanelLayout.createSequentialGroup()
                        .addComponent(ProductPriceTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ProductDescriptionLabel)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(MainPanelLayout.createSequentialGroup()
                        .addComponent(ProductDescriptionTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(OrderQuantityLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(OrderQuantityTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(SaveOrderDetailsButton)
                            .addComponent(ResetButton)
                            .addComponent(AddtoCartButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(PrintLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(PrintPurchaseOrder))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(35, 35, 35))
        );

        GoToUser.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        GoToUser.setText("User");

        GoToOrder.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        GoToOrder.setText("Order");
        GoToOrder.setBorder(javax.swing.BorderFactory.createMatteBorder(4, 4, 4, 4, new java.awt.Color(51, 153, 255)));
        GoToOrder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GoToOrderActionPerformed(evt);
            }
        });

        GoToProduct.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        GoToProduct.setText("Product");

        GoToCategory.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        GoToCategory.setText("Category");

        GoToDashboard.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        GoToDashboard.setText("Dashboard");

        javax.swing.GroupLayout BackgroundPanelLayout = new javax.swing.GroupLayout(BackgroundPanel);
        BackgroundPanel.setLayout(BackgroundPanelLayout);
        BackgroundPanelLayout.setHorizontalGroup(
            BackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, BackgroundPanelLayout.createSequentialGroup()
                .addGap(110, 110, 110)
                .addGroup(BackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(GoToDashboard, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
                    .addComponent(GoToOrder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(GoToCategory, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
                    .addComponent(GoToProduct, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(GoToUser, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(MainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(73, 73, 73))
        );
        BackgroundPanelLayout.setVerticalGroup(
            BackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(BackgroundPanelLayout.createSequentialGroup()
                .addGap(75, 75, 75)
                .addGroup(BackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(MainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(BackgroundPanelLayout.createSequentialGroup()
                        .addComponent(GoToDashboard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(GoToUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(GoToCategory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(15, 15, 15)
                        .addComponent(GoToProduct, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(GoToOrder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(90, 90, 90))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BackgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BackgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void GoToOrderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GoToOrderActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_GoToOrderActionPerformed

    private void PrintPurchaseOrderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PrintPurchaseOrderActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_PrintPurchaseOrderActionPerformed


    
    /**
     * @param args the command line arguments
     */
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
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Order.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Order.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Order.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Order.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Order().setVisible(true);
            }
        });
    }
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AddtoCartButton;
    private javax.swing.JPanel BackgroundPanel;
    private javax.swing.JTable CartTable;
    private javax.swing.JLabel CartTableLable;
    private javax.swing.JButton GoToCategory;
    private javax.swing.JButton GoToDashboard;
    private javax.swing.JButton GoToOrder;
    private javax.swing.JButton GoToProduct;
    private javax.swing.JButton GoToUser;
    private javax.swing.JLabel Header2Label;
    private javax.swing.JLabel HeaderLabel;
    private javax.swing.JPanel MainPanel;
    private javax.swing.JLabel OrderQuantityLabel;
    private javax.swing.JTextField OrderQuantityTextfield;
    private javax.swing.JLabel PrintLabel;
    private javax.swing.JButton PrintPurchaseOrder;
    private javax.swing.JLabel ProductDescriptionLabel;
    private javax.swing.JTextField ProductDescriptionTextfield;
    private javax.swing.JTable ProductListTable;
    private javax.swing.JLabel ProductListTableLabel;
    private javax.swing.JLabel ProductNameLabel;
    private javax.swing.JTextField ProductNameTextfield;
    private javax.swing.JLabel ProductPriceLabel;
    private javax.swing.JTextField ProductPriceTextfield;
    private javax.swing.JButton ResetButton;
    private javax.swing.JButton SaveOrderDetailsButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    // End of variables declaration//GEN-END:variables
}