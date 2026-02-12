# CAR.OSGi

Ein leichtgewichtiger OSGi-Framework-Launcher mit integriertem Hot-Deployment-Mechanismus. Das Projekt stellt eine eigenständige Laufzeitumgebung bereit, die ein OSGi-Framework (standardmäßig Eclipse Equinox) initialisiert, konfiguriert und verwaltet. Bundles werden automatisch aus einer konfigurierbaren Install Area installiert, aktualisiert und bei Bedarf zur Laufzeit nachgeladen.

## Projektstruktur

```
biz.car.osgi
├── bundle/       Konstanten, Nachrichten, Konfigurationsschlüssel und Diagnose-Logger
├── deploy/       Deployment-Logik: Install Area, Bundle Storage und Hot Deployer
└── framework/    Fassade zum OSGi-Framework sowie Event-Listener
```

## Packages

### `biz.car.osgi`

Das Root-Package enthält die Einstiegspunkte der Anwendung.

| Klasse | Beschreibung |
|---|---|
| **Main** | Enthält die `main()`-Methode und startet den `Launcher` in einem eigenen Thread. Dient gleichzeitig als Stopp-Schnittstelle über `stop()`. |
| **Launcher** | Implementiert `Runnable` und orchestriert den gesamten Startvorgang: Laden der System- und Framework-Properties (via Typesafe Config / HOCON), Erstellen des Framework Data Area-Verzeichnisses, Registrierung des Shutdown Hooks, Aufbau der OSGi-Konfiguration, Initialisierung des Frameworks, Verarbeitung der Install Area und optionaler Start des Hot Deployments. Abschließend wird das Framework gestartet und auf dessen Beendigung gewartet. |
| **ShutdownHook** | Ein `Runnable`, das als JVM-Shutdown-Hook registriert wird. Er stoppt den Directory Watcher und fährt das OSGi-Framework sauber herunter, wenn die VM beendet wird. |

### `biz.car.osgi.bundle`

Dieses Package bündelt alle projektspezifischen Konstanten, Nachrichtentexte und die Diagnose-Infrastruktur. Die Klassen werden über externe Konfigurationsdateien (`BND.conf`, `MSG.properties`, `reference.conf`) initialisiert.

| Klasse | Beschreibung |
|---|---|
| **BND** | Bundle-Konstanten wie Thread-Namen (`OSGI_MAIN`, `SHUTDOWN_HOOK`, `BUNDLE_WATCHER`) und der Framework-Name. Stellt außerdem Hilfsmethoden bereit, die numerische OSGi-Event- und State-Codes in lesbare Strings übersetzen (`bundleEvent()`, `frameworkEvent()`, `serviceEvent()`, `state()`). |
| **MSG** | Nachrichtentexte für Log-Ausgaben (z. B. `FWK_INITIALIZED`, `FWK_STARTED`, `BUNDLE_EVENT`). Die Texte werden aus `MSG.properties` geladen und unterstützen Platzhalter im SLF4J-Stil (`{}`). |
| **VAL** | Schlüsselnamen für Konfigurationswerte wie `framework_data_area`, `osgi_install_area` oder `framework_hotdeploy_enabled`. Die Feldnamen werden per Static Field Initialization (`SFI`) automatisch mit ihren eigenen Bezeichnern als Werte befüllt. |
| **KEY** | Lädt das Framework-spezifische Property-Mapping (`Equinox.properties`), das die abstrakten Konfigurationsschlüssel auf die konkreten Schlüssel der jeweiligen OSGi-Implementierung abbildet. |
| **DIAG** | Stellt einen dedizierten SLF4J-Logger für Diagnose-Zwecke bereit (`DIAG.LOG`). Der Logger-Name wird über die Anwendungskonfiguration bestimmt (Schlüssel `diagnoseLogger`). |

### `biz.car.osgi.deploy`

Dieses Package implementiert die gesamte Deployment-Logik — vom Erkennen der JAR-Dateien in der Install Area über die Synchronisation mit dem Bundle Storage bis hin zum dateibasierten Hot Deployment.

| Klasse | Beschreibung |
|---|---|
| **InstallArea** | Repräsentiert die OSGi Install Area auf dem Dateisystem. Scannt rekursiv alle `.jar`-Dateien und synchronisiert sie mit dem Framework-Bundle-Storage über die `reconcile()`-Methode: neue JARs werden installiert, aktualisierte JARs führen zu einem Bundle-Update, und nicht mehr vorhandene JARs bewirken eine Deinstallation. Das Start-Level eines Bundles wird aus der Verzeichnisstruktur abgeleitet (z. B. Ordner `/04/` → Start-Level 4). Stellt außerdem die Singleton-Referenz auf den `Deployer` bereit. |
| **BundleStorage** | Bildet den aktuellen Zustand des OSGi-Bundle-Cache ab. Beim Erzeugen werden alle installierten Bundles geladen, deren Location auf die Install Area zeigt. Bietet Lookup per Location-String und eine `uninstallBundles()`-Methode, die verwaiste Bundles (ohne zugehörige JAR-Datei) entfernt. |
| **BundleLocation** | Funktionales Interface mit einer statischen Hilfsmethode `toURI()`, die Dateipfade in URI-Strings konvertiert. |
| **Deployer** | Implementiert `DirectoryListener` und überwacht die Install Area mittels eines `DirectoryWatcher` (File-System-Watch-Service). Bei Dateiänderungen werden die Events gesammelt und nach einer kurzen Verzögerung (Debouncing, 1 Sekunde) der vollständige Deployment-Zyklus ausgelöst: Reconcile → `FrameworkWiring.refreshBundles()` → Warten auf `PACKAGES_REFRESHED` → Starten der neuen/aktualisierten Bundles. |

### `biz.car.osgi.framework`

Dieses Package kapselt die Interaktion mit der OSGi-Framework-Implementierung hinter einer statischen Fassade und stellt die zugehörigen Event-Listener bereit.

| Klasse | Beschreibung |
|---|---|
| **XFramework** | Zentrale Fassade zum OSGi-Framework. Verwaltet den Framework-Lebenszyklus mit den Methoden `init()`, `start()`, `stop()`, `refreshAndWait()` und `startBundles()`. Die Methode `refreshAndWait()` implementiert einen synchronen Refresh-Zyklus mit `CountDownLatch`, der auf das `PACKAGES_REFRESHED`-Event wartet (mit konfigurierbarem Timeout). Fragment-Bundles werden beim Start automatisch übersprungen. |
| **XFrameworkFactory** | Erzeugt die Framework-Instanz über den Java `ServiceLoader`-Mechanismus (`ServiceLoader<FrameworkFactory>`). Damit ist die konkrete OSGi-Implementierung (z. B. Equinox, Felix) austauschbar, ohne den Code ändern zu müssen. |
| **XFrameworkListener** | Implementiert `FrameworkListener` und protokolliert alle Framework-Events (STARTED, ERROR, PACKAGES_REFRESHED usw.) über den Diagnose-Logger. |
| **XBundleListener** | Implementiert `BundleListener` und protokolliert Bundle-Lifecycle-Events (installed, started, stopped, updated usw.). Events des System-Bundles werden gefiltert. |
| **XServiceListener** | Implementiert `ServiceListener` und protokolliert Service-Events (REGISTERED, CHANGED, REMOVING usw.) über den Diagnose-Logger. |
| **FrameworkDiagnose** | Funktionales Interface mit einer statischen `accept()`-Methode, die eine umfassende Diagnose-Ausgabe des Frameworks erzeugt: alle Framework-Properties, das aktuelle Start-Level sowie für jedes installierte Bundle dessen ID, State, Version, Änderungsdatum, Start-Level und Location. |

## Konfiguration

Die Anwendung verwendet [Typesafe Config (HOCON)](https://github.com/lightbend/config) und wird über folgende Dateien konfiguriert:

| Datei | Beschreibung |
|---|---|
| `reference.conf` | Default-Werte der Anwendung (z. B. Name des Diagnose-Loggers) |
| `system.default.properties` | Standard-Systemproperties |
| `framework.default.properties` | Standard-Framework-Properties |
| `Equinox.properties` | Mapping der abstrakten Schlüssel auf Equinox-spezifische OSGi-Schlüssel |
| `BND.conf` | Bundle-Konstanten, Event-Mappings und Thread-Namen |
| `MSG.properties` | Nachrichtentexte für das Logging |

System- und Framework-Properties können durch gleichnamige Dateien ohne den Prefix `default` überschrieben werden (`system.properties`, `framework.properties`).

## Abhängigkeiten

- **OSGi Framework API** (`org.osgi.framework`) — z. B. Eclipse Equinox oder Apache Felix
- **Typesafe Config** — HOCON-basierte Konfiguration
- **SLF4J** — Logging-Fassade
- **CAR Base Library** (`biz.car.*`) — projektübergreifende Infrastruktur (Logger, Config-Utilities, IO-Watcher)
