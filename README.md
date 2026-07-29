# BlueMap Folia Regions

Zeigt Folia-Tick-Regionen als Marker-Overlay in **BlueMap** an.

> Der Branch `version-2` enthält die schrittweise Entwicklung von Version 2.

<img width="809" height="502" alt="Screenshot" src="https://github.com/user-attachments/assets/3153a9f2-dbf4-4259-bbf9-d4fbbef9c754" />

## Features

- Marker-Set „Folia Tick-Regionen“ pro BlueMap-Map
- Exakte Regionsumrisse einschließlich konkaver Flächen, Löcher und getrennter Teilflächen
- Regions-ID, Chunk- und Blockzentrum, Sektionen, Chunks und Regionsfläche
- Entitäten und Spieler einschließlich Dichte pro Chunk
- Konfigurierbare Markerbezeichnung und HTML-Detailansicht
- Zeitpunkt der letzten Datenerfassung
- Laufzeitstatus und manuelles Aktualisieren per Befehl
- Standardmäßig ausgeblendet und in BlueMap umschaltbar

## Voraussetzungen

- **Folia** (Version passend zum Build des Plugins)
- **BlueMap** (BlueMapAPI)
- **JDK 25** für den Build
- **Gradle 9.x** über den mitgelieferten Wrapper

## Installation

1. BlueMap installieren und starten, damit die Maps existieren
2. Dieses Plugin in den `plugins/`-Ordner legen
3. Server neu starten
4. In BlueMap „Marker“ → „Folia Tick-Regionen“ aktivieren

## Konfiguration

Beim ersten Start wird `plugins/BlueMap-Folia-Regions/config.yml` erstellt.

```yaml
update-interval-seconds: 5

marker-set:
  id: folia-regions
  label: Folia Tick-Regionen
  default-hidden: true
  toggleable: true

markers:
  label-format: "Region[{center_x},{center_z}]"
  detail-format: |-
    <b>Folia-Region {region_id}</b><br>
    Welt: {world}<br>
    Zentrum: Chunk {center_chunk_x}, {center_chunk_z} / Block {center_block_x}, {center_block_z}<br>
    Sektionen: {sections}<br>
    Chunks: {chunks}<br>
    Regionsfl&auml;che: {area_blocks} Bl&ouml;cke&sup2;<br>
    Entit&auml;ten: {entities} ({entities_per_chunk}/Chunk)<br>
    Spieler: {players} ({players_per_chunk}/Chunk)<br>
    Aktualisiert: {updated_at}
  timestamp-format: "yyyy-MM-dd HH:mm:ss z"
  height: 80
  line-color: "#9b46ffff"
  fill-color: "#d2aaff59"
  line-width: 2
```

Nach Änderungen kann die Config mit `/bmfr reload` neu geladen werden.

Für `markers.label-format` und `markers.detail-format` stehen folgende Platzhalter zur Verfügung:

- `{region_id}`, `{world}`
- `{center_x}`, `{center_z}` als kompatible Kurzform der Chunkkoordinaten
- `{center_chunk_x}`, `{center_chunk_z}`
- `{center_block_x}`, `{center_block_z}`
- `{sections}`, `{chunks}`, `{area_blocks}`
- `{entities}`, `{players}`
- `{entities_per_chunk}`, `{players_per_chunk}`
- `{updated_at}`

Unbekannte Platzhalter bleiben unverändert. Dynamische Werte in der HTML-Detailansicht werden automatisch maskiert.

## Befehle

- `/bmfr reload` – Konfiguration neu laden
- `/bmfr refresh` – Marker aller BlueMap-Maps sofort neu berechnen
- `/bmfr status` – BlueMap-Verbindung, Maps, Regions- und Markeranzahl sowie Aktualisierungsdauer anzeigen

Die zugehörigen Berechtigungen sind `bluemapfoliaregions.reload`, `bluemapfoliaregions.refresh` und `bluemapfoliaregions.status`. Sie sind standardmäßig Operatoren vorbehalten.

## Build

```bash
./gradlew build
```
