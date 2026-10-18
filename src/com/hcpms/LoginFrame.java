package com.hcpms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/** Login window; offers first-run administrator setup if no accounts exist. */
public class LoginFrame extends JFrame {
    private final AuthManager authManager;
    private final Consumer<AppUser> onAuthenticated;
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel heading = new JLabel("Welcome back", SwingConstants.CENTER);
    private final JLabel subtitle = new JLabel("Sign in to HCPMS", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JButton submitButton = new JButton("Sign In");
    private boolean firstRun;

    public LoginFrame(AuthManager authManager, Consumer<AppUser> onAuthenticated) {
        this.authManager = authManager;
        this.onAuthenticated = onAuthenticated;

        setTitle("HCPMS | Secure Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(470, 390);
        setMinimumSize(new Dimension(440, 360));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(UIStyle.BACKGROUND);
        root.setBorder(new EmptyBorder(28, 34, 24, 34));

        heading.setFont(new Font("SansSerif", Font.BOLD, 25));
        heading.setForeground(UIStyle.NAVY);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(UIStyle.MUTED);

        JPanel titles = new JPanel(new BorderLayout(0, 7));
        titles.setOpaque(false);
        titles.add(heading, BorderLayout.NORTH);
        titles.add(subtitle, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER),
                new EmptyBorder(18, 18, 18, 18)));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 4, 7, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0; c.gridy = 0;
        JLabel userLabel = new JLabel("Username");
        userLabel.setForeground(UIStyle.TEXT);
        form.add(userLabel, c);
        c.gridy++;
        form.add(usernameField, c);
        c.gridy++;
        JLabel passLabel = new JLabel("Password");
        passLabel.setForeground(UIStyle.TEXT);
        form.add(passLabel, c);
        c.gridy++;
        form.add(passwordField, c);
        c.gridy++;
        c.insets = new Insets(12, 4, 4, 4);
        UIStyle.styleButton(submitButton, UIStyle.BLUE);
        submitButton.setPreferredSize(new Dimension(150, 38));
        form.add(submitButton, c);

        statusLabel.setForeground(UIStyle.MUTED);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        root.add(titles, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        setContentPane(root);

        submitButton.addActionListener(e -> submit());
        passwordField.addActionListener(e -> submit());
        refreshFirstRunState();
    }

    private void refreshFirstRunState() {
        try {
            firstRun = !authManager.hasAnyUsers();
            if (firstRun) {
                heading.setText("Create Administrator");
                subtitle.setText("First-time setup for HCPMS");
                submitButton.setText("Create Admin Account");
                statusLabel.setText("Use a unique username and a password of at least 10 characters.");
            } else {
                heading.setText("Welcome back");
                subtitle.setText("Sign in to HCPMS");
                submitButton.setText("Sign In");
                statusLabel.setText(" ");
            }
        } catch (SQLException ex) {
            submitButton.setEnabled(false);
            statusLabel.setText("Cannot access the user table. Run database/users.sql first.");
            JOptionPane.showMessageDialog(this,
                    "Could not check login accounts.\nRun database/users.sql as HCPMS_USER.\n\n"
                            + ex.getMessage(),
                    "Login Setup Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submit() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        if (username.isBlank() || password.length == 0) {
            clearPassword(password);
            showMessage("Enter both username and password.", false);
            return;
        }

        submitButton.setEnabled(false);
        try {
            if (firstRun) {
                authManager.createInitialAdmin(username, password);
                showMessage("Administrator account created. Opening HCPMS...", true);
                onAuthenticated.accept(new AppUser(-1, username, "ADMIN", null));
                dispose();
            } else {
                AppUser user = authManager.authenticate(username, password);
                if (user == null) {
                    passwordField.setText("");
                    showMessage("Invalid username or password, or account is disabled.", false);
                } else {
                    onAuthenticated.accept(user);
                    dispose();
                }
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showMessage(ex.getMessage(), false);
        } catch (SQLException ex) {
            showMessage("Database error. Check your Oracle connection and app_users table.", false);
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        } catch (GeneralSecurityException ex) {
            showMessage("Secure password verification is unavailable on this Java installation.", false);
        } finally {
            clearPassword(password);
            submitButton.setEnabled(true);
        }
    }

    private void showMessage(String message, boolean success) {
        statusLabel.setForeground(success ? UIStyle.TEAL : UIStyle.RED);
        statusLabel.setText(message);
    }

    private static void clearPassword(char[] password) {
        if (password != null) java.util.Arrays.fill(password, '\0');
    }
}
