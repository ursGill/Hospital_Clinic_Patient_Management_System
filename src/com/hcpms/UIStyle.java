
package com.hcpms;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;

public final class UIStyle {

    public static final Color NAVY = new Color(25, 48, 78);
    public static final Color BLUE = new Color(35, 105, 180);
    public static final Color TEAL = new Color(22, 125, 114);
    public static final Color RED = new Color(185, 55, 55);
    public static final Color BACKGROUND = new Color(244, 247, 251);
    public static final Color BORDER = new Color(220, 227, 236);
    public static final Color TEXT = new Color(40, 52, 68);
    public static final Color MUTED = new Color(105, 119, 137);

    private UIStyle() {
    }

    public static void applyTheme() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("OptionPane.messageFont",
                new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("TabbedPane.selected",
                Color.WHITE);
        UIManager.put("TabbedPane.font",
                new Font("SansSerif", Font.BOLD, 13));
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.font",
                new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("ComboBox.font",
                new Font("SansSerif", Font.PLAIN, 14));
    }

    public static void styleTree(Component component) {
        if (component instanceof JPanel) {
            component.setBackground(BACKGROUND);
        }

        if (component instanceof JButton button) {
            styleButton(button, BLUE);
        }

        if (component instanceof JTextField field) {
            field.setFont(new Font("SansSerif", Font.PLAIN, 14));
            field.setPreferredSize(new Dimension(
                    Math.max(field.getPreferredSize().width, 120), 36));
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    new EmptyBorder(5, 9, 5, 9)));
        }

        if (component instanceof JComboBox<?> combo) {
            combo.setFont(new Font("SansSerif", Font.PLAIN, 14));
            combo.setPreferredSize(new Dimension(
                    Math.max(combo.getPreferredSize().width, 120), 36));
        }

        if (component instanceof JTable table) {
            styleTable(table);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                styleTree(child);
            }
        }
    }

    public static void styleButton(JButton button, Color color) {
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));
        button.setMargin(new Insets(8, 14, 8, 14));
        button.setMinimumSize(new Dimension(100, 38));
        button.setPreferredSize(new Dimension(
                Math.max(button.getPreferredSize().width, 110), 38));
    }

    public static void styleTable(JTable table) {
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(218, 233, 250));
        table.setSelectionForeground(TEXT);
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setGridColor(BORDER);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(1, 1));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setForeground(Color.WHITE);
        header.setBackground(NAVY);
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
    }
}
