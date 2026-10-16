package com.hcpms;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientManager {
    private final DatabaseConnection database;

    public PatientManager(DatabaseConnection database) {
        this.database = database;
    }

    public int registerPatient(Patient patient) throws SQLException {
        validate(patient);
        String sql = "INSERT INTO patients (full_name, age, gender, phone, address) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, new String[]{"PATIENT_ID"})) {
            fillStatement(statement, patient);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Patient was inserted but the generated ID was not returned.");
    }

    public List<Patient> getAllPatients() throws SQLException {
        String sql = "SELECT patient_id, full_name, age, gender, phone, address, created_at " +
                     "FROM patients ORDER BY patient_id DESC";
        List<Patient> patients = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) patients.add(readPatient(result));
        }
        return patients;
    }

    public List<Patient> searchPatients(String query) throws SQLException {
        if (query == null || query.isBlank()) return getAllPatients();
        String sql = "SELECT patient_id, full_name, age, gender, phone, address, created_at " +
                     "FROM patients WHERE LOWER(full_name) LIKE LOWER(?) OR phone LIKE ? " +
                     "ORDER BY patient_id DESC";
        List<Patient> patients = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String pattern = "%" + query.trim() + "%";
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) patients.add(readPatient(result));
            }
        }
        return patients;
    }

    public boolean updatePatient(Patient patient) throws SQLException {
        if (patient.getPatientId() <= 0) {
            throw new IllegalArgumentException("Select a patient before updating.");
        }
        validate(patient);
        String sql = "UPDATE patients SET full_name=?, age=?, gender=?, phone=?, address=? WHERE patient_id=?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, patient);
            statement.setInt(6, patient.getPatientId());
            return statement.executeUpdate() == 1;
        }
    }

    private void validate(Patient patient) {
        if (patient == null) throw new IllegalArgumentException("Patient details are required.");
        if (patient.getFullName() == null || patient.getFullName().isBlank())
            throw new IllegalArgumentException("Full name is required.");
        if (patient.getFullName().trim().length() > 100)
            throw new IllegalArgumentException("Name must be 100 characters or fewer.");
        if (patient.getAge() < 0 || patient.getAge() > 120)
            throw new IllegalArgumentException("Age must be between 0 and 120.");
        if (patient.getGender() == null || patient.getGender().isBlank())
            throw new IllegalArgumentException("Select a gender.");
        String phone = patient.getPhone() == null ? "" : patient.getPhone().trim();
        if (!phone.matches("[0-9]{10,15}"))
            throw new IllegalArgumentException("Phone must contain 10 to 15 digits.");
        if (patient.getAddress() != null && patient.getAddress().length() > 250)
            throw new IllegalArgumentException("Address must be 250 characters or fewer.");
        patient.setFullName(patient.getFullName().trim());
        patient.setPhone(phone);
        if (patient.getAddress() != null) patient.setAddress(patient.getAddress().trim());
    }

    private void fillStatement(PreparedStatement statement, Patient patient) throws SQLException {
        statement.setString(1, patient.getFullName());
        statement.setInt(2, patient.getAge());
        statement.setString(3, patient.getGender());
        statement.setString(4, patient.getPhone());
        statement.setString(5, patient.getAddress());
    }

    private Patient readPatient(ResultSet result) throws SQLException {
        return new Patient(result.getInt("patient_id"), result.getString("full_name"),
            result.getInt("age"), result.getString("gender"), result.getString("phone"),
            result.getString("address"), result.getTimestamp("created_at"));
    }
}
