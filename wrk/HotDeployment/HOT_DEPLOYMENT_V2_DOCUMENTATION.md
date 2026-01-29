# Hot Deployment Implementation v2.0 - Production Grade

## Architektur-Übersicht

Diese Implementierung folgt dem **korrekten OSGi Hot Deployment Pattern** mit Framework Refresh Cycle.

## 🔄 Deployment Cycle

```
┌─────────────────────────────────────────────────────────────┐
│ 1. FILE SYSTEM EVENT DETECTION                              │
│    - WatchService detects JAR changes                       │
│    - CREATE / MODIFY / DELETE events                        │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. FILE SETTLING                                            │
│    - Wait 500ms for file operations to complete             │
│    - Prevents processing incomplete writes                  │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. RECONCILE BUNDLES (via Deployer pattern)                │
│    ├─ BundleStorage.uninstallBundles(installArea)          │
│    │  └─ Remove bundles whose JARs were deleted            │
│    │                                                         │
│    └─ InstallArea.reconcile(bundleStorage)                 │
│       ├─ Update bundles with newer JAR timestamp           │
│       └─ Install new bundles from new JARs                 │
│                                                             │
│    ⚠️  WICHTIG: Bundles werden NICHT gestartet!            │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. FRAMEWORK REFRESH                                        │
│    - FrameworkWiring.refreshBundles(null)                   │
│    - Resolves package dependencies                          │
│    - Updates package wirings                                │
│    - Clears stale wiring state                              │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. WAIT FOR PACKAGES_REFRESHED EVENT                        │
│    - FrameworkListener waits for event                      │
│    - CountDownLatch synchronization                         │
│    - Timeout: 30 seconds                                    │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. START BUNDLES                                            │
│    - Start all newly installed/updated bundles              │
│    - Skip fragment bundles                                  │
│    - Use START_ACTIVATION_POLICY flag                       │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 Warum dieser Ansatz?

### Problem: Direktes Start nach Install

```java
// ❌ FALSCH - Kann zu ClassNotFoundException führen
bundle = context.installBundle(location);
bundle.start(); // Package wiring noch nicht aktualisiert!
```

**Probleme:**
- Bundle A exportiert Package `com.example.api` v1.0
- Bundle B importiert `com.example.api` v1.0
- Bundle A wird aktualisiert auf v2.0
- Bundle B hat noch Wiring zu v1.0 Packages
- **Resultat**: ClassNotFoundException, NoClassDefFoundError

### Lösung: Framework Refresh Cycle

```java
// ✅ RICHTIG - Framework Refresh erst durchführen
context.installBundle(location);          // Install
frameworkWiring.refreshBundles(null);     // Refresh
// ... wait for PACKAGES_REFRESHED event
bundle.start();                           // Start (nach Refresh)
```

**Vorteile:**
- ✅ Alle Package-Abhängigkeiten werden neu aufgelöst
- ✅ Stale Wirings werden entfernt
- ✅ Bundles sehen konsistenten Package-Export-State
- ✅ Keine ClassNotFoundException

## 📋 Implementierungs-Details

### 1. Event Detection (WatchService)

```java
WatchService watchService = FileSystems.getDefault().newWatchService();
path.register(watchService, 
    ENTRY_CREATE, 
    ENTRY_MODIFY, 
    ENTRY_DELETE
);
```

**Batch Processing:**
- Nicht jedes einzelne Event wird verarbeitet
- Alle Events innerhalb eines Poll-Cycles → ein Deployment-Cycle
- Reduziert unnötige Framework-Refreshes

### 2. Reconcile Pattern (Wiederverwendung Ihrer Deployer-Logik)

```java
BundleStorage bundleStorage = new BundleStorage();
InstallArea installArea = new InstallArea();

// Uninstall removed bundles
bundleStorage.uninstallBundles(installArea);

// Install new, update existing (WITHOUT starting)
List<Bundle> newOrUpdatedBundles = installArea.reconcile(bundleStorage);
```

**Separation of Concerns:**
- `reconcile()` macht nur Install/Update
- Start-Logik ist separiert
- Deployer-Logik bleibt wiederverwendbar

### 3. Framework Refresh mit Event-Synchronisation

```java
final CountDownLatch refreshLatch = new CountDownLatch(1);

FrameworkListener refreshListener = event -> {
    if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
        refreshLatch.countDown();
    }
};

bundleContext.addFrameworkListener(refreshListener);
frameworkWiring.refreshBundles(null, refreshListener);

// Warten auf PACKAGES_REFRESHED
boolean refreshed = refreshLatch.await(30, TimeUnit.SECONDS);
```

**Thread-Synchronisation:**
- `CountDownLatch` für sicheres Warten
- Timeout verhindert endloses Warten
- Listener wird immer entfernt (try-finally)

### 4. Bundle Start nach Refresh

```java
for (Bundle bundle : newOrUpdatedBundles) {
    if (!isFragment(bundle)) {
        bundle.start(Bundle.START_ACTIVATION_POLICY);
    }
}
```

**Start-Policy:**
- `START_ACTIVATION_POLICY`: Lazy Activation
- Fragments werden übersprungen
- Fehler werden geloggt, aber nicht propagiert

## 🔬 OSGi Framework Wiring Konzept

### Was ist Package Wiring?

```
Bundle A (v1.0)                Bundle B
├─ Export: com.api;v=1.0  ←───┤ Import: com.api;v=[1.0,2.0)
└─ class ApiImpl              └─ uses ApiImpl
```

### Was passiert bei Update ohne Refresh?

```
VORHER:
Bundle A v1.0 → Export com.api v1.0
Bundle B      → Wired to com.api v1.0 ✓

UPDATE (ohne Refresh):
Bundle A v2.0 → Export com.api v2.0
Bundle B      → Wired to com.api v1.0 ❌ (STALE!)

Bundle B.start() → ClassNotFoundException
```

### Was macht Framework Refresh?

```
1. Stoppt alle affected Bundles
2. Entfernt stale Wirings
3. Re-resolved alle Package-Abhängigkeiten
4. Erstellt neue Wirings
5. Fired PACKAGES_REFRESHED Event

NACHHER:
Bundle A v2.0 → Export com.api v2.0
Bundle B      → Wired to com.api v2.0 ✓
Bundle B.start() → Success!
```

## 📊 Vergleich: Alte vs. Neue Implementierung

| Aspekt | V1 (direkt) | V2 (refresh cycle) |
|--------|-------------|-------------------|
| Install → Start | Sofort | Nach Refresh |
| Package Wiring | Potentiell stale | Garantiert konsistent |
| ClassNotFoundException | Möglich | Verhindert |
| Bundle Dependencies | Nicht garantiert | Aufgelöst |
| Production-Ready | Nein | Ja ✓ |
| OSGi-Konform | Nein | Ja ✓ |

## 🚀 Integration

### Datei-Platzierung

```
src/biz/car/osgi/
├─ Launcher.java (bereits vorhanden, unverändert)
└─ deploy/
   ├─ BundleDeploymentWatcher.java  ← NEUE DATEI (v2)
   ├─ Deployer.java                 (bereits vorhanden)
   ├─ BundleStorage.java            (bereits vorhanden)
   └─ InstallArea.java              (bereits vorhanden)
```

### Keine Änderungen an bestehenden Klassen nötig!

Die neue Implementierung **wiederverwendet** Ihre bestehende Deployer-Logik:
- ✅ `BundleStorage.uninstallBundles()`
- ✅ `InstallArea.reconcile()`
- ✅ Start-Level-Management
- ✅ Fragment-Detection

## 🧪 Testing

### Test-Szenario 1: Neues Bundle

```bash
# Framework läuft
cp my-bundle-1.0.0.jar bundles/10/

# Erwarteter Ablauf:
# [INFO] Hot deployment cycle started
# [INFO] Reconciled 1 bundle(s)
# [INFO] Refreshing framework packages
# [INFO] Framework packages refreshed successfully
# [INFO] Started bundle: my-bundle [42]
# [INFO] Bundle start summary: 1 started, 0 failed
# [INFO] Hot deployment cycle completed
```

### Test-Szenario 2: Bundle Update mit Dependencies

```bash
# Bundle A (v1.0) läuft, Bundle B verwendet Bundle A
cp bundle-a-2.0.0.jar bundles/10/bundle-a-1.0.0.jar

# Erwarteter Ablauf:
# [INFO] Hot deployment cycle started
# [INFO] Reconciled 1 bundle(s)
# [INFO] Refreshing framework packages  ← WICHTIG!
# [INFO] Framework packages refreshed successfully
# [INFO] Started bundle: bundle-a [10]
# [INFO] Bundle start summary: 1 started, 0 failed
# [INFO] Hot deployment cycle completed

# Bundle B hat jetzt korrekte Wiring zu bundle-a v2.0 ✓
```

### Test-Szenario 3: Mehrere gleichzeitige Änderungen

```bash
# Mehrere JARs gleichzeitig kopieren
cp bundle-a.jar bundles/10/ &
cp bundle-b.jar bundles/10/ &
cp bundle-c.jar bundles/20/ &
wait

# Erwarteter Ablauf:
# ALLE Änderungen werden in EINEM Cycle verarbeitet
# [INFO] Hot deployment cycle started
# [INFO] Reconciled 3 bundle(s)
# [INFO] Refreshing framework packages
# [INFO] Framework packages refreshed successfully
# [INFO] Started bundle: bundle-a [10]
# [INFO] Started bundle: bundle-b [11]
# [INFO] Started bundle: bundle-c [12]
# [INFO] Bundle start summary: 3 started, 0 failed
# [INFO] Hot deployment cycle completed
```

## ⚡ Performance-Überlegungen

### Framework Refresh ist "teuer"

**Warum?**
- Stoppt betroffene Bundles
- Re-resolved alle Packages
- Kann mehrere Sekunden dauern

**Optimierung:**
- Batch-Processing: Alle Events in einem Cycle
- Nur ein Refresh pro Cycle (nicht pro Bundle)
- File-Settling verhindert zu frühe Verarbeitung

### Batch-Window

```java
// Single event
Event: bundle-a.jar MODIFY
→ Wait 500ms
→ Process deployment cycle

// Multiple events within 500ms
Event: bundle-a.jar MODIFY
Event: bundle-b.jar CREATE  } Batched together
Event: bundle-c.jar MODIFY  }
→ Wait 500ms
→ Process deployment cycle (all 3 bundles)
```

## 🛡️ Fehlerbehandlung

### Refresh Timeout

```java
boolean refreshed = refreshLatch.await(30, TimeUnit.SECONDS);
if (!refreshed) {
    LOG.warn("Framework refresh timed out");
    // Bundles werden NICHT gestartet
    // System bleibt in konsistentem Zustand
}
```

### Bundle Start Fehler

```java
try {
    bundle.start();
} catch (Exception e) {
    LOG.error("Failed to start bundle", e);
    // Fehler wird geloggt
    // ABER: Andere Bundles werden weiter gestartet
    // Kein Abbruch des gesamten Cycles
}
```

### Watch Service Fehler

```java
try {
    // Main watch loop
} catch (Exception e) {
    LOG.error("Hot deployment watcher error", e);
} finally {
    cleanup(); // Immer ausgeführt
}
```

## 📈 Production Deployment

### Development Environment
```properties
# framework.properties
framework.hotdeploy.enabled = true
```

**Vorteile:**
- Schneller Development-Cycle
- Sofortiges Feedback
- Kein Framework-Neustart

### Production Environment
```properties
# framework.properties
framework.hotdeploy.enabled = false
```

**Vorteile:**
- Kontrollierte Deployments
- Kein spontanes Bundle-Reloading
- Vorhersagbares Verhalten

### Staging Environment
```properties
# framework.properties
framework.hotdeploy.enabled = true
```

**Verwendung:**
- Testing von Deployments
- QA-Umgebung
- Demo-Systeme

## 🎓 OSGi Best Practices (implementiert)

### ✅ 1. Framework Refresh nach Structural Changes
- Bundle install → Refresh
- Bundle update → Refresh
- Bundle uninstall → Refresh (optional)

### ✅ 2. Event-Driven Synchronisation
- `PACKAGES_REFRESHED` Event
- Keine busy-waiting loops
- Timeout protection

### ✅ 3. Separation of Concerns
- Detection ≠ Reconciliation
- Reconciliation ≠ Starting
- Each phase is testable

### ✅ 4. Graceful Error Handling
- Individual bundle errors don't break system
- Proper cleanup in finally blocks
- Meaningful error messages

### ✅ 5. Fragment Bundle Support
- Detected via `Fragment-Host` header
- Not started (invalid operation)
- Properly handled in refresh cycle

## 📚 Weiterführende OSGi-Konzepte

### FrameworkWiring API

```java
Bundle systemBundle = context.getBundle(0);
FrameworkWiring wiring = systemBundle.adapt(FrameworkWiring.class);

// Refresh specific bundles
wiring.refreshBundles(Collection<Bundle>);

// Refresh all bundles
wiring.refreshBundles(null);

// Resolve bundles (without restart)
wiring.resolveBundles(Collection<Bundle>);
```

### Framework Events

| Event Type | Bedeutung | Wann gefeuert |
|-----------|-----------|---------------|
| STARTED | Framework gestartet | Nach init |
| STOPPED | Framework gestoppt | Nach stop |
| PACKAGES_REFRESHED | Packages refreshed | Nach refreshBundles() |
| STARTLEVEL_CHANGED | Start level geändert | Nach setStartLevel() |
| ERROR | Framework Fehler | Bei kritischen Fehlern |

## 🔍 Debugging

### Log-Level Konfiguration

```xml
<!-- logback.xml -->
<logger name="biz.car.osgi.deploy" level="DEBUG"/>
```

**Debug Output:**
```
[DEBUG] Registered install area: /path/to/bundles
[DEBUG] Detected JAR file change: bundle-a.jar
[INFO]  Hot deployment cycle started
[DEBUG] Uninstalled 0 bundle(s)
[DEBUG] Updated 1 bundle(s)
[DEBUG] Installed 0 new bundle(s)
[INFO]  Reconciled 1 bundle(s)
[INFO]  Refreshing framework packages
[DEBUG] Waiting for PACKAGES_REFRESHED event
[INFO]  Framework packages refreshed successfully
[DEBUG] Starting bundle: bundle-a [10]
[INFO]  Started bundle: bundle-a [10]
[INFO]  Bundle start summary: 1 started, 0 failed
[INFO]  Hot deployment cycle completed
```

### Gogo Console Commands

```bash
# Bundle status
osgi> ss

# Bundle details
osgi> bundle 10

# Package wiring
osgi> packages 10

# Services
osgi> services
```

## ✨ Zusammenfassung

Diese Implementierung ist:

1. **✅ OSGi-Konform**: Folgt OSGi Core Specification
2. **✅ Production-Ready**: Robuste Fehlerbehandlung
3. **✅ Wiederverwendbar**: Nutzt bestehende Deployer-Logik
4. **✅ Event-Driven**: Korrekte Framework-Event-Behandlung
5. **✅ Performant**: Batch-Processing von Events
6. **✅ Testbar**: Klare Separation of Concerns
7. **✅ Dokumentiert**: Vollständig kommentiert

**Status: Ready for Production** 🚀
