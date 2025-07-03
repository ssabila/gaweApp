package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestDatabaseConnection {
    public static void main(String[] args) {
        System.out.println("=== Testing MySQL Database Connection ===");

        String url = "jdbc:mysql://localhost:3306/gawe_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String username = "root";
        String password = "";

        try {
            System.out.println("Attempting to connect to: " + url);
            System.out.println("Username: " + username);
            System.out.println("Password: " + (password.isEmpty() ? "(empty)" : "(set)"));

            Connection connection = DriverManager.getConnection(url, username, password);
            System.out.println("✅ Database connection successful!");

            // Test query
            var stmt = connection.createStatement();
            var rs = stmt.executeQuery("SELECT COUNT(*) as count FROM employees");
            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("✅ Found " + count + " employees in database");
            }

            connection.close();
            System.out.println("✅ Connection closed successfully");

        } catch (SQLException e) {
            System.err.println("❌ Database connection failed!");
            System.err.println("Error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());

            if (e.getMessage().contains("Unknown database")) {
                System.out.println("\n🔧 Solution: Create database manually:");
                System.out.println("mysql -u root -p");
                System.out.println("CREATE DATABASE gawe_db;");
            }

            if (e.getMessage().contains("Access denied")) {
                System.out.println("\n🔧 Solution: Check MySQL credentials:");
                System.out.println("Update DatabaseConfig.java with correct username/password");
            }
        }
    }
}