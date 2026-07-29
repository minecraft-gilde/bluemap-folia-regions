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
- TPS, durchschnittliche MSPT und schlechteste 5 % beziehungsweise 1 % der Tickzeiten
- Regionsauslastung über auswählbare Zeitfenster von 5 Sekunden bis 15 Minuten
- Heatmap nach Auslastung, MSPT oder TPS mit konfigurierbaren Statusfarben
- Kurzzeit-Trends für TPS, Tickzeit und Auslastung sowie Erkennung kritischer Tickspitzen
- Dauer eines ununterbrochenen Warnzustands
- Eigene Statusfarbe pro Leistungswert und kompakte Ursachenanalyse mit Schwellenwert
- Neutrale Aktivitätstrends für Entitäten und Spieler
- Optionaler Belastungskontext für hohe Entitätsdichte und ungewöhnlich große Regionen
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

trends:
  enabled: true
  # Trends nach längeren Pausen oder einer geänderten Regionsform zurücksetzen
  reset-after-seconds: 30
  sensitivity:
    # Kleinere Änderungen gelten als stabil
    tps: 0.10
    mspt: 1.0
    utilization-percentage-points: 2.0
    # Absolute Anzahländerungen unterhalb dieser Werte gelten als stabil
    entities: 5
    players: 1

load-context:
  enabled: true
  thresholds:
    entities-per-chunk:
      warning: 8.0
      high: 16.0
      critical: 32.0
    region-chunks:
      warning: 1500
      high: 3000
      critical: 5000

visualization:
  # static, utilization, mspt oder tps
  mode: utilization
  # 5s, 15s, 1m, 5m oder 15m
  report-window: 15s
  thresholds:
    utilization:
      warning: 0.60
      high: 0.75
      critical: 0.90
    mspt:
      warning: 25.0
      high: 40.0
      critical: 50.0
    tps:
      warning: 19.5
      high: 18.0
      critical: 15.0
  colors:
    normal:
      line-color: "#37b24dff"
      fill-color: "#51cf6666"
    warning:
      line-color: "#f08c00ff"
      fill-color: "#ffd43b73"
    high:
      line-color: "#e8590cff"
      fill-color: "#ff922b80"
    critical:
      line-color: "#c92a2aff"
      fill-color: "#fa525299"
    unavailable:
      line-color: "#9b46ffff"
      fill-color: "#d2aaff59"

marker-set:
  id: folia-regions
  label: Folia Tick-Regionen
  default-hidden: true
  toggleable: true

markers:
  label-format: "Region[{center_x},{center_z}]"
  detail-format: |-
    <div style="width:100%;max-width:100%;box-sizing:border-box;overflow:hidden;font-size:14px;line-height:1.25">
      <div style="display:flex;align-items:center;justify-content:space-between;gap:12px">
        <strong style="font-size:18px">Folia-Region {region_id}</strong>
        <span style="color:{status_color};border:1px solid {status_color};border-radius:999px;padding:2px 8px;font-size:12px;font-weight:700">{status}</span>
      </div>
      <div style="margin-top:3px;opacity:.65;font-size:12px">{world} &middot; Chunk {center_chunk_x}, {center_chunk_z} &middot; Block {center_block_x}, {center_block_z}</div>
      <div style="margin-top:11px;padding-top:9px;border-top:1px solid rgba(255,255,255,.14)">
        <div style="margin-bottom:6px;opacity:.55;font-size:10px;font-weight:700;letter-spacing:.08em">REGION</div>
        <div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px">
          <div><strong>{sections_formatted}</strong><div style="opacity:.6;font-size:11px">Sektionen</div></div>
          <div><strong>{chunks_formatted}</strong><div style="opacity:.6;font-size:11px">Chunks</div></div>
          <div><strong>{area_blocks_formatted}</strong><div style="opacity:.6;font-size:11px">Bl&ouml;cke&sup2;</div></div>
        </div>
      </div>
      <div style="margin-top:11px;padding-top:9px;border-top:1px solid rgba(255,255,255,.14)">
      <div style="margin-bottom:6px;opacity:.55;font-size:10px;font-weight:700;letter-spacing:.08em">AKTIVIT&Auml;T</div>
      <div style="display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px">
        <div><strong>{entities_formatted} <span style="color:{entities_trend_color};font-size:11px">{entities_trend}</span></strong><div style="opacity:.65;font-size:11px">Entit&auml;ten</div><div style="margin-top:1px;opacity:.45;font-size:10px">{entities_per_chunk} / Chunk</div></div>
        <div><strong>{players_formatted} <span style="color:{players_trend_color};font-size:11px">{players_trend}</span></strong><div style="opacity:.65;font-size:11px">Spieler</div><div style="margin-top:1px;opacity:.45;font-size:10px">{players_per_chunk} / Chunk</div></div>
      </div>
      <div style="display:{load_context_display};margin-top:6px;opacity:.65;font-size:11px;line-height:1.35;overflow-wrap:anywhere">{load_context}</div>
      </div>
      <div style="margin-top:11px;padding-top:9px;border-top:1px solid rgba(255,255,255,.14)">
      <div style="margin-bottom:6px;opacity:.55;font-size:10px;font-weight:700;letter-spacing:.08em">LEISTUNG &middot; {report_window}</div>
      <div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px">
        <div><strong style="color:{tps_status_color}">{tps}</strong><div style="opacity:.6;font-size:11px">TPS</div></div>
        <div><strong style="color:{mspt_status_color}">{mspt} ms</strong><div style="opacity:.6;font-size:11px">&Oslash; Tickzeit</div></div>
        <div><strong style="color:{utilization_status_color}">{utilization} %</strong><div style="opacity:.6;font-size:11px">Auslastung</div></div>
      </div>
      <div style="margin-top:7px;opacity:.65;font-size:11px;line-height:1.35">Spitzen: 5 % {mspt_worst_5} ms &middot; 1 % {mspt_worst_1} ms &middot; {collected_ticks_formatted} Ticks</div>
      <div style="margin-top:5px;opacity:.65;font-size:11px;line-height:1.35">Trend: TPS <span style="color:{tps_trend_color};font-weight:700">{tps_trend}</span> &middot; Tickzeit <span style="color:{mspt_trend_color};font-weight:700">{mspt_trend}</span> &middot; Auslastung <span style="color:{utilization_trend_color};font-weight:700">{utilization_trend}</span>{spike_detail}{warning_duration_detail}</div>
      <div style="margin-top:5px;opacity:.75;font-size:11px;line-height:1.35;overflow-wrap:anywhere">{diagnosis}</div>
    </div>
      <div style="margin-top:10px;text-align:right;opacity:.45;font-size:10px">Stand: {updated_at}</div>
    </div>
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
- `{sections_formatted}`, `{chunks_formatted}`, `{area_blocks_formatted}`
- `{entities}`, `{players}`
- `{entities_formatted}`, `{players_formatted}`
- `{entities_per_chunk}`, `{players_per_chunk}`
- `{entities_trend}`, `{players_trend}` als neutrale Aktivitätsrichtungen
- `{entities_trend_status}`, `{players_trend_status}`
- `{entities_trend_color}`, `{players_trend_color}`
- `{load_context}`, `{load_context_display}`
- `{report_window}`, `{collected_ticks}`, `{collected_ticks_formatted}`, `{tps}`, `{mspt}`
- `{mspt_worst_5}`, `{mspt_worst_1}`, `{utilization}`
- `{tps_status}`, `{mspt_status}`, `{utilization_status}`
- `{tps_status_color}`, `{mspt_status_color}`, `{utilization_status_color}`
- `{diagnosis}` als kompakte Erklärung auffälliger Leistungswerte und ihrer Schwellen
- `{status}` für den schlechtesten Gesamtstatus aller Leistungsmetriken
- `{status_color}` als zugehörige CSS-Farbe
- `{visualization_status}` für den Status der aktuell ausgewählten Heatmap-Metrik
- `{visualization_color}` als zugehörige CSS-Farbe
- `{visualization_mode}`
- `{trend_available}`, `{trend_samples}`
- `{tps_trend}`, `{mspt_trend}`, `{utilization_trend}` als kompakte Richtungspfeile
- `{tps_trend_status}`, `{mspt_trend_status}`, `{utilization_trend_status}`
- `{tps_trend_color}`, `{mspt_trend_color}`, `{utilization_trend_color}`
- `{tick_spike}`, `{spike_detail}`
- `{warning_duration}`, `{warning_duration_detail}`
- `{updated_at}`

Unbekannte Platzhalter bleiben unverändert. Dynamische Werte in der HTML-Detailansicht werden automatisch maskiert.

Im Modus `static` werden weiterhin `markers.line-color` und `markers.fill-color` verwendet. Die übrigen Modi wählen die Farbe anhand der zugehörigen Schwellenwerte. Solange Folia noch nicht genügend Tickdaten gesammelt hat, wird die Farbe `unavailable` verwendet.

Trends vergleichen eine Region mit ihrer vorherigen erfolgreichen Erfassung. Grün kennzeichnet eine Verbesserung, Rot eine Verschlechterung und Grau einen stabilen Wert. Nach einer Änderung der zugehörigen Sektionen oder nach Ablauf von `trends.reset-after-seconds` beginnt die Erfassung neu. Dadurch werden dynamisch geteilte oder zusammengeführte Folia-Regionen nicht miteinander verglichen. Die Trendhistorie wird nur im Arbeitsspeicher gehalten und bei einem Reload oder Serverneustart zurückgesetzt.

Die Farbe eines Leistungswertes beschreibt seinen aktuellen Status; der daneben angezeigte Trend beschreibt dagegen seine zeitliche Entwicklung. So kann ein kritischer Wert gleichzeitig einen grünen Verbesserungstrend besitzen. Die Diagnose nennt alle auffälligen Metriken, beginnend mit der schwerwiegendsten, und zeigt den jeweils erreichten Schwellenwert.

Die Pfeile bei Entitäten und Spielern zeigen ausschließlich, ob sich ihre Anzahl erhöht, verringert oder stabil entwickelt hat. Sie verwenden deshalb bewusst keine roten oder grünen Bewertungsfarben. Der Belastungskontext erscheint nur, wenn mindestens ein Wert die unter `load-context.thresholds` konfigurierte Schwelle erreicht. Er beschreibt gleichzeitig vorhandene Auffälligkeiten, ohne daraus automatisch eine Ursache für die Regionsleistung abzuleiten.

## Befehle

- `/bmfr reload` – Konfiguration neu laden
- `/bmfr refresh` – Marker aller BlueMap-Maps sofort neu berechnen
- `/bmfr status` – BlueMap-Verbindung, Maps, Regions- und Markeranzahl sowie Aktualisierungsdauer anzeigen

Die zugehörigen Berechtigungen sind `bluemapfoliaregions.reload`, `bluemapfoliaregions.refresh` und `bluemapfoliaregions.status`. Sie sind standardmäßig Operatoren vorbehalten.

## Build

```bash
./gradlew build
```
