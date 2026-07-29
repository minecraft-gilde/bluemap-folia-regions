# BlueMap Folia Regions

Zeigt Folia-Tick-Regionen als übersichtliches Marker-Overlay in **BlueMap** an.

> Der Branch `version-2` enthält die Entwicklung von Version 2.

<img width="809" height="502" alt="Folia-Regionen in BlueMap" src="https://github.com/user-attachments/assets/3153a9f2-dbf4-4259-bbf9-d4fbbef9c754" />

## Features

- Exakte Regionsumrisse einschließlich konkaver Flächen, Löcher und getrennter Teilflächen
- Regionsdaten wie Zentrum, Sektionen, Chunks, Fläche, Entitäten und Spieler
- TPS, MSPT, Tickspitzen und Auslastung für verschiedene Zeitfenster
- Konfigurierbare Heatmap nach Auslastung, MSPT oder TPS
- Kurzzeit-Trends, Warnzustandsdauer und kompakte Ursachenanalyse
- Belastungskontext für hohe Entitätsdichte und ungewöhnlich große Regionen
- Kompakte, für Desktop und Mobilgeräte geeignete Detailansicht
- Laufzeitstatus, sofortige Aktualisierung und Reload per Befehl

## Voraussetzungen

- **Folia** in einer zum Plugin-Build passenden Version
- **BlueMap** mit verfügbarer BlueMapAPI

Für einen lokalen Build werden zusätzlich **JDK 25** und der mitgelieferte Gradle-Wrapper benötigt.

## Installation

1. BlueMap installieren und einmal starten, damit die Maps angelegt werden.
2. Die Plugin-JAR in den Ordner `plugins/` legen.
3. Den Server neu starten.
4. In BlueMap unter „Marker“ das Overlay „Folia Tick-Regionen“ aktivieren.

## Konfiguration

Beim ersten Start wird `plugins/BlueMap-Folia-Regions/config.yml` erstellt. Die wichtigsten Einstellungen:

```yaml
update-interval-seconds: 5

visualization:
  mode: utilization       # static, utilization, mspt oder tps
  report-window: 15s      # 5s, 15s, 1m, 5m oder 15m

trends:
  enabled: true

load-context:
  enabled: true

marker-set:
  default-hidden: true
```

Die vollständige Beschreibung aller Einstellungen, Schwellenwerte, Farben und Formatplatzhalter befindet sich in der [Konfigurationsreferenz](docs/CONFIGURATION.md).

## Befehle

| Befehl | Beschreibung |
| --- | --- |
| `/bmfr reload` | Konfiguration neu laden |
| `/bmfr refresh` | Marker aller BlueMap-Maps sofort neu berechnen |
| `/bmfr status` | Verbindungs-, Regions-, Marker- und Laufzeitstatus anzeigen |

Die Berechtigungen heißen entsprechend `bluemapfoliaregions.reload`, `bluemapfoliaregions.refresh` und `bluemapfoliaregions.status`. Sie sind standardmäßig Operatoren vorbehalten.

## Dokumentation

- [Konfiguration und Formatplatzhalter](docs/CONFIGURATION.md)
- [Release mit Git-Tags erstellen](docs/RELEASE.md)

## Build

```bash
./gradlew build
```
