
package com.hcpms;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class DashboardPanel extends JPanel {

    private final DatabaseConnection database;

    private final JLabel patientsLabel = new JLabel("—");
    private final JLabel doctorsLabel = new JLabel("—");
    private final JLabel appointmentsLabel = new JLabel("—");
    private final JLabel scheduledLabel = new JLabel("—");
    private final JLabel completedLabel = new JLabel("—");
    private final JLabel cancelledLabel = new JLabel("—");
    private final JLabel messageLabel = new JLabel(" ");

    public DashboardPanel(DatabaseConnection database) {
        this.database = database;

        setLayout(new BorderLayout(20, 22));
        setBackground(UIStyle.BACKGROUND);
        setBorder(new EmptyBorder(24, 26, 20, 26));

        add(createHeader(), BorderLayout.NORTH);
        add(createCards(), BorderLayout.CENTER);

        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        messageLabel.setForeground(UIStyle.MUTED);
        add(messageLabel, BorderLayout.SOUTH);

        loadDashboard();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(15, 10));
        header.setOpaque(false);

        JLabel heading = new JLabel("Hospital Dashboard");
        heading.setFont(new Font("SansSerif", Font.BOLD, 27));
        heading.setForeground(UIStyle.NAVY);

        JLabel subtitle = new JLabel(
                "Monitor patients, doctors and appointment activity");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(UIStyle.MUTED);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(heading);
        titles.add(Box.createVerticalStrut(6));
        titles.add(subtitle);

        JButton refreshButton = new JButton("Refresh Dashboard");
        UIStyle.styleButton(refreshButton, UIStyle.BLUE);
        refreshButton.addActionListener(e -> loadDashboard());

        header.add(titles, BorderLayout.WEST);
        header.add(refreshButton, BorderLayout.EAST);
        return header;
    }

    private JPanel createCards() {
        JPanel cards = new JPanel(new GridLayout(2, 3, 18, 18));
        cards.setOpaque(false);
        cards.setBorder(new EmptyBorder(28, 0, 15, 0));

        cards.add(createCard("TOTAL PATIENTS",
                patientsLabel, UIStyle.BLUE, "Registered patient records"));

        cards.add(createCard("TOTAL DOCTORS",
                doctorsLabel, UIStyle.TEAL, "Registered medical staff"));

        cards.add(createCard("TOTAL APPOINTMENTS",
                appointmentsLabel, new Color(113, 87, 174),
                "All appointment records"));

        cards.add(createCard("SCHEDULED",
                scheduledLabel, new Color(198, 130, 35),
                "Appointments with scheduled status"));

        cards.add(createCard("COMPLETED",
                completedLabel, UIStyle.TEAL,
                "Appointments marked completed"));

        cards.add(createCard("CANCELLED",
                cancelledLabel, UIStyle.RED,
                "Appointments marked cancelled"));

        return cards;
    }

    private JPanel createCard(
            String title, JLabel value, Color accent, String description) {

        JPanel card = new JPanel(new BorderLayout(8, 12));
        card.setBackground(Color.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                        0, 5, 0, 0, accent),
                new EmptyBorder(18, 18, 18, 18)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setForeground(UIStyle.MUTED);

        value.setFont(new Font("SansSerif", Font.BOLD, 34));
        value.setForeground(accent);

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descriptionLabel.setForeground(UIStyle.MUTED);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(descriptionLabel, BorderLayout.SOUTH);

        return card;
    }

    public void loadDashboard() {
        String sql =
            "SELECT " +
            "(SELECT COUNT(*) FROM patients) AS total_patients, " +
            "(SELECT COUNT(*) FROM doctors) AS total_doctors, " +
            "(SELECT COUNT(*) FROM appointments) AS total_appointments, " +
            "(SELECT COUNT(*) FROM appointments " +
                "WHERE status = 'SCHEDULED') AS scheduled_count, " +
            "(SELECT COUNT(*) FROM appointments " +
                "WHERE status = 'COMPLETED') AS completed_count, " +
            "(SELECT COUNT(*) FROM appointments " +
                "WHERE status = 'CANCELLED') AS cancelled_count " +
            "FROM dual";

        try (Connection con = database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                patientsLabel.setText(
                        String.valueOf(rs.getInt("total_patients")));
                doctorsLabel.setText(
                        String.valueOf(rs.getInt("total_doctors")));
                appointmentsLabel.setText(
                        String.valueOf(rs.getInt("total_appointments")));
                scheduledLabel.setText(
                        String.valueOf(rs.getInt("scheduled_count")));
                completedLabel.setText(
                        String.valueOf(rs.getInt("completed_count")));
                cancelledLabel.setText(
                        String.valueOf(rs.getInt("cancelled_count")));
            }

            messageLabel.setText("Dashboard data refreshed successfully.");

        } catch (SQLException ex) {
            messageLabel.setText("Unable to load dashboard data.");
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load dashboard data:\n" + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
