package com.hcpms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseConnectionTest {
    public static void main(String[] args) {
        try (Connection connection = new DatabaseConnection().getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                 "SELECT USER, SYS_CONTEXT('USERENV', 'SERVICE_NAME') FROM dual")) {
            if (result.next()) {
                System.out.println("Connection successful.");
                System.out.println("Database user: " + result.getString(1));
                System.out.println("Service name: " + result.getString(2));
            }
        } catch (Exception ex) {
            System.err.println("Connection test failed: " + ex.getMessage());
            System.err.println("Check the JDBC URL, Oracle listener, username and password.");
            System.exit(1);
        }
    }
}
