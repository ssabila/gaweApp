package id.ac.stis.pbo.demo1.data;

/**
 * Factory class for creating and managing DataStore instances
 */
public class DataStoreFactory {
    private static MySQLDataStore instance;
    
    private DataStoreFactory() {
        // Private constructor to prevent instantiation
    }
    
    public static synchronized MySQLDataStore getMySQLDataStore() {
        if (instance == null) {
            instance = new MySQLDataStore();
        }
        return instance;
    }
    
    public static void closeDataStore() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}
