package id.ac.stis.pbo.demo1.database;

/**
 * Database configuration class
 */
public class DatabaseConfig {
    // Database configuration
    public static final String DB_HOST = "localhost";
    public static final String DB_PORT = "3306";
    public static final String DB_NAME = "gawe_db";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "";  // Default empty password for root

    static {
        // Print database configuration on startup
        System.out.println("Database Configuration:");
        System.out.println("Host: " + DB_HOST);
        System.out.println("Port: " + DB_PORT);
        System.out.println("Database: " + DB_NAME);
        System.out.println("Username: " + DB_USER);
        System.out.println("JDBC URL: " + getJdbcUrl());
    }
    
    // Connection pool settings
    public static final int MAX_POOL_SIZE = 20;
    public static final int MIN_IDLE = 5;
    public static final int CONNECTION_TIMEOUT = 30000;
    public static final int IDLE_TIMEOUT = 600000;
    public static final int MAX_LIFETIME = 1800000;
    
    private DatabaseConfig() {
        // Private constructor to prevent instantiation
    }
    
    public static String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                DB_HOST, DB_PORT, DB_NAME);
    }
}
