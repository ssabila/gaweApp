package id.ac.stis.pbo.demo1.database;

/**
 * Custom exception class for database-related errors
 */
public class DatabaseException extends RuntimeException {
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DatabaseException(Throwable cause) {
        super("Database operation failed: " + cause.getMessage(), cause);
    }
    
    public static class ConnectionException extends DatabaseException {
        public ConnectionException(String message, Throwable cause) {
            super("Failed to connect to database: " + message, cause);
        }
    }
    
    public static class InitializationException extends DatabaseException {
        public InitializationException(String message, Throwable cause) {
            super("Failed to initialize database: " + message, cause);
        }
    }
    
    public static class AuthenticationException extends DatabaseException {
        public AuthenticationException(String message) {
            super("Authentication failed: " + message);
        }
    }
    
    public static class QueryException extends DatabaseException {
        public QueryException(String message, Throwable cause) {
            super("Query execution failed: " + message, cause);
        }
    }
}
