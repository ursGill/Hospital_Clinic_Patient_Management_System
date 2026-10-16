
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

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel heading = new JLabel("Hospital Dashboard");
        heading.setFont(new Font("SansSerif", Font.BOLD, 24));

        JLabel subtitle = new JLabel(
                "Overview of patients, doctors and appointments");
        subtitle.setForeground(Color.GRAY);

        JPanel header = new JPanel(new BorderLayout(5, 5));
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(heading);
        titles.add(subtitle);

        JButton refreshButton = new JButton("Refresh Dashboard");
        refreshButton.addActionListener(e -> loadDashboard());

        header.add(titles, BorderLayout.WEST);
        header.add(refreshButton, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(2, 3, 15, 15));

        cards.add(createCard("Total Patients", patientsLabel));
        cards.add(createCard("Total Doctors", doctorsLabel));
        cards.add(createCard("Total Appointments", appointmentsLabel));
        cards.add(createCard("Scheduled", scheduledLabel));
        cards.add(createCard("Completed", completedLabel));
        cards.add(createCard("Cancelled", cancelledLabel));

        add(cards, BorderLayout.CENTER);
        add(messageLabel, BorderLayout.SOUTH);

        loadDashboard();
    }

    private JPanel createCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(5, 15));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 30));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

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

            messageLabel.setText("Dashboard updated successfully.");

        } catch (SQLException ex) {
            messageLabel.setText("Unable to load dashboard data.");
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load dashboard data:\n" + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
