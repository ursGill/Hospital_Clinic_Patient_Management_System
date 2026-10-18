package com.hcpms;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        UIStyle.applyTheme();
        try {
            DatabaseConnection database = new DatabaseConnection();
            PatientManager patientManager = new PatientManager(database);
            DoctorManager doctorManager = new DoctorManager(database);
            AppointmentManager appointmentManager = new AppointmentManager(database);
            AuthManager authManager = new AuthManager(database);

            SwingUtilities.invokeLater(() -> {
                LoginFrame login = new LoginFrame(authManager, role -> {
                    MainFrame frame = new MainFrame(
                            patientManager, doctorManager,
                            appointmentManager, database);
                    frame.setVisible(true);
                });
                login.setVisible(true);
            });
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(),
                    "Configuration Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
