
package com.hcpms;

import java.awt.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

public class AppointmentPanel extends JPanel {

    private final PatientManager patientManager;
    private final DoctorManager doctorManager;
    private final AppointmentManager appointmentManager;
    private final Integer doctorIdFilter;
    private final boolean readOnly;

    private final JComboBox<Choice> patientBox = new JComboBox<>();
    private final JComboBox<Choice> doctorBox = new JComboBox<>();

    private final JTextField dateTimeField = new JTextField(19);

    private final JComboBox<String> statusBox =
            new JComboBox<>(new String[]{
                "SCHEDULED", "COMPLETED", "CANCELLED"
            });

    private final JComboBox<String> filterBox =
            new JComboBox<>(new String[]{
                "All Appointments",
                "Upcoming",
                "Past / Overdue",
                "Completed",
                "Cancelled"
            });

    private final JLabel countLabel = new JLabel("Appointments: 0");
    private final JLabel messageLabel = new JLabel(" ");

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                new String[]{
                    "ID", "Patient", "Doctor",
                    "Appointment Time", "Status"
                }, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable table = new JTable(tableModel);

    private final DateTimeFormatter inputFormatter =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    private final DateTimeFormatter displayFormatter =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

    private List<Appointment> allAppointments = new ArrayList<>();

    public AppointmentPanel(
            PatientManager patientManager,
            DoctorManager doctorManager,
            AppointmentManager appointmentManager) {
        this(patientManager, doctorManager, appointmentManager, null, false);
    }

    public AppointmentPanel(
            PatientManager patientManager,
            DoctorManager doctorManager,
            AppointmentManager appointmentManager,
            Integer doctorIdFilter,
            boolean readOnly) {

        this.patientManager = patientManager;
        this.doctorManager = doctorManager;
        this.appointmentManager = appointmentManager;
        this.doctorIdFilter = doctorIdFilter;
        this.readOnly = readOnly;

        setLayout(new BorderLayout(16, 16));
        setBackground(UIStyle.BACKGROUND);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        add(createForm(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        if (readOnly) {
            patientBox.setEnabled(false);
            doctorBox.setEnabled(false);
            dateTimeField.setEnabled(false);
            statusBox.setEnabled(false);
            setReadOnlyControls();
        }

        filterBox.addActionListener(e -> applyFilter());

        if (!readOnly) {
            loadDropdowns();
        }
        loadAppointments();
    }

    private JPanel createForm() {
        JPanel outer = new JPanel(new BorderLayout(10, 10));
        outer.setBackground(UIStyle.BACKGROUND);

        JLabel heading = new JLabel("Appointment Management");
        heading.setFont(new Font("SansSerif", Font.BOLD, 23));
        heading.setForeground(UIStyle.NAVY);

        JLabel subtitle = new JLabel(
                "Schedule appointments and manage their status");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(UIStyle.MUTED);

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(heading);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(subtitle);

        outer.add(titlePanel, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER),
                new EmptyBorder(12, 14, 12, 14)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 8, 7, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        addField(form, c, 0, 0, "Patient", patientBox);
        addField(form, c, 2, 0, "Doctor", doctorBox);
        addField(form, c, 0, 1, "Date and time", dateTimeField);
        addField(form, c, 2, 1, "Appointment status", statusBox);

        dateTimeField.setToolTipText("Format: yyyy-MM-dd HH:mm:ss");
        dateTimeField.setText("");
        dateTimeField.setPreferredSize(new Dimension(210, 36));

        JLabel hint = new JLabel("Enter date and time as yyyy-MM-dd HH:mm:ss");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setForeground(UIStyle.MUTED);

        JButton scheduleButton = new JButton("Schedule Appointment");
        JButton updateButton = new JButton("Update Status");
        JButton clearButton = new JButton("Clear Date");
        JButton refreshButton = new JButton("Refresh");

        UIStyle.styleButton(scheduleButton, UIStyle.BLUE);
        UIStyle.styleButton(updateButton, UIStyle.TEAL);
        UIStyle.styleButton(clearButton, UIStyle.NAVY);
        UIStyle.styleButton(refreshButton, new Color(82, 101, 122));

        scheduleButton.addActionListener(e -> scheduleAppointment());
        updateButton.addActionListener(e -> updateStatus());
        clearButton.addActionListener(e -> dateTimeField.setText(""));
        refreshButton.addActionListener(e -> refreshData());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.setOpaque(false);
        buttons.add(scheduleButton);
        buttons.add(updateButton);
        buttons.add(clearButton);
        buttons.add(refreshButton);

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.setOpaque(false);
        bottom.add(hint, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.CENTER);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 4;
        c.weightx = 1;
        form.add(bottom, c);

        outer.add(form, BorderLayout.CENTER);
        return outer;
    }

    private void setReadOnlyControls() {
        // The appointment form buttons are contained in the NORTH component tree.
        setControlsEnabled(this, false);
        filterBox.setEnabled(true);
        table.setEnabled(true);
    }

    private void setControlsEnabled(java.awt.Container parent, boolean enabled) {
        for (java.awt.Component component : parent.getComponents()) {
            if (component instanceof JButton button) {
                button.setEnabled(enabled || "Refresh".equals(button.getText()));
            }
            if (component instanceof java.awt.Container container) setControlsEnabled(container, enabled);
        }
    }

    private void addField(
            JPanel panel,
            GridBagConstraints c,
            int x,
            int y,
            String label,
            JComponent field) {

        JLabel fieldLabel = new JLabel(label + ":");
        fieldLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        fieldLabel.setForeground(UIStyle.TEXT);

        c.gridx = x;
        c.gridy = y;
        c.gridwidth = 1;
        c.weightx = 0;
        panel.add(fieldLabel, c);

        c.gridx = x + 1;
        c.weightx = 1;
        panel.add(field, c);
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UIStyle.BACKGROUND);

        JPanel toolbar = new JPanel(new BorderLayout(10, 5));
        toolbar.setOpaque(false);

        JLabel heading = new JLabel("Appointment Records");
        heading.setFont(new Font("SansSerif", Font.BOLD, 17));
        heading.setForeground(UIStyle.NAVY);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterPanel.setOpaque(false);
        filterPanel.add(new JLabel("Filter:"));
        filterPanel.add(filterBox);

        toolbar.add(heading, BorderLayout.WEST);
        toolbar.add(filterPanel, BorderLayout.EAST);

        filterBox.setPreferredSize(new Dimension(180, 36));

        UIStyle.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowHeight(32);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // Fill the appointment form whenever a table row is selected.
        // The row is converted to the model index so sorting works correctly.
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                populateSelectedAppointment();
            }
        });

        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(0).setPreferredWidth(55);
        columns.getColumn(1).setPreferredWidth(180);
        columns.getColumn(2).setPreferredWidth(180);
        columns.getColumn(3).setPreferredWidth(190);
        columns.getColumn(4).setPreferredWidth(110);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);


        


        return panel;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 5));
        footer.setOpaque(false);

        countLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        countLabel.setForeground(UIStyle.NAVY);

        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        messageLabel.setForeground(UIStyle.MUTED);

        footer.add(countLabel, BorderLayout.WEST);
        footer.add(messageLabel, BorderLayout.CENTER);
        return footer;
    }

    private void loadDropdowns() {
        try {
            patientBox.removeAllItems();
            doctorBox.removeAllItems();

            for (Patient p : patientManager.getAllPatients()) {
                patientBox.addItem(new Choice(
                        p.getPatientId(), p.getFullName()));
            }

            for (Doctor d : doctorManager.getAllDoctors()) {
                doctorBox.addItem(new Choice(
                        d.getDoctorId(), d.getFullName()));
            }

            if (patientBox.getItemCount() == 0) {
                messageLabel.setText("Register a patient before scheduling.");
            } else if (doctorBox.getItemCount() == 0) {
                messageLabel.setText("Register a doctor before scheduling.");
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void scheduleAppointment() {
        Choice patient = (Choice) patientBox.getSelectedItem();
        Choice doctor = (Choice) doctorBox.getSelectedItem();

        if (patient == null || doctor == null) {
            JOptionPane.showMessageDialog(this,
                    "Register at least one patient and one doctor first.",
                    "Missing Information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(
                    dateTimeField.getText().trim(), inputFormatter);

            Timestamp timestamp = Timestamp.valueOf(dateTime);

            if (!dateTime.isAfter(LocalDateTime.now())) {
                JOptionPane.showMessageDialog(this,
                        "Choose a future date and time.",
                        "Invalid Appointment Time",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            appointmentManager.addAppointment(
                    patient.id, doctor.id, timestamp);

            JOptionPane.showMessageDialog(this,
                    "Appointment scheduled successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            dateTimeField.setText("");
            filterBox.setSelectedItem("All Appointments");
            loadAppointments();

        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Enter a valid date and time.\n"
                    + "Required format: yyyy-MM-dd HH:mm:ss\n"
                    + "Example: 2026-12-20 10:30:00",
                    "Invalid Date or Time",
                    JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Cannot Schedule Appointment",
                    JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void populateSelectedAppointment() {
        int viewRow = table.getSelectedRow();

        if (viewRow == -1) {
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);

        try {
            int appointmentId = Integer.parseInt(
                    tableModel.getValueAt(modelRow, 0).toString());

            // Use the appointment ID rather than the row index. This remains
            // correct when the table is sorted or filtered.
            for (Appointment appointment : allAppointments) {
                if (appointment.getAppointmentId() == appointmentId) {
                    selectChoiceById(patientBox, appointment.getPatientId());
                    selectChoiceById(doctorBox, appointment.getDoctorId());

                    dateTimeField.setText(
                            appointment.getAppointmentTime()
                                    .toLocalDateTime()
                                    .format(inputFormatter));

                    // OVERDUE is only a display label. The database status
                    // remains SCHEDULED until the user explicitly changes it.
                    statusBox.setSelectedItem(appointment.getStatus());

                    messageLabel.setText(
                            "Selected appointment #" + appointmentId + ".");
                    return;
                }
            }
        } catch (NumberFormatException ex) {
            messageLabel.setText("Could not read the selected appointment.");
        }
    }

    private void selectChoiceById(JComboBox<Choice> comboBox, int id) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            Choice choice = comboBox.getItemAt(i);

            if (choice != null && choice.id == id) {
                comboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private void updateStatus() {
        int viewRow = table.getSelectedRow();

        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Select an appointment from the table first.",
                    "No Appointment Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);
        int appointmentId = Integer.parseInt(
                tableModel.getValueAt(modelRow, 0).toString());

        String status = (String) statusBox.getSelectedItem();

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Change appointment #" + appointmentId
                        + " to " + status + "?",
                "Confirm Status Update",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            appointmentManager.updateStatus(appointmentId, status);

            messageLabel.setText(
                    "Appointment #" + appointmentId + " updated.");

            loadAppointments();

        } catch (SQLException | IllegalArgumentException ex) {
            showError(ex);
        }
    }

    private void refreshData() {
        if (!readOnly) loadDropdowns();
        loadAppointments();
        messageLabel.setText("Appointment data refreshed.");
    }

    private void loadAppointments() {
        try {
            allAppointments = appointmentManager.getAllAppointments();
            applyFilter();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void applyFilter() {
        String filter = (String) filterBox.getSelectedItem();
        LocalDateTime now = LocalDateTime.now();

        tableModel.setRowCount(0);
        int visibleCount = 0;

        for (Appointment a : allAppointments) {
            if (doctorIdFilter != null && a.getDoctorId() != doctorIdFilter.intValue()) {
                continue;
            }
            LocalDateTime appointmentTime =
                    a.getAppointmentTime().toLocalDateTime();

            boolean past = appointmentTime.isBefore(now);
            String status = a.getStatus();

            boolean include;

            if ("Upcoming".equals(filter)) {
                include = "SCHEDULED".equals(status) && !past;
            } else if ("Past / Overdue".equals(filter)) {
                include = "SCHEDULED".equals(status) && past;
            } else if ("Completed".equals(filter)) {
                include = "COMPLETED".equals(status);
            } else if ("Cancelled".equals(filter)) {
                include = "CANCELLED".equals(status);
            } else {
                include = true;
            }

            if (!include) {
                continue;
            }

            String displayedStatus =
                    "SCHEDULED".equals(status) && past
                            ? "OVERDUE" : status;

            tableModel.addRow(new Object[]{
                a.getAppointmentId(),
                a.getPatientName(),
                a.getDoctorName(),
                a.getAppointmentTime().toLocalDateTime()
                        .format(displayFormatter),
                displayedStatus
            });

            visibleCount++;
        }

        countLabel.setText("Appointments shown: " + visibleCount);
    }

    private void showError(Exception ex) {
        String message = ex.getMessage();

        if (message != null && message.contains("ORA-00001")) {
            message = "This appointment conflicts with an existing booking.";
        } else if (message != null && message.contains("ORA-02291")) {
            message = "The selected patient or doctor does not exist.";
        } else if (message == null || message.isBlank()) {
            message = "An unexpected error occurred.";
        }

        messageLabel.setText("Operation failed.");

        JOptionPane.showMessageDialog(
                this,
                message,
                "Appointment Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private static class Choice {
        private final int id;
        private final String name;

        private Choice(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return id + " - " + name;
        }
    }
}
