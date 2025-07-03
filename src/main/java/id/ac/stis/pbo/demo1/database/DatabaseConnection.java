package id.ac.stis.pbo.demo1.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Singleton class to manage database connections using HikariCP
 */
public class DatabaseConnection {
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());
    private static DatabaseConnection instance;
    private HikariDataSource dataSource;

    private DatabaseConnection() {
        initializePool();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    private void initializePool() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DatabaseConfig.getJdbcUrl());
            config.setUsername(DatabaseConfig.DB_USER);
            config.setPassword(DatabaseConfig.DB_PASSWORD);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // Connection pool settings
            config.setMaximumPoolSize(DatabaseConfig.MAX_POOL_SIZE);
            config.setMinimumIdle(DatabaseConfig.MIN_IDLE);
            config.setConnectionTimeout(DatabaseConfig.CONNECTION_TIMEOUT);
            config.setIdleTimeout(DatabaseConfig.IDLE_TIMEOUT);
            config.setMaxLifetime(DatabaseConfig.MAX_LIFETIME);
            
            // Additional configuration for better stability
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            
            dataSource = new HikariDataSource(config);
            logger.info("Database connection pool initialized successfully");
        } catch (Exception e) {
            logger.severe("Failed to initialize database connection pool: " + e.getMessage());
            throw new DatabaseException.ConnectionException("Failed to initialize connection pool", e);
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DatabaseException.ConnectionException("Failed to get database connection", e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }

    // Register a shutdown hook instead of using finalize()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }
}
