
package com.hcpms;

import java.awt.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class AppointmentPanel extends JPanel {

    private final PatientManager patientManager;
    private final DoctorManager doctorManager;
    private final AppointmentManager appointmentManager;

    private final JComboBox<Choice> patientBox = new JComboBox<>();
    private final JComboBox<Choice> doctorBox = new JComboBox<>();
    private final JTextField dateTimeField =
            new JTextField(20);

    private final JComboBox<String> statusBox =
            new JComboBox<>(new String[]{
                "SCHEDULED", "COMPLETED", "CANCELLED"
            });

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

    public AppointmentPanel(
            PatientManager patientManager,
            DoctorManager doctorManager,
            AppointmentManager appointmentManager) {

        this.patientManager = patientManager;
        this.doctorManager = doctorManager;
        this.appointmentManager = appointmentManager;

        setLayout(new BorderLayout(10, 10));

        add(createForm(), BorderLayout.NORTH);

        table.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);

        add(new JScrollPane(table), BorderLayout.CENTER);

        loadDropdowns();
        loadAppointments();
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                "Schedule Appointment"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 7, 5, 7);
        c.anchor = GridBagConstraints.WEST;

        addField(form, c, 0, "Patient:", patientBox);
        addField(form, c, 1, "Doctor:", doctorBox);
        addField(form, c, 2, "Date and time:", dateTimeField);
        addField(form, c, 3, "New status:", statusBox);

        dateTimeField.setToolTipText(
                "Format: yyyy-MM-dd HH:mm:ss");

        JButton scheduleButton =
                new JButton("Schedule Appointment");
        JButton updateButton =
                new JButton("Update Status");
        JButton refreshButton =
                new JButton("Refresh");
        
        JPanel buttons = new JPanel(new FlowLayout(
                FlowLayout.LEFT));
        buttons.add(scheduleButton);
        buttons.add(updateButton);
        buttons.add(refreshButton);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        form.add(buttons, c);

        scheduleButton.addActionListener(e -> scheduleAppointment());
        updateButton.addActionListener(e -> updateStatus());
        refreshButton.addActionListener(e -> {
            loadDropdowns();
            loadAppointments();
        });

        return form;
    }

    private void addField(
            JPanel panel, GridBagConstraints c,
            int row, String label, JComponent field) {

        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 1;
        panel.add(new JLabel(label), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        panel.add(field, c);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
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

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void scheduleAppointment() {
        Choice patient = (Choice) patientBox.getSelectedItem();
        Choice doctor = (Choice) doctorBox.getSelectedItem();

        if (patient == null || doctor == null) {
            JOptionPane.showMessageDialog(this,
                    "Register at least one patient and one doctor first.");
            return;
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(
                    dateTimeField.getText().trim(),
                    DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd HH:mm:ss"));

            Timestamp timestamp = Timestamp.valueOf(dateTime);

            appointmentManager.addAppointment(
                    patient.id, doctor.id, timestamp);

            JOptionPane.showMessageDialog(this,
                    "Appointment scheduled successfully.");

            dateTimeField.setText("");
            loadAppointments();

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    "Enter a valid future date and time.\n"
                    + "Required format: yyyy-MM-dd HH:mm:ss");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void updateStatus() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                    "Select an appointment from the table.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int appointmentId = Integer.parseInt(
                tableModel.getValueAt(modelRow, 0).toString());

        String status = (String) statusBox.getSelectedItem();

        try {
            appointmentManager.updateStatus(
                    appointmentId, status);

            JOptionPane.showMessageDialog(this,
                    "Appointment status updated.");

            loadAppointments();

        } catch (SQLException | IllegalArgumentException ex) {
            showError(ex);
        }
    }

    private void loadAppointments() {
        try {
            tableModel.setRowCount(0);

            List<Appointment> appointments =
                    appointmentManager.getAllAppointments();

            for (Appointment a : appointments) {
                tableModel.addRow(new Object[]{
                    a.getAppointmentId(),
                    a.getPatientName(),
                    a.getDoctorName(),
                    a.getAppointmentTime(),
                    a.getStatus()
                });
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        String message = ex.getMessage();

        if (message != null && message.contains("ORA-00001")) {
            message = "This doctor already has an appointment "
                    + "at that exact date and time.";
        } else if (message != null
                && message.contains("ORA-02291")) {
            message = "The selected patient or doctor "
                    + "does not exist.";
        } else if (message == null) {
            message = "An unexpected error occurred.";
        }

        JOptionPane.showMessageDialog(this,
                message, "Appointment Error",
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
