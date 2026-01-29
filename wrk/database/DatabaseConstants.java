/* --------------------------------------------------------------------------
 * Project: CAR Database API
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.database.api;

/**
 * Constants for database configuration and operations.
 * 
 * @version 1.0.0 25.01.2026 16:00:00
 */
public interface DatabaseConstants {

    /**
     * Configuration PID for the database service.
     */
    String CONFIG_PID = "biz.car.database";
    
    /**
     * Property key for JDBC URL.
     */
    String PROP_JDBC_URL = "jdbc.url";
    
    /**
     * Property key for database user.
     */
    String PROP_DB_USER = "db.user";
    
    /**
     * Property key for database password.
     */
    String PROP_DB_PASSWORD = "db.password";
    
    /**
     * Property key for database driver class.
     */
    String PROP_DRIVER_CLASS = "driver.class";
    
    /**
     * Property key for connection pool minimum size.
     */
    String PROP_POOL_MIN_SIZE = "pool.minSize";
    
    /**
     * Property key for connection pool maximum size.
     */
    String PROP_POOL_MAX_SIZE = "pool.maxSize";
    
    /**
     * Property key for connection timeout in milliseconds.
     */
    String PROP_CONNECTION_TIMEOUT = "connection.timeout";
    
    /**
     * Property key for idle timeout in milliseconds.
     */
    String PROP_IDLE_TIMEOUT = "idle.timeout";
    
    /**
     * Property key for maximum connection lifetime in milliseconds.
     */
    String PROP_MAX_LIFETIME = "max.lifetime";
    
    /**
     * Property key for auto-commit setting.
     */
    String PROP_AUTO_COMMIT = "auto.commit";
    
    /**
     * Property key for database initialization mode.
     * Values: "create", "update", "none"
     */
    String PROP_INIT_MODE = "init.mode";
    
    /**
     * Default JDBC URL for H2 embedded database.
     */
    String DEFAULT_JDBC_URL = "jdbc:h2:./H2/IAM;IFEXISTS=FALSE";
    
    /**
     * Default database user.
     */
    String DEFAULT_DB_USER = "sa";
    
    /**
     * Default database password (empty).
     */
    String DEFAULT_DB_PASSWORD = "";
    
    /**
     * Default H2 driver class.
     */
    String DEFAULT_DRIVER_CLASS = "org.h2.Driver";
    
    /**
     * Default minimum pool size.
     */
    int DEFAULT_POOL_MIN_SIZE = 2;
    
    /**
     * Default maximum pool size.
     */
    int DEFAULT_POOL_MAX_SIZE = 10;
    
    /**
     * Default connection timeout (30 seconds).
     */
    long DEFAULT_CONNECTION_TIMEOUT = 30000;
    
    /**
     * Default idle timeout (10 minutes).
     */
    long DEFAULT_IDLE_TIMEOUT = 600000;
    
    /**
     * Default max lifetime (30 minutes).
     */
    long DEFAULT_MAX_LIFETIME = 1800000;
}
