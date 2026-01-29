# Hot Deployment Implementation für CAR.OSGi

## Übersicht

Die Hot Deployment Implementierung ermöglicht das automatische Erkennen und Verarbeiten von Bundle-Änderungen zur Laufzeit ohne Framework-Neustart.

## Komponenten

### 1. BundleDeploymentWatcher
Hauptklasse für das Hot Deployment:
- **Funktion**: Überwacht das `bundles/` Verzeichnis und alle Unterverzeichnisse
- **Technologie**: Java WatchService API (NIO.2)
- **Thread**: Läuft als Daemon-Thread im Hintergrund

### 2. Launcher (erweitert)
- Integriert den `BundleDeploymentWatcher`
- Startet/Stoppt den Watcher basierend auf Konfiguration
- Graceful Shutdown beim Framework-Stop

## Features

### Automatische Bundle-Operationen

| Datei-Event | OSGi-Operation | Beschreibung |
|------------|----------------|--------------|
| **CREATE** | `installBundle()` | Neues JAR → Bundle installieren und starten |
| **MODIFY** | `update()` | Geändertes JAR → Bundle aktualisieren (nur wenn neuer) |
| **DELETE** | `uninstall()` | Gelöschtes JAR → Bundle deinstallieren |

### Start-Level-Management
- **Automatische Erkennung**: Verzeichnisstruktur `/01/`, `/02/`, ... wird als Start-Level interpretiert
- **Beispiel**:
  ```
  bundles/
  ├── 01/           → Start Level 1
  │   └── core.jar
  ├── 10/           → Start Level 10
  │   └── services.jar
  └── 20/           → Start Level 20
      └── rest-api.jar
  ```

### Fragment-Bundle-Unterstützung
- Erkennt Fragment-Bundles automatisch (über `Fragment-Host` Header)
- Fragment-Bundles werden **nicht** gestartet (nur installiert)

### File-Settling
- **Wartezeit**: 500ms nach Datei-Event
- **Stabilität-Check**: Prüft ob Datei-Größe stabil ist (max. 5 Versuche)
- **Verhindert**: Verarbeitung von unvollständig kopierten Dateien

### Rekursive Verzeichnis-Überwachung
- Überwacht automatisch alle Unterverzeichnisse
- Neu erstellte Unterverzeichnisse werden dynamisch registriert

## Konfiguration

### framework.properties

```properties
# Enable/disable hot deployment (default: true)
framework.hotdeploy.enabled = true
```

**Optionen**:
- `true`: Hot Deployment aktiv (Standard)
- `false`: Hot Deployment deaktiviert, nur initiales Deployment

## Verwendung

### Development Workflow

1. **Framework starten**:
   ```bash
   java -jar lib/car.osgi-2.0.0.jar
   ```

2. **Bundle entwickeln**:
   - Änderungen am Bundle-Code vornehmen
   - Maven build: `mvn clean package`

3. **Hot Deploy**:
   - JAR in `bundles/XX/` Verzeichnis kopieren
   - **Automatisch**: Bundle wird installiert und gestartet
   - Logs zeigen: `"Hot deployed bundle: <name> [<id>]"`

4. **Bundle aktualisieren**:
   - Neues JAR über altes kopieren
   - **Automatisch**: Bundle wird aktualisiert
   - Logs zeigen: `"Updated bundle: <name> [<id>]"`

5. **Bundle entfernen**:
   - JAR aus Verzeichnis löschen
   - **Automatisch**: Bundle wird deinstalliert
   - Logs zeigen: `"Uninstalled bundle: <name> [<id>]"`

## Integration in bestehenden Code

### 1. BundleDeploymentWatcher.java
```
Ziel: src/biz/car/osgi/deploy/BundleDeploymentWatcher.java
```

### 2. Launcher.java (Update)
```
Ersetzen: src/biz/car/osgi/Launcher.java
```
**Neue Methoden**:
- `startHotDeployment(Config)`: Startet Watcher
- `stopHotDeployment()`: Stoppt Watcher gracefully

### 3. framework.properties (Update)
```
Erweitern: configuration/framework.properties
```
**Neue Property**:
- `framework.hotdeploy.enabled`

## Logging

### Info-Level Meldungen
```
Hot deployment watcher started
Registered install area for hot deployment: <path>
Hot deployed bundle: <symbolic-name> [<id>]
Updated bundle: <symbolic-name> [<id>]
Uninstalled bundle: <symbolic-name> [<id>]
Hot deployment watcher stopped
```

### Error-Level Meldungen
```
Failed to install bundle from: <location>
Failed to update bundle from: <location>
Failed to uninstall bundle from: <location>
Hot deployment watcher error: <details>
```

### Debug-Level Meldungen
```
Could not set start level for bundle: <symbolic-name>
Registered new directory for watching: <dir-name>
```

## Technische Details

### Thread-Sicherheit
- Watcher läuft als **Daemon-Thread**
- Verhindert nicht JVM-Shutdown
- `volatile boolean running` für sichere Thread-Kommunikation

### Performance
- **Poll-Intervall**: 1 Sekunde
- **Minimale CPU-Last** im Idle-Zustand
- **Event-Batching**: Alle Events eines Watch-Keys werden zusammen verarbeitet

### Fehlerbehandlung
- **Bundle-Operationen**: Fehler werden geloggt, aber System läuft weiter
- **Watch-Service-Fehler**: Werden geloggt, Watcher wird gestoppt
- **IO-Fehler**: Werden abgefangen und geloggt

## Vorteile

1. ✅ **Kein Framework-Neustart** bei Bundle-Änderungen
2. ✅ **Schneller Development-Zyklus** (Sekunden statt Minuten)
3. ✅ **Automatische Synchronisation** zwischen Dateisystem und OSGi
4. ✅ **Unterstützt komplexe Verzeichnisstrukturen**
5. ✅ **Production-Ready** (kann deaktiviert werden)
6. ✅ **Robust** gegen Fehler in einzelnen Bundles

## Testing

### Test-Szenario 1: Neues Bundle
```bash
# 1. Framework starten
java -jar lib/car.osgi-2.0.0.jar

# 2. Neues Bundle kopieren
cp test-bundle-1.0.0.jar bundles/10/

# 3. Erwartung:
# LOG: "Hot deployed bundle: test-bundle [42]"
# Bundle ist installiert und läuft
```

### Test-Szenario 2: Bundle Update
```bash
# 1. Bundle bereits installiert
# 2. Neues JAR drüberkopieren
cp test-bundle-1.0.1.jar bundles/10/test-bundle-1.0.0.jar

# 3. Erwartung:
# LOG: "Updated bundle: test-bundle [42]"
# Bundle ist aktualisiert
```

### Test-Szenario 3: Bundle Entfernung
```bash
# 1. Bundle löschen
rm bundles/10/test-bundle-1.0.0.jar

# 2. Erwartung:
# LOG: "Uninstalled bundle: test-bundle [42]"
# Bundle ist deinstalliert
```

## Best Practices

### Development
1. **Separate Verzeichnisse** für verschiedene Start-Levels
2. **Aussagekräftige Bundle-Namen** für besseres Logging
3. **Symbolic Names** in MANIFEST.MF verwenden

### Production
1. **Hot Deployment deaktivieren** in Production (`framework.hotdeploy.enabled = false`)
2. **Controlled Deployments** über CI/CD Pipeline
3. **Monitoring** der Logs für Failed Deployments

### Troubleshooting
1. **Logs prüfen**: INFO und ERROR Level aktivieren
2. **Bundle-Status**: Gogo Console verwenden (`ss`, `start`, `stop`)
3. **Datei-Permissions**: Sicherstellen dass JVM Lese-/Schreibrechte hat

## Erweiterungsmöglichkeiten

### Mögliche zukünftige Features
1. **Configurable Polling-Intervall**
2. **Blacklist/Whitelist für Verzeichnisse**
3. **Pre/Post-Deploy Hooks**
4. **Notification System** (JMX, REST API)
5. **Rollback-Mechanismus** bei fehlgeschlagenem Update
6. **Dependency-Order** beim gleichzeitigen Deploy mehrerer Bundles

## Zusammenfassung

Das Hot Deployment Feature:
- ✅ **Implementiert** und getestet
- ✅ **Production-Ready** mit Disable-Option
- ✅ **Robust** und fehlerresilient
- ✅ **Performant** mit minimaler CPU-Last
- ✅ **Einfach** zu verwenden (Zero-Config)

**Status**: Ready for Integration 🚀
