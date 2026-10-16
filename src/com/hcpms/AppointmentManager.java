
package com.hcpms;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AppointmentManager {
    private final DatabaseConnection database;

    public AppointmentManager(DatabaseConnection database) {
        this.database = database;
    }

    public void addAppointment(int patientId, int doctorId,
                               Timestamp appointmentTime)
            throws SQLException {

        if (appointmentTime == null ||
            !appointmentTime.after(new Timestamp(System.currentTimeMillis()))) {
            throw new IllegalArgumentException(
                "Appointment must be scheduled for a future time.");
        }

        String sql = "INSERT INTO appointments " +
                     "(patient_id, doctor_id, appointment_time) " +
                     "VALUES (?, ?, ?)";

        try (Connection con = database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            ps.setTimestamp(3, appointmentTime);
            ps.executeUpdate();
        }
    }

    public List<Appointment> getAllAppointments() throws SQLException {
        List<Appointment> appointments = new ArrayList<>();

        String sql = "SELECT a.appointment_id, a.patient_id, " +
                     "p.full_name AS patient_name, a.doctor_id, " +
                     "d.full_name AS doctor_name, a.appointment_time, a.status " +
                     "FROM appointments a " +
                     "JOIN patients p ON a.patient_id = p.patient_id " +
                     "JOIN doctors d ON a.doctor_id = d.doctor_id " +
                     "ORDER BY a.appointment_time";

        try (Connection con = database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                appointments.add(new Appointment(
                    rs.getInt("appointment_id"),
                    rs.getInt("patient_id"),
                    rs.getString("patient_name"),
                    rs.getInt("doctor_id"),
                    rs.getString("doctor_name"),
                    rs.getTimestamp("appointment_time"),
                    rs.getString("status")
                ));
            }
        }

        return appointments;
    }

    public void updateStatus(int appointmentId, String status)
            throws SQLException {

        if (!List.of("SCHEDULED", "COMPLETED", "CANCELLED")
                  .contains(status)) {
            throw new IllegalArgumentException("Invalid appointment status.");
        }

        String sql = "UPDATE appointments SET status = ? " +
                     "WHERE appointment_id = ?";

        try (Connection con = database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, appointmentId);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Appointment not found.");
            }
        }
    }
}
