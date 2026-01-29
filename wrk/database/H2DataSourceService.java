/* --------------------------------------------------------------------------
 * Project: CAR Database H2 Implementation
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.database.h2;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import biz.car.database.api.DataSourceService;
import biz.car.database.api.DatabaseConstants;

/**
 * H2 Database DataSource Service implementation using HikariCP connection pool.
 * 
 * <p>This service provides a pooled DataSource for H2 database access.
 * It uses HikariCP for high-performance connection pooling and can be
 * configured via Configuration Admin.
 * 
 * @version 1.0.0 25.01.2026 16:00:00
 */
@Component(
    immediate = true,
    configurationPid = DatabaseConstants.CONFIG_PID,
    configurationPolicy = ConfigurationPolicy.OPTIONAL,
    service = DataSourceService.class,
    property = {
        "database.type=H2",
        "service.description=H2 Database DataSource Service"
    }
)
@Designate(ocd = H2DataSourceService.Config.class)
public class H2DataSourceService implements DataSourceService {

    @ObjectClassDefinition(
        name = "H2 Database Configuration",
        description = "Configuration for H2 Database DataSource"
    )
    public @interface Config {
        
        @AttributeDefinition(
            name = "JDBC URL",
            description = "JDBC connection URL for H2 database"
        )
        String jdbc_url() default DatabaseConstants.DEFAULT_JDBC_URL;
        
        @AttributeDefinition(
            name = "Database User",
            description = "Database username"
        )
        String db_user() default DatabaseConstants.DEFAULT_DB_USER;
        
        @AttributeDefinition(
            name = "Database Password",
            description = "Database password"
        )
        String db_password() default DatabaseConstants.DEFAULT_DB_PASSWORD;
        
        @AttributeDefinition(
            name = "Pool Minimum Size",
            description = "Minimum number of connections in pool"
        )
        int pool_minSize() default DatabaseConstants.DEFAULT_POOL_MIN_SIZE;
        
        @AttributeDefinition(
            name = "Pool Maximum Size",
            description = "Maximum number of connections in pool"
        )
        int pool_maxSize() default DatabaseConstants.DEFAULT_POOL_MAX_SIZE;
        
        @AttributeDefinition(
            name = "Connection Timeout",
            description = "Connection timeout in milliseconds"
        )
        long connection_timeout() default DatabaseConstants.DEFAULT_CONNECTION_TIMEOUT;
        
        @AttributeDefinition(
            name = "Idle Timeout",
            description = "Idle timeout in milliseconds"
        )
        long idle_timeout() default DatabaseConstants.DEFAULT_IDLE_TIMEOUT;
        
        @AttributeDefinition(
            name = "Max Lifetime",
            description = "Maximum lifetime of connection in milliseconds"
        )
        long max_lifetime() default DatabaseConstants.DEFAULT_MAX_LIFETIME;
        
        @AttributeDefinition(
            name = "Auto Commit",
            description = "Enable auto-commit for connections"
        )
        boolean auto_commit() default true;
        
        @AttributeDefinition(
            name = "Initialization Mode",
            description = "Database initialization mode: create, update, none"
        )
        String init_mode() default "create";
    }
    
    private HikariDataSource dataSource;
    private String jdbcUrl;
    private String databaseProductName;
    private String databaseVersion;
    
    @Activate
    protected void activate(Config aConfig) throws SQLException {
        System.out.println("========================================");
        System.out.println("Activating H2 DataSource Service");
        System.out.println("========================================");
        
        configure(aConfig);
        
        System.out.println("H2 DataSource Service activated");
        System.out.println("JDBC URL: " + jdbcUrl);
        System.out.println("Database: " + databaseProductName + " " + databaseVersion);
        System.out.println("Pool: min=" + aConfig.pool_minSize() + ", max=" + aConfig.pool_maxSize());
        System.out.println("========================================");
    }
    
    @Modified
    protected void modified(Config aConfig) throws SQLException {
        System.out.println("H2 DataSource Service configuration modified");
        deactivate();
        activate(aConfig);
    }
    
    @Deactivate
    protected void deactivate() {
        System.out.println("========================================");
        System.out.println("Deactivating H2 DataSource Service");
        
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("DataSource closed");
        }
        
        dataSource = null;
        System.out.println("H2 DataSource Service deactivated");
        System.out.println("========================================");
    }
    
    @Override
    public DataSource getDataSource() {
        return dataSource;
    }
    
    @Override
    public String getDatabaseProductName() {
        return databaseProductName;
    }
    
    @Override
    public String getDatabaseVersion() {
        return databaseVersion;
    }
    
    @Override
    public String getJdbcUrl() {
        return jdbcUrl;
    }
    
    @Override
    public boolean isAvailable() {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }
        
        try (Connection l_conn = dataSource.getConnection()) {
            return l_conn.isValid(5);
        } catch (SQLException anEx) {
            return false;
        }
    }
    
    @Override
    public String getPoolStatistics() {
        if (dataSource == null) {
            return "DataSource not initialized";
        }
        
        HikariPoolMXBean l_pool = dataSource.getHikariPoolMXBean();
        
        StringBuilder l_sb = new StringBuilder();
        l_sb.append("Pool Statistics:\n");
        l_sb.append("  Active Connections: ").append(l_pool.getActiveConnections()).append("\n");
        l_sb.append("  Idle Connections: ").append(l_pool.getIdleConnections()).append("\n");
        l_sb.append("  Total Connections: ").append(l_pool.getTotalConnections()).append("\n");
        l_sb.append("  Threads Awaiting: ").append(l_pool.getThreadsAwaitingConnection()).append("\n");
        
        return l_sb.toString();
    }
    
    /**
     * Configures the HikariCP DataSource.
     */
    private void configure(Config aConfig) throws SQLException {
        HikariConfig l_config = new HikariConfig();
        
        // Basic connection settings
        this.jdbcUrl = aConfig.jdbc_url();
        l_config.setJdbcUrl(jdbcUrl);
        l_config.setUsername(aConfig.db_user());
        l_config.setPassword(aConfig.db_password());
        l_config.setDriverClassName(DatabaseConstants.DEFAULT_DRIVER_CLASS);
        
        // Pool settings
        l_config.setMinimumIdle(aConfig.pool_minSize());
        l_config.setMaximumPoolSize(aConfig.pool_maxSize());
        l_config.setConnectionTimeout(aConfig.connection_timeout());
        l_config.setIdleTimeout(aConfig.idle_timeout());
        l_config.setMaxLifetime(aConfig.max_lifetime());
        l_config.setAutoCommit(aConfig.auto_commit());
        
        // Pool name for monitoring
        l_config.setPoolName("CAR-H2-Pool");
        
        // Connection test query
        l_config.setConnectionTestQuery("SELECT 1");
        
        // Performance optimizations
        l_config.addDataSourceProperty("cachePrepStmts", "true");
        l_config.addDataSourceProperty("prepStmtCacheSize", "250");
        l_config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        // Create the DataSource
        dataSource = new HikariDataSource(l_config);
        
        // Get database metadata
        retrieveDatabaseMetadata();
        
        // Initialize database if needed
        initializeDatabase(aConfig.init_mode());
    }
    
    /**
     * Retrieves database metadata.
     */
    private void retrieveDatabaseMetadata() throws SQLException {
        try (Connection l_conn = dataSource.getConnection()) {
            DatabaseMetaData l_meta = l_conn.getMetaData();
            databaseProductName = l_meta.getDatabaseProductName();
            databaseVersion = l_meta.getDatabaseProductVersion();
        }
    }
    
    /**
     * Initializes the database schema based on init mode.
     */
    private void initializeDatabase(String aInitMode) {
        if ("none".equalsIgnoreCase(aInitMode)) {
            System.out.println("Database initialization: NONE (skipped)");
            return;
        }
        
        System.out.println("Database initialization mode: " + aInitMode);
        
        // TODO: Implement schema creation/update based on init mode
        // This will be used by IAM bundles to create their tables
        
        if ("create".equalsIgnoreCase(aInitMode)) {
            createSchema();
        } else if ("update".equalsIgnoreCase(aInitMode)) {
            updateSchema();
        }
    }
    
    /**
     * Creates the initial database schema.
     */
    private void createSchema() {
        System.out.println("Creating database schema...");
        
        // Example: Create a simple version table
        String l_sql = "CREATE TABLE IF NOT EXISTS DB_VERSION (" +
                      "VERSION VARCHAR(50) PRIMARY KEY, " +
                      "APPLIED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        
        try (Connection l_conn = dataSource.getConnection();
             java.sql.Statement l_stmt = l_conn.createStatement()) {
            
            l_stmt.execute(l_sql);
            
            // Insert version
            String l_insertSql = "MERGE INTO DB_VERSION (VERSION) VALUES ('1.0.0')";
            l_stmt.execute(l_insertSql);
            
            System.out.println("Database schema created");
            
        } catch (SQLException anEx) {
            System.err.println("Failed to create schema: " + anEx.getMessage());
        }
    }
    
    /**
     * Updates the database schema.
     */
    private void updateSchema() {
        System.out.println("Updating database schema...");
        
        // TODO: Implement schema migration logic
        // For now, just create if not exists
        createSchema();
    }
}
