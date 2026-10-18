package com.hcpms;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Handles HCPMS login and initial administrator setup. */
public class AuthManager {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    private final DatabaseConnection database;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthManager(DatabaseConnection database) {
        this.database = database;
    }

    public boolean hasAnyUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM app_users";
        try (Connection con = database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    /** Creates the first administrator only while the user table is empty. */
    public void createInitialAdmin(String username, char[] password)
            throws SQLException, GeneralSecurityException {
        String cleanUsername = validateUsername(username);
        validatePassword(password);

        try (Connection con = database.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement lock = con.prepareStatement(
                        "LOCK TABLE app_users IN EXCLUSIVE MODE")) {
                    lock.execute();
                }

                try (PreparedStatement check = con.prepareStatement(
                        "SELECT COUNT(*) FROM app_users");
                     ResultSet rs = check.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) != 0) {
                        throw new IllegalStateException(
                                "An account already exists. Please log in.");
                    }
                }

                byte[] salt = new byte[SALT_LENGTH_BYTES];
                secureRandom.nextBytes(salt);
                String saltText = Base64.getEncoder().encodeToString(salt);
                String hashText = Base64.getEncoder().encodeToString(
                        hashPassword(password, salt));

                String insert = "INSERT INTO app_users "
                        + "(username, password_hash, password_salt, role, is_active) "
                        + "VALUES (?, ?, ?, 'ADMIN', 'Y')";
                try (PreparedStatement ps = con.prepareStatement(insert)) {
                    ps.setString(1, cleanUsername);
                    ps.setString(2, hashText);
                    ps.setString(3, saltText);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException | RuntimeException | GeneralSecurityException ex) {
                try { con.rollback(); } catch (SQLException ignored) { }
                throw ex;
            } finally {
                try { con.setAutoCommit(true); } catch (SQLException ignored) { }
            }
        }
    }

    /** Returns the authenticated account, or null for invalid credentials. */
    public AppUser authenticate(String username, char[] password)
            throws SQLException, GeneralSecurityException {
        if (username == null || username.isBlank() || password == null
                || password.length == 0) {
            return null;
        }

        String sql = "SELECT user_id, username, password_hash, password_salt, role, doctor_id "
                + "FROM app_users WHERE username = ? AND is_active = 'Y'";
        try (Connection con = database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                byte[] salt = Base64.getDecoder().decode(rs.getString("password_salt"));
                byte[] expected = Base64.getDecoder().decode(rs.getString("password_hash"));
                byte[] actual = hashPassword(password, salt);
                if (java.security.MessageDigest.isEqual(expected, actual)) {
                    int doctorIdValue = rs.getInt("doctor_id");
                    Integer doctorId = rs.wasNull() ? null : doctorIdValue;
                    String role = rs.getString("role");
                    if ("DOCTOR".equals(role) && doctorId == null) {
                        throw new SQLException("This doctor account is not linked to a doctor record. Ask an administrator to correct it.");
                    }
                    return new AppUser(rs.getInt("user_id"), rs.getString("username"), role, doctorId);
                }
                return null;
            }
        }
    }

    public void createStaffUser(String username, char[] password, String role, Integer doctorId)
            throws SQLException, GeneralSecurityException {
        String cleanUsername = validateUsername(username);
        validatePassword(password);
        if (!"DOCTOR".equals(role) && !"RECEPTIONIST".equals(role)) {
            throw new IllegalArgumentException("Choose DOCTOR or RECEPTIONIST for a staff account.");
        }
        if ("DOCTOR".equals(role) && (doctorId == null || doctorId <= 0)) {
            throw new IllegalArgumentException("Select the doctor record linked to this account.");
        }
        if ("RECEPTIONIST".equals(role) && doctorId != null) {
            throw new IllegalArgumentException("Receptionist accounts cannot be linked to a doctor record.");
        }
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        String hashText = Base64.getEncoder().encodeToString(hashPassword(password, salt));
        String saltText = Base64.getEncoder().encodeToString(salt);
        String sql = "INSERT INTO app_users (username, password_hash, password_salt, role, is_active, doctor_id) VALUES (?, ?, ?, ?, 'Y', ?)";
        try (Connection con = database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cleanUsername); ps.setString(2, hashText); ps.setString(3, saltText); ps.setString(4, role);
            if (doctorId == null) ps.setNull(5, java.sql.Types.NUMERIC); else ps.setInt(5, doctorId);
            ps.executeUpdate();
        }
    }

    public java.util.List<String[]> getUsers() throws SQLException {
        java.util.List<String[]> users = new java.util.ArrayList<>();
        String sql = "SELECT user_id, username, role, is_active, doctor_id FROM app_users ORDER BY user_id";
        try (Connection con = database.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int did = rs.getInt("doctor_id");
                boolean doctorIdNull = rs.wasNull();
                users.add(new String[]{String.valueOf(rs.getInt("user_id")), rs.getString("username"),
                    rs.getString("role"), rs.getString("is_active"), doctorIdNull ? "" : String.valueOf(did)});
            }
        }
        return users;
    }

    public boolean deactivateUser(int userId, String currentUsername) throws SQLException {
        String checkSql = "SELECT username, role, is_active FROM app_users WHERE user_id = ?";
        try (Connection con = database.getConnection(); PreparedStatement check = con.prepareStatement(checkSql)) {
            check.setInt(1, userId);
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) return false;
                if (rs.getString("username").equalsIgnoreCase(currentUsername)) {
                    throw new IllegalArgumentException("You cannot deactivate your own account while logged in.");
                }
                if ("ADMIN".equals(rs.getString("role")) && "Y".equals(rs.getString("is_active"))) {
                    try (PreparedStatement count = con.prepareStatement("SELECT COUNT(*) FROM app_users WHERE role='ADMIN' AND is_active='Y'"); ResultSet cr = count.executeQuery()) {
                        cr.next();
                        if (cr.getInt(1) <= 1) throw new IllegalArgumentException("The last active administrator cannot be deactivated.");
                    }
                }
            }
        }
        try (Connection con = database.getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE app_users SET is_active='N' WHERE user_id=? AND is_active='Y'")) {
            ps.setInt(1, userId); return ps.executeUpdate() == 1;
        }
    }

    private static String validateUsername(String username) {
        if (username == null) throw new IllegalArgumentException("Username is required.");
        String value = username.trim();
        if (!value.matches("[A-Za-z0-9._-]{4,50}")) {
            throw new IllegalArgumentException(
                    "Username must be 4-50 characters and use only letters, numbers, dot, underscore, or hyphen.");
        }
        return value;
    }

    private static void validatePassword(char[] password) {
        if (password == null || password.length < 10) {
            throw new IllegalArgumentException("Password must contain at least 10 characters.");
        }
        if (password.length > 128) {
            throw new IllegalArgumentException("Password must be no longer than 128 characters.");
        }
    }

    private static byte[] hashPassword(char[] password, byte[] salt)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}
