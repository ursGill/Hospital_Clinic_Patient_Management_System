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

    /** Returns the role for a valid login, or null when credentials are invalid. */
    public String authenticate(String username, char[] password)
            throws SQLException, GeneralSecurityException {
        if (username == null || username.isBlank() || password == null
                || password.length == 0) {
            return null;
        }

        String sql = "SELECT password_hash, password_salt, role "
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
                    return rs.getString("role");
                }
                return null;
            }
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
