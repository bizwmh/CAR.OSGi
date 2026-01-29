# CAR Database Service Bundles

## Übersicht

Zwei Bundle-Architektur für saubere Separation von API und Implementation:

```
car.database.api (Start Level 5)
└─ Definiert Interfaces und Konstanten

car.database.h2 (Start Level 5)  
└─ H2 Implementation mit HikariCP Connection Pool
```

## 🏗️ Architektur

### Bundle-Abhängigkeiten

```
┌─────────────────────────────────────┐
│  car.database.h2                    │
│  ├─ H2DataSourceService             │
│  ├─ HikariCP (embedded)             │
│  └─ implements DataSourceService    │
└──────────────┬──────────────────────┘
               │
               │ uses
               ↓
┌─────────────────────────────────────┐
│  car.database.api                   │
│  ├─ DataSourceService (Interface)  │
│  ├─ DatabaseConstants               │
│  └─ Export Package                  │
└─────────────────────────────────────┘
               ↑
               │ uses
               │
┌──────────────┴──────────────────────┐
│  Ihre IAM Bundles                   │
│  └─ @Reference DataSourceService    │
└─────────────────────────────────────┘
```

## 📦 Bundle Details

### car.database.api

**Zweck:** API-Definition für Database Services

**Exports:**
- `biz.car.database.api` (Version 1.0.0)

**Inhalt:**
- `DataSourceService` - Service Interface
- `DatabaseConstants` - Konfigurationskonstanten

**Start Level:** 5

### car.database.h2

**Zweck:** H2 Database Implementation mit Connection Pooling

**Provides:**
- OSGi Service: `DataSourceService`

**Embedded:**
- HikariCP 5.1.0 (Connection Pool)

**Dependencies:**
- H2 Database 2.2.224 (provided by framework)
- car.database.api 1.0.0

**Start Level:** 5

## 🚀 Installation

### 1. Projekt-Struktur erstellen

```
workspace/
├── car.database.api/
│   ├── src/main/java/biz/car/database/api/
│   │   ├── DataSourceService.java
│   │   └── DatabaseConstants.java
│   └── pom.xml
│
└── car.database.h2/
    ├── src/main/java/biz/car/database/h2/
    │   └── H2DataSourceService.java
    └── pom.xml
```

### 2. Build

```bash
# API Bundle
cd car.database.api
mvn clean install

# H2 Implementation Bundle
cd ../car.database.h2
mvn clean install
```

### 3. Deploy

```bash
# Kopieren in bundles/05/ für Start Level 5
cp car.database.api/target/car.database.api-1.0.0.jar bundles/05/
cp car.database.h2/target/car.database.h2-1.0.0.jar bundles/05/
```

### 4. Verify

```bash
# In Gogo Console:
osgi> ss | grep database
   18|Active     |    5|car.database.api (1.0.0)
   19|Active     |    5|car.database.h2 (1.0.0)

osgi> services DataSourceService
{biz.car.database.api.DataSourceService}={...}
```

## ⚙️ Konfiguration

### Via Configuration Admin

**PID:** `biz.car.database`

**Konfigurationsdatei:** `configuration/biz.car.database.cfg`

```properties
# JDBC Connection
jdbc.url=jdbc:h2:./H2/IAM;IFEXISTS=FALSE
db.user=sa
db.password=

# Connection Pool
pool.minSize=2
pool.maxSize=10
connection.timeout=30000
idle.timeout=600000
max.lifetime=1800000

# Settings
auto.commit=true
init.mode=create
```

### Standard-Konfiguration

Wenn keine Konfiguration vorhanden, werden folgende Defaults verwendet:

| Parameter | Default | Beschreibung |
|-----------|---------|--------------|
| `jdbc.url` | `jdbc:h2:./H2/IAM;IFEXISTS=FALSE` | H2 embedded DB |
| `db.user` | `sa` | Superuser |
| `db.password` | *(leer)* | Kein Passwort |
| `pool.minSize` | `2` | Min. Connections |
| `pool.maxSize` | `10` | Max. Connections |
| `connection.timeout` | `30000` | 30 Sekunden |
| `idle.timeout` | `600000` | 10 Minuten |
| `max.lifetime` | `1800000` | 30 Minuten |
| `auto.commit` | `true` | Auto-Commit an |
| `init.mode` | `create` | Schema erstellen |

## 💻 Usage - Service Injection

### Declarative Services (Empfohlen)

```java
package biz.car.identity.core;

import javax.sql.DataSource;
import org.osgi.service.component.annotations.*;
import biz.car.database.api.DataSourceService;

@Component
public class UserRepository {
    
    private DataSourceService dataSourceService;
    
    @Reference
    protected void setDataSourceService(DataSourceService aService) {
        this.dataSourceService = aService;
        System.out.println("DataSource injected: " + aService.getDatabaseProductName());
    }
    
    protected void unsetDataSourceService(DataSourceService aService) {
        this.dataSourceService = null;
    }
    
    public void saveUser(User aUser) {
        DataSource l_ds = dataSourceService.getDataSource();
        
        try (Connection l_conn = l_ds.getConnection();
             PreparedStatement l_stmt = l_conn.prepareStatement(
                 "INSERT INTO USERS (ID, USERNAME, EMAIL) VALUES (?, ?, ?)")) {
            
            l_stmt.setLong(1, aUser.getId());
            l_stmt.setString(2, aUser.getUsername());
            l_stmt.setString(3, aUser.getEmail());
            l_stmt.executeUpdate();
            
        } catch (SQLException anEx) {
            throw new RuntimeException("Failed to save user", anEx);
        }
    }
}
```

### Service Tracker (Alternative)

```java
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class MyActivator implements BundleActivator {
    
    private ServiceTracker<DataSourceService, DataSourceService> tracker;
    
    @Override
    public void start(BundleContext aContext) {
        tracker = new ServiceTracker<>(
            aContext, 
            DataSourceService.class, 
            null
        );
        tracker.open();
        
        DataSourceService l_service = tracker.getService();
        if (l_service != null) {
            System.out.println("Database available: " + l_service.isAvailable());
        }
    }
    
    @Override
    public void stop(BundleContext aContext) {
        tracker.close();
    }
}
```

## 🔍 Monitoring

### Pool Statistics

```java
@Reference
DataSourceService dataSourceService;

public void printPoolStats() {
    String l_stats = dataSourceService.getPoolStatistics();
    System.out.println(l_stats);
}

// Output:
// Pool Statistics:
//   Active Connections: 2
//   Idle Connections: 8
//   Total Connections: 10
//   Threads Awaiting: 0
```

### Health Check

```java
public boolean isDatabaseHealthy() {
    return dataSourceService != null && dataSourceService.isAvailable();
}
```

### Database Info

```java
public void printDatabaseInfo() {
    System.out.println("Database: " + dataSourceService.getDatabaseProductName());
    System.out.println("Version: " + dataSourceService.getDatabaseVersion());
    System.out.println("JDBC URL: " + dataSourceService.getJdbcUrl());
}

// Output:
// Database: H2
// Version: 2.2.224
// JDBC URL: jdbc:h2:./H2/IAM;IFEXISTS=FALSE
```

## 🗄️ Database Initialization

### Automatic Schema Creation

Der Service erstellt automatisch eine `DB_VERSION` Tabelle:

```sql
CREATE TABLE IF NOT EXISTS DB_VERSION (
    VERSION VARCHAR(50) PRIMARY KEY,
    APPLIED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

### Custom Schema Creation

Erweitern Sie die `createSchema()` Methode:

```java
private void createSchema() {
    System.out.println("Creating database schema...");
    
    try (Connection l_conn = dataSource.getConnection();
         Statement l_stmt = l_conn.createStatement()) {
        
        // Version table
        l_stmt.execute("CREATE TABLE IF NOT EXISTS DB_VERSION (...)");
        
        // IAM Tables (beispielhaft)
        l_stmt.execute("CREATE TABLE IF NOT EXISTS USERS (" +
            "ID BIGINT PRIMARY KEY AUTO_INCREMENT, " +
            "USERNAME VARCHAR(255) UNIQUE NOT NULL, " +
            "EMAIL VARCHAR(255) UNIQUE NOT NULL, " +
            "PASSWORD_HASH VARCHAR(255), " +
            "CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        
        l_stmt.execute("CREATE TABLE IF NOT EXISTS ROLES (" +
            "ID BIGINT PRIMARY KEY AUTO_INCREMENT, " +
            "NAME VARCHAR(100) UNIQUE NOT NULL, " +
            "DESCRIPTION VARCHAR(500))");
        
        l_stmt.execute("CREATE TABLE IF NOT EXISTS USER_ROLES (" +
            "USER_ID BIGINT, " +
            "ROLE_ID BIGINT, " +
            "PRIMARY KEY (USER_ID, ROLE_ID), " +
            "FOREIGN KEY (USER_ID) REFERENCES USERS(ID), " +
            "FOREIGN KEY (ROLE_ID) REFERENCES ROLES(ID))");
        
        System.out.println("Database schema created");
        
    } catch (SQLException anEx) {
        System.err.println("Failed to create schema: " + anEx.getMessage());
    }
}
```

## 🔒 Sicherheit

### Password Protection (Production)

```properties
# configuration/biz.car.database.cfg
jdbc.url=jdbc:h2:./H2/IAM;IFEXISTS=FALSE;CIPHER=AES
db.user=admin
db.password=ChangeMe123!
```

### File Encryption

```properties
jdbc.url=jdbc:h2:./H2/IAM;CIPHER=AES;FILE_ENCRYPTION_KEY=YourSecretKey
```

### SSL Connection (Server Mode)

```properties
jdbc.url=jdbc:h2:ssl://localhost:9092/IAM;SSL=TRUE
```

## 🐛 Troubleshooting

### Problem: Service not found

```
@Reference
DataSourceService dataSourceService; // null
```

**Diagnose:**
```bash
osgi> ss | grep database
osgi> services DataSourceService
```

**Lösung:**
- Bundle Status prüfen (sollte Active sein)
- SCR Component Status: `scr:list`
- Logs prüfen auf Activation-Fehler

### Problem: Connection Pool exhausted

```
Exception: Connection is not available, request timed out after 30000ms
```

**Lösung:**
```properties
# Erhöhen Sie pool.maxSize
pool.maxSize=20
```

### Problem: Database locked

```
Exception: Database may be already in use
```

**Lösung:**
```properties
# Verwenden Sie AUTO_SERVER für Multi-Process-Zugriff
jdbc.url=jdbc:h2:./H2/IAM;AUTO_SERVER=TRUE;IFEXISTS=FALSE
```

### Problem: Schema not created

**Diagnose:**
```properties
init.mode=none  # Schema creation disabled
```

**Lösung:**
```properties
init.mode=create
```

## 📊 Performance Tuning

### HikariCP Best Practices

```properties
# Optimal für typische Workloads:
pool.minSize=5
pool.maxSize=20
connection.timeout=30000
idle.timeout=600000
max.lifetime=1800000
```

### H2 Performance Settings

```properties
jdbc.url=jdbc:h2:./H2/IAM;\
    IFEXISTS=FALSE;\
    CACHE_SIZE=65536;\
    LOCK_TIMEOUT=10000;\
    DB_CLOSE_DELAY=-1
```

### Prepared Statement Caching

Bereits aktiviert in `H2DataSourceService`:
```java
l_config.addDataSourceProperty("cachePrepStmts", "true");
l_config.addDataSourceProperty("prepStmtCacheSize", "250");
l_config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
```

## ✅ Zusammenfassung

**Was Sie bekommen:**

✅ **Production-ready DataSource Service** mit HikariCP  
✅ **Konfigurierbar** via Configuration Admin  
✅ **Hot-Reconfiguration** (Modified lifecycle)  
✅ **Connection Pooling** für Performance  
✅ **Auto-Schema-Creation** für Development  
✅ **Monitoring** via Pool Statistics  
✅ **Saubere API-Separation** (API + Implementation)  
✅ **OSGi Declarative Services** Integration  

**Nächste Schritte:**

1. Bundles bauen und deployen
2. Service in IAM-Bundles injizieren
3. Database-Schema erweitern
4. User/Role Management implementieren

**Status: Production Ready** 🚀
