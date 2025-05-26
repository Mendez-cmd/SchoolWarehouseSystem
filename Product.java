
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;


public class Product extends javax.swing.JFrame  {

    public Product() {
        initComponents();
        loadCategories();
        loadProductTable();
        setupActionListeners();
        setupNavigation();
    }
    
    private void setupActionListeners() {
        SaveButton2.addActionListener(e -> saveProduct());
        ResetButton2.addActionListener(e -> resetForm());
        CloseButton2.addActionListener(e -> this.dispose());
        UpdateButton2.addActionListener(e -> updateProduct());
        
        // Table selection listener
        jTable1.getSelectionModel().addListSelectionListener(e -> {
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
    
    private void loadSelectedProduct() {
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow >= 0) {
            ProductNameTextfield.setText(jTable1.getValueAt(selectedRow, 1).toString());
            ProductAddQuantityTextfield.setText(jTable1.getValueAt(selectedRow, 2).toString());
            ProductPriceTextfield.setText(jTable1.getValueAt(selectedRow, 3).toString());
            ProductDescriptionTextField.setText(jTable1.getValueAt(selectedRow, 4).toString());
            
            // Set category in combo box
            String category = jTable1.getValueAt(selectedRow, 5).toString();
            for (int i = 0; i < ProductCategoryComboBox.getItemCount(); i++) {
                if (ProductCategoryComboBox.getItemAt(i).contains(category)) {
                    ProductCategoryComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
    
    private void updateProduct() {
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a product to update");
            return;
        }
        
        String name = ProductNameTextfield.getText().trim();
        String quantityStr = ProductAddQuantityTextfield.getText().trim();
        String priceStr = ProductPriceTextfield.getText().trim();
        String description = ProductDescriptionTextField.getText().trim();
        String categorySelection = (String) ProductCategoryComboBox.getSelectedItem();

        if (name.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty() ||
            categorySelection == null || categorySelection.equals("Select Category")) {
            JOptionPane.showMessageDialog(this, "Please fill all fields");
            return;
        }

        try {
            int productId = Integer.parseInt(jTable1.getValueAt(selectedRow, 0).toString());
            int quantity = Integer.parseInt(quantityStr);
            double price = Double.parseDouble(priceStr);
            int categoryId = Integer.parseInt(categorySelection.split(" - ")[0]);

            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                try {
                    String query = "UPDATE products SET name=?, quantity=?, price=?, description=?, category_id=? WHERE id=?";
                    PreparedStatement stmt = conn.prepareStatement(query);
                    stmt.setString(1, name);
                    stmt.setInt(2, quantity);
                    stmt.setDouble(3, price);
                    stmt.setString(4, description);
                    stmt.setInt(5, categoryId);
                    stmt.setInt(6, productId);

                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        JOptionPane.showMessageDialog(this, "Product updated successfully!");
                        resetForm();
                        loadProductTable();
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error updating product: " + e.getMessage());
                } finally {
                    DatabaseConnection.closeConnection(conn);
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for quantity and price");
        }
    }

    private void loadProductTable() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            try {
                String query = "SELECT p.id, p.name, p.quantity, p.price, p.description, COALESCE(c.name, 'No Category') as category " +
                              "FROM products p LEFT JOIN categories c ON p.category_id = c.id ORDER BY p.name";
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

                jTable1.setModel(model);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage());
            } finally {
                DatabaseConnection.closeConnection(conn);
            }
        }
    }

    private void loadCategories() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            try {
                String query = "SELECT id, name FROM categories ORDER BY name";
                PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery();

                ProductCategoryComboBox.removeAllItems();
                ProductCategoryComboBox.addItem("Select Category");

                while (rs.next()) {
                    ProductCategoryComboBox.addItem(rs.getInt("id") + " - " + rs.getString("name"));
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error loading categories: " + e.getMessage());
            } finally {
                DatabaseConnection.closeConnection(conn);
            }
        }
    }

    private void saveProduct() {
        String name = ProductNameTextfield.getText().trim();
        String quantityStr = ProductAddQuantityTextfield.getText().trim();
        String priceStr = ProductPriceTextfield.getText().trim();
        String description = ProductDescriptionTextField.getText().trim();
        String categorySelection = (String) ProductCategoryComboBox.getSelectedItem();

        if (name.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty() ||
            categorySelection == null || categorySelection.equals("Select Category")) {
            JOptionPane.showMessageDialog(this, "Please fill all fields");
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);
            double price = Double.parseDouble(priceStr);
            int categoryId = Integer.parseInt(categorySelection.split(" - ")[0]);

            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                try {
                    String query = "INSERT INTO products (name, quantity, price, description, category_id) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement stmt = conn.prepareStatement(query);
                    stmt.setString(1, name);
                    stmt.setInt(2, quantity);
                    stmt.setDouble(3, price);
                    stmt.setString(4, description);
                    stmt.setInt(5, categoryId);

                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        JOptionPane.showMessageDialog(this, "Product saved successfully!");
                        resetForm();
                        loadProductTable();
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error saving product: " + e.getMessage());
                } finally {
                    DatabaseConnection.closeConnection(conn);
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for quantity and price");
        }
    }

    private void resetForm() {
        ProductNameTextfield.setText("");
        ProductAddQuantityTextfield.setText("");
        ProductPriceTextfield.setText("");
        ProductDescriptionTextField.setText("");
        ProductCategoryComboBox.setSelectedIndex(0);
        jTable1.clearSelection();
    }
    
    private javax.swing.JTextArea ProductDescriptionTextfield;


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
        jTable1 = new javax.swing.JTable();
        ProductPriceLabel = new javax.swing.JLabel();
        ProductCategoryComboBox = new javax.swing.JComboBox<>();
        ProductDescriptionLabel = new javax.swing.JLabel();
        ProductAddQuantityLabel = new javax.swing.JLabel();
        ProductCategoryLabel = new javax.swing.JLabel();
        NameLabel2 = new javax.swing.JLabel();
        ProductAddQuantityTextfield = new javax.swing.JTextField();
        ProductPriceTextfield = new javax.swing.JTextField();
        ProductNameTextfield = new javax.swing.JTextField();
        CloseButton2 = new javax.swing.JButton();
        ResetButton2 = new javax.swing.JButton();
        UpdateButton2 = new javax.swing.JButton();
        SaveButton2 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        ProductDescriptionTextField = new javax.swing.JTextArea();
        AddButtonActionPerformed = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        ProductIDTextField = new javax.swing.JTextField();
        DeleteButtonActionPerformed = new javax.swing.JButton();
        ClearButtonActionPerformed = new javax.swing.JButton();
        SearchButtonActionPerformed = new javax.swing.JButton();
        GoToOrder = new javax.swing.JButton();
        GoToCategory = new javax.swing.JButton();
        GoToDashboard = new javax.swing.JButton();
        GoToUser = new javax.swing.JButton();
        GoToProduct = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1366, 768));

        BackgroundPanel.setBackground(new java.awt.Color(24, 65, 127));
        BackgroundPanel.setMaximumSize(new java.awt.Dimension(1366, 768));
        BackgroundPanel.setMinimumSize(new java.awt.Dimension(1366, 768));

        MainPanel.setBackground(new java.awt.Color(255, 255, 255));
        MainPanel.setMaximumSize(new java.awt.Dimension(1050, 570));
        MainPanel.setMinimumSize(new java.awt.Dimension(1050, 570));

        HeaderLabel.setFont(new java.awt.Font("Segoe UI", 1, 48)); // NOI18N
        HeaderLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        HeaderLabel.setText("Manage Product");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
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
                false, false, false, false, true, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setResizable(false);
            jTable1.getColumnModel().getColumn(1).setResizable(false);
            jTable1.getColumnModel().getColumn(2).setResizable(false);
            jTable1.getColumnModel().getColumn(3).setResizable(false);
            jTable1.getColumnModel().getColumn(4).setResizable(false);
            jTable1.getColumnModel().getColumn(5).setResizable(false);
        }

        ProductPriceLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        ProductPriceLabel.setText("Price");

        ProductDescriptionLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        ProductDescriptionLabel.setText("Description");

        ProductAddQuantityLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        ProductAddQuantityLabel.setText("Add Quantity");

        ProductCategoryLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        ProductCategoryLabel.setText("Category");

        NameLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        NameLabel2.setText("Name");

        ProductAddQuantityTextfield.setForeground(new java.awt.Color(204, 204, 204));
        ProductAddQuantityTextfield.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ProductAddQuantityTextfieldActionPerformed(evt);
            }
        });

        ProductPriceTextfield.setForeground(new java.awt.Color(204, 204, 204));

        ProductNameTextfield.setForeground(new java.awt.Color(204, 204, 204));

        CloseButton2.setText("Close");

        ResetButton2.setText("Reset");

        UpdateButton2.setText("Update");
        UpdateButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UpdateButton2ActionPerformed(evt);
            }
        });

        SaveButton2.setText("Save");

        ProductDescriptionTextField.setColumns(20);
        ProductDescriptionTextField.setRows(5);
        jScrollPane2.setViewportView(ProductDescriptionTextField);

        AddButtonActionPerformed.setText("Add");
        AddButtonActionPerformed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddButtonActionPerformedActionPerformed(evt);
            }
        });

        jLabel1.setText("IDLabel");

        DeleteButtonActionPerformed.setText("Delete");
        DeleteButtonActionPerformed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteButtonActionPerformedActionPerformed(evt);
            }
        });

        ClearButtonActionPerformed.setText("Clear");
        ClearButtonActionPerformed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClearButtonActionPerformedActionPerformed(evt);
            }
        });

        SearchButtonActionPerformed.setText("Search");
        SearchButtonActionPerformed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SearchButtonActionPerformedActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout MainPanelLayout = new javax.swing.GroupLayout(MainPanel);
        MainPanel.setLayout(MainPanelLayout);
        MainPanelLayout.setHorizontalGroup(
            MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(HeaderLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(MainPanelLayout.createSequentialGroup()
                .addGap(67, 67, 67)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(NameLabel2)
                        .addComponent(ProductAddQuantityLabel)
                        .addComponent(ProductPriceLabel)
                        .addComponent(ProductDescriptionLabel)
                        .addComponent(ProductCategoryLabel)
                        .addComponent(ProductAddQuantityTextfield)
                        .addComponent(ProductPriceTextfield)
                        .addComponent(ProductCategoryComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, MainPanelLayout.createSequentialGroup()
                            .addComponent(SaveButton2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(UpdateButton2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(ResetButton2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(CloseButton2))
                        .addComponent(ProductNameTextfield)
                        .addComponent(jScrollPane2)
                        .addComponent(jLabel1)
                        .addComponent(ProductIDTextField))
                    .addGroup(MainPanelLayout.createSequentialGroup()
                        .addComponent(AddButtonActionPerformed)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(DeleteButtonActionPerformed)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ClearButtonActionPerformed)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(SearchButtonActionPerformed)))
                .addContainerGap(218, Short.MAX_VALUE))
        );
        MainPanelLayout.setVerticalGroup(
            MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(HeaderLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(MainPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ProductIDTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(NameLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ProductNameTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ProductAddQuantityLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ProductAddQuantityTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ProductPriceLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ProductPriceTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ProductDescriptionLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ProductCategoryLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ProductCategoryComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(SaveButton2)
                            .addComponent(UpdateButton2)
                            .addComponent(ResetButton2)
                            .addComponent(CloseButton2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(AddButtonActionPerformed)
                            .addComponent(DeleteButtonActionPerformed)
                            .addComponent(ClearButtonActionPerformed)
                            .addComponent(SearchButtonActionPerformed)))
                    .addComponent(jScrollPane1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GoToOrder.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        GoToOrder.setText("Order");
        GoToOrder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GoToOrderActionPerformed(evt);
            }
        });

        GoToCategory.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        GoToCategory.setText("Category");

        GoToDashboard.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        GoToDashboard.setText("Dashboard");

        GoToUser.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        GoToUser.setText("User");

        GoToProduct.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        GoToProduct.setText("Product");
        GoToProduct.setBorder(javax.swing.BorderFactory.createMatteBorder(4, 4, 4, 4, new java.awt.Color(51, 153, 255)));

        javax.swing.GroupLayout BackgroundPanelLayout = new javax.swing.GroupLayout(BackgroundPanel);
        BackgroundPanel.setLayout(BackgroundPanelLayout);
        BackgroundPanelLayout.setHorizontalGroup(
            BackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, BackgroundPanelLayout.createSequentialGroup()
                .addGap(110, 110, 110)
                .addGroup(BackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(GoToDashboard, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
                    .addComponent(GoToOrder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(GoToCategory, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
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
                .addGroup(BackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(MainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(BackgroundPanelLayout.createSequentialGroup()
                        .addComponent(GoToDashboard, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(GoToUser, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(GoToCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15)
                        .addComponent(GoToProduct, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(GoToOrder, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(101, Short.MAX_VALUE))
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

    private void ProductAddQuantityTextfieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ProductAddQuantityTextfieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ProductAddQuantityTextfieldActionPerformed

    private void AddButtonActionPerformedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddButtonActionPerformedActionPerformed
        String productID = ProductIDTextField.getText();
        String productName = ProductNameTextfield.getText();
        String productPrice = ProductPriceTextfield.getText();
        String productQty = ProductAddQuantityTextfield.getText();
        String productDescription = ProductDescriptionTextField.getText();
        
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3307/school_warehouse_system", "root", "BKMmkb15");
            String sql = "INSERT INTO products (product_id, product_name, product_price, product_qty, product_description) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, productID);
            pst.setString(2, productName);
            pst.setString(3, productPrice);
            pst.setString(4, productQty);
            pst.setString(5, productDescription);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(null, "Product has been added successfully!");
            con.close();

        } catch (SQLException err) {
            JOptionPane.showMessageDialog(null, "Error inserting product: " + err.getMessage());
        }
    }//GEN-LAST:event_AddButtonActionPerformedActionPerformed

    private void UpdateButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UpdateButton2ActionPerformed
        String productID = ProductIDTextField.getText();
        String productName = ProductNameTextfield.getText();
        String productPrice = ProductPriceTextfield.getText();
        String productQty = ProductAddQuantityTextfield.getText();
        String productDescription = ProductDescriptionTextField.getText();
        
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3307/school_warehouse_system", "root", "BKMmkb15");
            String sql = "UPDATE products SET product_name = ?, product_price = ?, product_qty = ?, product_description = ? WHERE product_id = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, productName);
            pst.setString(2, productPrice);
            pst.setString(3, productQty);
            pst.setString(4, productDescription);
            pst.setString(5, productID);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(null, "Product has been updated successfully!");
            con.close();

        } catch (SQLException err) {
            JOptionPane.showMessageDialog(null, "Update Error: " + err.getMessage());
        }
    }//GEN-LAST:event_UpdateButton2ActionPerformed

    private void DeleteButtonActionPerformedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteButtonActionPerformedActionPerformed
        String productID = ProductIDTextField.getText();

        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/products_db", "root", "root");
            String sql = "DELETE FROM products WHERE product_id = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, productID);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(null, "Product has been deleted successfully!");
            con.close();

        } catch (SQLException err) {
            JOptionPane.showMessageDialog(null, "Delete Error: " + err.getMessage());
        }
    }//GEN-LAST:event_DeleteButtonActionPerformedActionPerformed

    private void ClearButtonActionPerformedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClearButtonActionPerformedActionPerformed
        ProductIDTextField.setText("");
        ProductNameTextfield.setText("");
        ProductPriceTextfield.setText("");
        ProductAddQuantityTextfield.setText("");
        ProductDescriptionTextField.setText("");
    }//GEN-LAST:event_ClearButtonActionPerformedActionPerformed

    private void SearchButtonActionPerformedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SearchButtonActionPerformedActionPerformed
        String productID = ProductIDTextField.getText();

        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/products_db", "root", "root");
            String sql = "SELECT * FROM products WHERE product_id = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, productID);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                ProductNameTextfield.setText(rs.getString("product_name"));
                ProductPriceTextfield.setText(rs.getString("product_price"));
                ProductAddQuantityTextfield.setText(rs.getString("product_qty"));
                ProductDescriptionTextField.setText(rs.getString("product_description"));
            } else {
                JOptionPane.showMessageDialog(null, "Product not found!");
            }
            con.close();

        } catch (SQLException err) {
            JOptionPane.showMessageDialog(null, "Connection Error: " + err.getMessage());
        }
    }//GEN-LAST:event_SearchButtonActionPerformedActionPerformed

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
            java.util.logging.Logger.getLogger(Product.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Product.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Product.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Product.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
               new Product();
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AddButtonActionPerformed;
    private javax.swing.JPanel BackgroundPanel;
    private javax.swing.JButton ClearButtonActionPerformed;
    private javax.swing.JButton CloseButton2;
    private javax.swing.JButton DeleteButtonActionPerformed;
    private javax.swing.JButton GoToCategory;
    private javax.swing.JButton GoToDashboard;
    private javax.swing.JButton GoToOrder;
    private javax.swing.JButton GoToProduct;
    private javax.swing.JButton GoToUser;
    private javax.swing.JLabel HeaderLabel;
    private javax.swing.JPanel MainPanel;
    private javax.swing.JLabel NameLabel2;
    private javax.swing.JLabel ProductAddQuantityLabel;
    private javax.swing.JTextField ProductAddQuantityTextfield;
    private javax.swing.JComboBox<String> ProductCategoryComboBox;
    private javax.swing.JLabel ProductCategoryLabel;
    private javax.swing.JLabel ProductDescriptionLabel;
    private javax.swing.JTextArea ProductDescriptionTextField;
    private javax.swing.JTextField ProductIDTextField;
    private javax.swing.JTextField ProductNameTextfield;
    private javax.swing.JLabel ProductPriceLabel;
    private javax.swing.JTextField ProductPriceTextfield;
    private javax.swing.JButton ResetButton2;
    private javax.swing.JButton SaveButton2;
    private javax.swing.JButton SearchButtonActionPerformed;
    private javax.swing.JButton UpdateButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
class DatabaseConnection {
    private static final String DB_URL = "jdbc:mysql://localhost:3307/school_warehouse_system"; // Replace with your database URL
    private static final String DB_USER = "root"; // Replace with your database username
    private static final String DB_PASSWORD = "BKMmkb15"; // Replace with your database password

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Ensure the driver is loaded
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Database connection error: " + e.getMessage());
        }
        return conn;
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}
}

