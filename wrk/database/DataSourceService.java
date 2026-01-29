/* --------------------------------------------------------------------------
 * Project: CAR Database API
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.database.api;

import javax.sql.DataSource;

/**
 * OSGi service interface for providing database access.
 * 
 * <p>This service provides a JDBC DataSource that can be used by other bundles
 * to access the database. The implementation handles connection pooling,
 * transaction management, and database lifecycle.
 * 
 * @version 1.0.0 25.01.2026 16:00:00
 */
public interface DataSourceService {

    /**
     * Gets the JDBC DataSource for database access.
     * 
     * @return the configured DataSource
     */
    DataSource getDataSource();
    
    /**
     * Gets the database product name (e.g., "H2", "PostgreSQL").
     * 
     * @return the database product name
     */
    String getDatabaseProductName();
    
    /**
     * Gets the database product version.
     * 
     * @return the database version
     */
    String getDatabaseVersion();
    
    /**
     * Gets the JDBC URL being used.
     * 
     * @return the JDBC URL
     */
    String getJdbcUrl();
    
    /**
     * Checks if the database is currently available.
     * 
     * @return true if database connection is available
     */
    boolean isAvailable();
    
    /**
     * Gets statistics about the connection pool.
     * 
     * @return connection pool statistics as formatted string
     */
    String getPoolStatistics();
}
