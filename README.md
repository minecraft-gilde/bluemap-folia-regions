# BlueMap Folia Regions

Zeigt Folia Tick-Regionen als Marker-Overlay in **BlueMap** an.

<img width="809" height="502" alt="Screenshot" src="https://github.com/user-attachments/assets/3153a9f2-dbf4-4259-bbf9-d4fbbef9c754" />

## Features
- Marker-Set "Folia Tick-Regionen" pro BlueMap-Map
- Zeigt Region-Umrisse + Infos (Sektionen, Chunks, Entitaeten, Spieler)
- Standardmaessig ausgeblendet (togglebar)

## Voraussetzungen
- **Folia** (Version passend zum Build des Plugins)
- **BlueMap** (BlueMapAPI)
- **JDK 25** fuer den Build
- **Gradle 9.x** ueber den mitgelieferten Wrapper

## Installation
1. BlueMap installieren und starten (damit die Maps existieren)
2. Dieses Plugin in den `plugins/` Ordner legen
3. Server neu starten
4. In BlueMap: Marker -> "Folia Tick-Regionen" aktivieren

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
  height: 80
  line-color: "#9b46ffff"
  fill-color: "#d2aaff59"
  line-width: 2
```

Nach Änderungen kann die Config mit `/bmfr reload` neu geladen werden. Für `markers.label-format` stehen `{world}`, `{center_x}`, `{center_z}`, `{sections}`, `{chunks}`, `{entities}` und `{players}` zur Verfügung.

## Build
```bash
./gradlew build
```
