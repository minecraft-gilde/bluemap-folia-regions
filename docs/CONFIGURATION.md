# Konfiguration

Beim ersten Start erstellt das Plugin die Datei `plugins/BlueMap-Folia-Regions/config.yml`. Nach Änderungen kann sie mit `/bmfr reload` neu geladen werden.

Die [mitgelieferte Standardkonfiguration](../src/main/resources/config.yml) enthält sämtliche Optionen und das vollständige HTML-Layout der Markeransicht. Sie dient als Referenz, damit die umfangreiche Vorlage nicht an mehreren Stellen gepflegt werden muss.

## Allgemeine Einstellungen

| Einstellung | Bedeutung |
| --- | --- |
| `config-version` | Version des Konfigurationsformats |
| `update-interval-seconds` | Abstand zwischen zwei Markeraktualisierungen |
| `marker-set.id` | Interne ID des BlueMap-Marker-Sets |
| `marker-set.label` | In BlueMap angezeigter Name |
| `marker-set.default-hidden` | Overlay beim Öffnen der Karte zunächst ausblenden |
| `marker-set.toggleable` | Umschalten des Overlays in BlueMap erlauben |

## Visualisierung

`visualization.mode` bestimmt, welcher Wert die Regionsfarbe steuert:

- `static` verwendet immer `markers.line-color` und `markers.fill-color`.
- `utilization` bewertet die Regionsauslastung.
- `mspt` bewertet die durchschnittliche Tickzeit.
- `tps` bewertet die TPS.

Mit `visualization.report-window` wird das ausgewertete Zeitfenster festgelegt. Zulässig sind `5s`, `15s`, `1m`, `5m` und `15m`.

Die Grenzwerte unter `visualization.thresholds` teilen die Messwerte in `normal`, `warning`, `high` und `critical` ein. Die zugehörigen Linien- und Füllfarben werden unter `visualization.colors` konfiguriert. Solange Folia noch nicht genügend Tickdaten gesammelt hat, wird `unavailable` verwendet.

## Trends

`trends.enabled` aktiviert Kurzzeit-Trends. Die Werte unter `trends.sensitivity` bestimmen, welche Änderungen noch als stabil gelten.

Eine Region wird mit ihrer vorherigen erfolgreichen Erfassung verglichen. Grün kennzeichnet eine Verbesserung, Rot eine Verschlechterung und Grau einen stabilen Wert. Nach einer Änderung ihrer Sektionen oder nach Ablauf von `trends.reset-after-seconds` beginnt die Erfassung neu. Dadurch werden dynamisch geteilte oder zusammengeführte Folia-Regionen nicht miteinander verglichen.

Die Trendhistorie liegt ausschließlich im Arbeitsspeicher und wird bei einem Reload oder Serverneustart zurückgesetzt. Die Farbe eines Leistungswertes beschreibt seinen aktuellen Status, der Trend dagegen seine zeitliche Entwicklung. Ein kritischer Wert kann daher gleichzeitig einen grünen Verbesserungstrend besitzen.

## Belastungskontext

`load-context.enabled` aktiviert Hinweise zu hoher Entitätsdichte und ungewöhnlich großen Regionen. Die Stufen werden unter `load-context.thresholds` konfiguriert.

Der Hinweis erscheint nur, wenn mindestens ein Wert seine konfigurierte Schwelle erreicht. Gleichzeitig vorhandene Auffälligkeiten werden gemeinsam beschrieben, ohne sie automatisch als Ursache für die Regionsleistung zu bewerten.

Die Pfeile bei Entitäten und Spielern zeigen lediglich, ob sich ihre Anzahl erhöht, verringert oder stabil entwickelt hat. Sie verwenden deshalb bewusst keine roten oder grünen Bewertungsfarben.

## Marker und HTML-Detailansicht

`markers.label-format` definiert die kurze Markerbezeichnung. Mit `markers.detail-format` kann die komplette HTML-Detailansicht angepasst werden. Dynamische Werte in der HTML-Ansicht werden automatisch maskiert. Unbekannte Platzhalter bleiben unverändert.

### Region und Position

- `{region_id}`, `{world}`
- `{center_x}`, `{center_z}` als kompatible Kurzform der Chunkkoordinaten
- `{center_chunk_x}`, `{center_chunk_z}`
- `{center_block_x}`, `{center_block_z}`
- `{sections}`, `{chunks}`, `{area_blocks}`
- `{sections_formatted}`, `{chunks_formatted}`, `{area_blocks_formatted}`

### Entitäten, Spieler und Belastung

- `{entities}`, `{players}`
- `{entities_formatted}`, `{players_formatted}`
- `{entities_per_chunk}`, `{players_per_chunk}`
- `{entities_trend}`, `{players_trend}`
- `{entities_trend_status}`, `{players_trend_status}`
- `{entities_trend_color}`, `{players_trend_color}`
- `{load_context}`, `{load_context_display}`

### Leistung und Status

- `{report_window}`
- `{collected_ticks}`, `{collected_ticks_formatted}`
- `{tps}`, `{mspt}`, `{mspt_worst_5}`, `{mspt_worst_1}`, `{utilization}`
- `{tps_status}`, `{mspt_status}`, `{utilization_status}`
- `{tps_status_color}`, `{mspt_status_color}`, `{utilization_status_color}`
- `{diagnosis}` als kompakte Erklärung auffälliger Werte und ihrer Schwellen
- `{status}`, `{status_color}` für den schlechtesten Gesamtstatus
- `{visualization_status}`, `{visualization_color}`, `{visualization_mode}`

### Trends und Aktualisierung

- `{trend_available}`, `{trend_samples}`
- `{tps_trend}`, `{mspt_trend}`, `{utilization_trend}`
- `{tps_trend_status}`, `{mspt_trend_status}`, `{utilization_trend_status}`
- `{tps_trend_color}`, `{mspt_trend_color}`, `{utilization_trend_color}`
- `{tick_spike}`, `{spike_detail}`
- `{warning_duration}`, `{warning_duration_detail}`
- `{updated_at}`

`markers.timestamp-format` legt die Darstellung von `{updated_at}` fest. Höhe, Linienfarbe, Füllfarbe und Linienbreite können über die übrigen `markers`-Einstellungen angepasst werden.
