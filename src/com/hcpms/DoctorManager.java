package com.hcpms;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorManager {
    private final DatabaseConnection database;

    public DoctorManager(DatabaseConnection database) {
        this.database = database;
    }

    public int addDoctor(Doctor doctor) throws SQLException {
        validate(doctor);

        String sql = "INSERT INTO doctors " +
            "(full_name, specialization, phone, consultation_fee) " +
            "VALUES (?, ?, ?, ?)";

        try (Connection con = database.getConnection();
             PreparedStatement ps =
                 con.prepareStatement(sql, new String[]{"DOCTOR_ID"})) {

            bindDoctor(ps, doctor);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        throw new SQLException("Doctor ID was not returned.");
    }

    public List<Doctor> getAllDoctors() throws SQLException {
        String sql = "SELECT * FROM doctors ORDER BY doctor_id DESC";
        List<Doctor> doctors = new ArrayList<>();

        try (Connection con = database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                doctors.add(readDoctor(rs));
            }
        }

        return doctors;
    }

    public List<Doctor> searchDoctors(String query) throws SQLException {
        if (query == null || query.isBlank()) {
            return getAllDoctors();
        }

        String sql = "SELECT * FROM doctors " +
            "WHERE LOWER(full_name) LIKE LOWER(?) " +
            "OR LOWER(specialization) LIKE LOWER(?) " +
            "ORDER BY doctor_id DESC";

        List<Doctor> doctors = new ArrayList<>();

        try (Connection con = database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String pattern = "%" + query.trim() + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    doctors.add(readDoctor(rs));
                }
            }
        }

        return doctors;
    }

    public boolean updateDoctor(Doctor doctor) throws SQLException {
        if (doctor.getDoctorId() <= 0) {
            throw new IllegalArgumentException("Select a doctor before updating.");
        }

        validate(doctor);

        String sql = "UPDATE doctors SET full_name=?, " +
            "specialization=?, phone=?, consultation_fee=? " +
            "WHERE doctor_id=?";

        try (Connection con = database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            bindDoctor(ps, doctor);
            ps.setInt(5, doctor.getDoctorId());
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Deletes a doctor only when the database allows it.
     * If appointments reference this doctor, Oracle's foreign-key constraint
     * rejects the delete and the caller can explain that to the user.
     */
    public boolean deleteDoctor(int doctorId) throws SQLException {
        if (doctorId <= 0) {
            throw new IllegalArgumentException("Select a doctor before deleting.");
        }

        String sql = "DELETE FROM doctors WHERE doctor_id = ?";
        try (Connection con = database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            return ps.executeUpdate() == 1;
        }
    }

    private void validate(Doctor d) {
        if (d == null || d.getFullName() == null || d.getFullName().isBlank()) {
            throw new IllegalArgumentException("Doctor name is required.");
        }
        if (d.getFullName().trim().length() > 100) {
            throw new IllegalArgumentException("Name must be at most 100 characters.");
        }
        if (d.getSpecialization() == null || d.getSpecialization().isBlank()) {
            throw new IllegalArgumentException("Specialization is required.");
        }
        if (d.getSpecialization().trim().length() > 80) {
            throw new IllegalArgumentException("Specialization must be at most 80 characters.");
        }

        String phone = d.getPhone() == null ? "" : d.getPhone().trim();
        if (!phone.matches("[0-9]{10,15}")) {
            throw new IllegalArgumentException("Phone must contain 10 to 15 digits.");
        }
        if (!Double.isFinite(d.getConsultationFee()) || d.getConsultationFee() < 0) {
            throw new IllegalArgumentException("Fee must be a non-negative number.");
        }

        d.setFullName(d.getFullName().trim());
        d.setSpecialization(d.getSpecialization().trim());
        d.setPhone(phone);
    }

    private void bindDoctor(PreparedStatement ps, Doctor d) throws SQLException {
        ps.setString(1, d.getFullName());
        ps.setString(2, d.getSpecialization());
        ps.setString(3, d.getPhone());
        ps.setDouble(4, d.getConsultationFee());
    }

    private Doctor readDoctor(ResultSet rs) throws SQLException {
        return new Doctor(
            rs.getInt("doctor_id"),
            rs.getString("full_name"),
            rs.getString("specialization"),
            rs.getString("phone"),
            rs.getDouble("consultation_fee"),
            rs.getTimestamp("created_at")
        );
    }
}
