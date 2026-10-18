package com.hcpms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;

/** Administrator-only account creation and deactivation. */
public class UserManagementPanel extends JPanel {
    private final AuthManager authManager;
    private final DoctorManager doctorManager;
    private final String currentUsername;
    private final JTextField usernameField = new JTextField(14);
    private final JPasswordField passwordField = new JPasswordField(14);
    private final JComboBox<String> roleBox = new JComboBox<>(new String[]{"DOCTOR", "RECEPTIONIST"});
    private final JComboBox<DoctorChoice> doctorBox = new JComboBox<>();
    private final JLabel message = new JLabel("Create and manage staff login accounts.");
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"User ID", "Username", "Role", "Active", "Doctor ID"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    public UserManagementPanel(AuthManager authManager, DoctorManager doctorManager, String currentUsername) {
        this.authManager = authManager;
        this.doctorManager = doctorManager;
        this.currentUsername = currentUsername;
        setLayout(new BorderLayout(12, 12));
        setBackground(UIStyle.BACKGROUND);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 8));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Create Staff Account"),
                new EmptyBorder(12, 12, 12, 12)));
        form.add(new JLabel("Username")); form.add(usernameField);
        form.add(new JLabel("Temporary password (min. 10 characters)")); form.add(passwordField);
        form.add(new JLabel("Role")); form.add(roleBox);
        form.add(new JLabel("Linked doctor (required for DOCTOR)")); form.add(doctorBox);

        JButton createButton = new JButton("Create Account");
        JButton activateButton = new JButton("Activate Selected");
        JButton deactivateButton = new JButton("Deactivate Selected");
        JButton refreshButton = new JButton("Refresh Accounts");
        UIStyle.styleButton(createButton, UIStyle.BLUE);
        UIStyle.styleButton(activateButton, new Color(22, 163, 74));
        UIStyle.styleButton(deactivateButton, UIStyle.RED);
        UIStyle.styleButton(refreshButton, UIStyle.NAVY);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);
        actions.add(createButton); 
        actions.add(activateButton);
        actions.add(deactivateButton); actions.add(refreshButton);
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false); top.add(form, BorderLayout.CENTER); top.add(actions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        UIStyle.styleTable(table);
        table.setRowHeight(29);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        message.setForeground(UIStyle.MUTED);
        add(message, BorderLayout.SOUTH);

        roleBox.addActionListener(e -> updateDoctorChoiceState());
        createButton.addActionListener(e -> createAccount());
        activateButton.addActionListener(e -> activateSelected());
        deactivateButton.addActionListener(e -> deactivateSelected());
        refreshButton.addActionListener(e -> refresh());
        refresh();
    }

    private void updateDoctorChoiceState() {
        boolean needsDoctor = "DOCTOR".equals(roleBox.getSelectedItem());
        doctorBox.setEnabled(needsDoctor);
        if (!needsDoctor) doctorBox.setSelectedIndex(-1);
        else if (doctorBox.getItemCount() > 0 && doctorBox.getSelectedIndex() < 0) doctorBox.setSelectedIndex(0);
    }

    private void refresh() {
        try {
            DoctorChoice previous = (DoctorChoice) doctorBox.getSelectedItem();
            doctorBox.removeAllItems();
            List<Doctor> doctors = doctorManager.getAllDoctors();
            for (Doctor d : doctors) doctorBox.addItem(new DoctorChoice(d.getDoctorId(), d.getFullName()));
            if (previous != null) {
                for (int i = 0; i < doctorBox.getItemCount(); i++) {
                    if (doctorBox.getItemAt(i).id == previous.id) { doctorBox.setSelectedIndex(i); break; }
                }
            }
            updateDoctorChoiceState();
            model.setRowCount(0);
            for (String[] row : authManager.getUsers()) model.addRow(row);
            message.setText("Accounts loaded: " + model.getRowCount());
        } catch (SQLException ex) {
            showError("Could not load accounts or doctors. " + ex.getMessage());
        }
    }

    private void createAccount() {
        char[] password = passwordField.getPassword();
        try {
            String role = (String) roleBox.getSelectedItem();
            DoctorChoice doctor = (DoctorChoice) doctorBox.getSelectedItem();
            Integer doctorId = "DOCTOR".equals(role) && doctor != null ? doctor.id : null;
            authManager.createStaffUser(usernameField.getText(), password, role, doctorId);
            usernameField.setText(""); passwordField.setText("");
            JOptionPane.showMessageDialog(this, "Account created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            refresh();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("ORA-00001"))
                showError("That username already exists. Choose another username.");
            else showError("Could not create account. " + ex.getMessage());
        } catch (GeneralSecurityException ex) {
            showError("Secure password hashing is unavailable: " + ex.getMessage());
        } finally {
            java.util.Arrays.fill(password, '\0');
            passwordField.setText("");
        }
    }

    
private void activateSelected() {
    int row = table.getSelectedRow();

    if (row < 0) {
        showError("Select an account first.");
        return;
    }

    int modelRow = table.convertRowIndexToModel(row);
    int id = Integer.parseInt(
            model.getValueAt(modelRow, 0).toString());
    String username = model.getValueAt(modelRow, 1).toString();
    String active = model.getValueAt(modelRow, 3).toString();

    if ("Y".equals(active)) {
        showError("This account is already active.");
        return;
    }

    int answer = JOptionPane.showConfirmDialog(
            this,
            "Activate account '" + username
                    + "'? The user will be able to sign in again "
                    + "using their existing password.",
            "Confirm Activation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
    );

    if (answer != JOptionPane.YES_OPTION) {
        return;
    }

    try {
        if (authManager.activateUser(id)) {
            message.setForeground(UIStyle.MUTED);
            message.setText("Account activated: " + username);
            refresh();
        } else {
            showError("Account was not activated. "
                    + "It may already be active or no longer exist.");
            refresh();
        }
    } catch (SQLException ex) {
        showError("Could not activate account. " + ex.getMessage());
    }
}


    private void deactivateSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { showError("Select an account first."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        int id = Integer.parseInt(model.getValueAt(modelRow, 0).toString());
        String username = model.getValueAt(modelRow, 1).toString();
        String active = model.getValueAt(modelRow, 3).toString();
        if (!"Y".equals(active)) { showError("This account is already inactive."); return; }
        int answer = JOptionPane.showConfirmDialog(this,
                "Deactivate account '" + username + "'? This user will no longer be able to sign in.",
                "Confirm Deactivation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            if (authManager.deactivateUser(id, currentUsername)) {
                message.setText("Account deactivated: " + username);
                refresh();
            }
        } catch (IllegalArgumentException | SQLException ex) { showError(ex.getMessage()); }
    }

    private void showError(String text) {
        message.setForeground(UIStyle.RED);
        message.setText(text);
    }

    private static final class DoctorChoice {
        private final int id; private final String name;
        private DoctorChoice(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return id + " - " + name; }
    }
}
