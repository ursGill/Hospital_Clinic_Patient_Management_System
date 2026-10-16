
package com.hcpms;

import javax.swing.JOptionPane;  //Dialog boxes
import javax.swing.SwingUtilities;  // thread- Safety(ensure gui code runs on Event Dispatch Thread)

public class Main {

    public static void main(String[] args) {
        try {
            DatabaseConnection database = new DatabaseConnection();

            PatientManager patientManager =
                    new PatientManager(database);

            DoctorManager doctorManager =
                    new DoctorManager(database);

             AppointmentManager appointmentManager =
                new AppointmentManager(database);

            SwingUtilities.invokeLater(() -> {
                MainFrame frame =
                        new MainFrame(patientManager, doctorManager,appointmentManager, database);
                frame.setVisible(true);
            });

        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(
                    null,
                    ex.getMessage(),
                    "Configuration Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
