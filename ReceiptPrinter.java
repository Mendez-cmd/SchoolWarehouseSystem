import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * Utility class for printing receipts and purchase orders
 * Provides various printing formats and options
 */
public class ReceiptPrinter {
    
    /**
     * Order item class to hold item details
     */
    public static class OrderItem {
        private String name;
        private int quantity;
        private double unitPrice;
        private double total;
        private String category;
        
        public OrderItem(String name, int quantity, double unitPrice, String category) {
            this.name = name;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.total = quantity * unitPrice;
            this.category = category;
        }
        
        // Getters
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
        public double getTotal() { return total; }
        public String getCategory() { return category; }
    }
    
    /**
     * Print a simple receipt format
     */
    public static void printSimpleReceipt(List<OrderItem> items, Component parent) {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        
        printerJob.setPrintable(new Printable() {
            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                if (pageIndex > 0) return NO_SUCH_PAGE;
                
                Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                
                Font headerFont = new Font("Arial", Font.BOLD, 16);
                Font normalFont = new Font("Arial", Font.PLAIN, 12);
                Font boldFont = new Font("Arial", Font.BOLD, 12);
                
                int y = 50;
                int lineHeight = 20;
                
                // Header
                g2d.setFont(headerFont);
                g2d.drawString("SCHOOL WAREHOUSE SYSTEM", 50, y);
                y += lineHeight * 2;
                
                g2d.setFont(normalFont);
                g2d.drawString("Purchase Order Receipt", 50, y);
                y += lineHeight;
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                g2d.drawString("Date: " + dateFormat.format(new Date()), 50, y);
                y += lineHeight;
                g2d.drawString("Order #: PO-" + System.currentTimeMillis(), 50, y);
                y += lineHeight * 2;
                
                // Items header
                g2d.setFont(boldFont);
                g2d.drawString("Item", 50, y);
                g2d.drawString("Qty", 250, y);
                g2d.drawString("Price", 300, y);
                g2d.drawString("Total", 400, y);
                y += lineHeight;
                
                // Draw line
                g2d.drawLine(50, y, 450, y);
                y += lineHeight;
                
                // Items
                g2d.setFont(normalFont);
                double grandTotal = 0;
                for (OrderItem item : items) {
                    g2d.drawString(item.getName(), 50, y);
                    g2d.drawString(String.valueOf(item.getQuantity()), 250, y);
                    g2d.drawString(String.format("$%.2f", item.getUnitPrice()), 300, y);
                    g2d.drawString(String.format("$%.2f", item.getTotal()), 400, y);
                    grandTotal += item.getTotal();
                    y += lineHeight;
                }
                
                // Total section
                y += lineHeight;
                g2d.drawLine(300, y, 450, y);
                y += lineHeight;
                
                double tax = grandTotal * 0.08;
                double finalTotal = grandTotal + tax;
                
                g2d.drawString("Subtotal:", 300, y);
                g2d.drawString(String.format("$%.2f", grandTotal), 400, y);
                y += lineHeight;
                
                g2d.drawString("Tax (8%):", 300, y);
                g2d.drawString(String.format("$%.2f", tax), 400, y);
                y += lineHeight;
                
                g2d.setFont(boldFont);
                g2d.drawString("TOTAL:", 300, y);
                g2d.drawString(String.format("$%.2f", finalTotal), 400, y);
                
                return PAGE_EXISTS;
            }
        });
        
        if (printerJob.printDialog()) {
            try {
                printerJob.print();
                JOptionPane.showMessageDialog(parent, "Receipt printed successfully!", 
                                            "Print Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(parent, "Error printing receipt: " + e.getMessage(), 
                                            "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Print detailed purchase order with company letterhead
     */
    public static void printDetailedPurchaseOrder(List<OrderItem> items, Component parent) {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        
        printerJob.setPrintable(new Printable() {
            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                if (pageIndex > 0) return NO_SUCH_PAGE;
                
                Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                
                Font titleFont = new Font("Arial", Font.BOLD, 20);
                Font headerFont = new Font("Arial", Font.BOLD, 14);
                Font normalFont = new Font("Arial", Font.PLAIN, 11);
                Font smallFont = new Font("Arial", Font.PLAIN, 9);
                
                int y = 30;
                int lineHeight = 15;
                
                // Company Header
                g2d.setFont(titleFont);
                g2d.drawString("SCHOOL WAREHOUSE SYSTEM", 50, y);
                y += 25;
                
                g2d.setFont(normalFont);
                g2d.drawString("123 Education Street, Learning City, State 12345", 50, y);
                y += lineHeight;
                g2d.drawString("Phone: (555) 123-4567 | Email: info@schoolwarehouse.com", 50, y);
                y += lineHeight * 2;
                
                // Purchase Order Title
                g2d.setFont(headerFont);
                g2d.drawString("PURCHASE ORDER", 200, y);
                y += lineHeight * 2;
                
                // Order Information Box
                g2d.setFont(normalFont);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String orderNumber = "PO-" + System.currentTimeMillis();
                
                g2d.drawRect(50, y, 200, 80);
                g2d.drawString("Order Number: " + orderNumber, 60, y + 20);
                g2d.drawString("Order Date: " + dateFormat.format(new Date()), 60, y + 35);
                g2d.drawString("Status: Pending", 60, y + 50);
                g2d.drawString("Payment Terms: Net 30", 60, y + 65);
                
                // Vendor Information Box
                g2d.drawRect(300, y, 200, 80);
                g2d.drawString("Vendor Information:", 310, y + 20);
                g2d.drawString("Internal Order", 310, y + 35);
                g2d.drawString("Warehouse Department", 310, y + 50);
                
                y += 100;
                
                // Items Table Header
                g2d.setFont(headerFont);
                g2d.drawString("Item Description", 50, y);
                g2d.drawString("Category", 200, y);
                g2d.drawString("Qty", 280, y);
                g2d.drawString("Unit Price", 320, y);
                g2d.drawString("Total", 420, y);
                y += lineHeight;
                
                // Table border
                g2d.drawLine(50, y, 480, y);
                y += 5;
                
                // Items
                g2d.setFont(normalFont);
                double grandTotal = 0;
                for (OrderItem item : items) {
                    if (y > 700) { // Page break check
                        return PAGE_EXISTS;
                    }
                    
                    g2d.drawString(item.getName(), 50, y);
                    g2d.drawString(item.getCategory(), 200, y);
                    g2d.drawString(String.valueOf(item.getQuantity()), 280, y);
                    g2d.drawString(String.format("$%.2f", item.getUnitPrice()), 320, y);
                    g2d.drawString(String.format("$%.2f", item.getTotal()), 420, y);
                    grandTotal += item.getTotal();
                    y += lineHeight;
                }
                
                // Total section
                y += 20;
                g2d.drawLine(320, y, 480, y);
                y += 15;
                
                double tax = grandTotal * 0.08;
                double finalTotal = grandTotal + tax;
                
                g2d.drawString("Subtotal:", 350, y);
                g2d.drawString(String.format("$%.2f", grandTotal), 420, y);
                y += lineHeight;
                
                g2d.drawString("Tax (8%):", 350, y);
                g2d.drawString(String.format("$%.2f", tax), 420, y);
                y += lineHeight;
                
                g2d.setFont(headerFont);
                g2d.drawString("TOTAL:", 350, y);
                g2d.drawString(String.format("$%.2f", finalTotal), 420, y);
                
                // Footer
                y += 40;
                g2d.setFont(smallFont);
                g2d.drawString("Terms and Conditions:", 50, y);
                y += 12;
                g2d.drawString("• Payment due within 30 days of receipt", 50, y);
                y += 12;
                g2d.drawString("• All items subject to availability", 50, y);
                y += 12;
                g2d.drawString("• Please retain this document for your records", 50, y);
                
                return PAGE_EXISTS;
            }
        });
        
        if (printerJob.printDialog()) {
            try {
                printerJob.print();
                JOptionPane.showMessageDialog(parent, "Purchase Order printed successfully!", 
                                            "Print Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(parent, "Error printing purchase order: " + e.getMessage(), 
                                            "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Show print preview dialog
     */
    public static void showPrintPreview(List<OrderItem> items, Component parent) {
        JDialog previewDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), 
                                          "Print Preview", true);
        previewDialog.setSize(700, 900);
        previewDialog.setLocationRelativeTo(parent);
        
        JPanel previewPanel = createPreviewPanel(items);
        JScrollPane scrollPane = new JScrollPane(previewPanel);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton printButton = new JButton("Print");
        JButton closeButton = new JButton("Close");
        
        printButton.addActionListener(e -> {
            printDetailedPurchaseOrder(items, parent);
            previewDialog.dispose();
        });
        
        closeButton.addActionListener(e -> previewDialog.dispose());
        
        buttonPanel.add(printButton);
        buttonPanel.add(closeButton);
        
        previewDialog.setLayout(new BorderLayout());
        previewDialog.add(scrollPane, BorderLayout.CENTER);
        previewDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        previewDialog.setVisible(true);
    }
    
    /**
     * Create preview panel that mimics the printed output
     */
    private static JPanel createPreviewPanel(List<OrderItem> items) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Add all the same content as the print method
        // This is a simplified version - you can expand it to match exactly
        
        JLabel title = new JLabel("SCHOOL WAREHOUSE SYSTEM");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        
        panel.add(Box.createVerticalStrut(20));
        
        JLabel subtitle = new JLabel("PURCHASE ORDER");
        subtitle.setFont(new Font("Arial", Font.BOLD, 16));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(subtitle);
        
        panel.add(Box.createVerticalStrut(20));
        
        // Add order details, items table, etc.
        // Similar to the createReceiptPanel method in Order.java
        
        return panel;
    }
}

