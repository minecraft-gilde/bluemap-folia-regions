package io.pfaumc.bluemapfoliaregions;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import io.pfaumc.bluemapfoliaregions.command.BlueMapFoliaRegionsCommand;
import io.pfaumc.bluemapfoliaregions.config.PluginConfiguration;
import io.pfaumc.bluemapfoliaregions.marker.RegionMarkerFactory;
import io.pfaumc.bluemapfoliaregions.marker.RegionMarkerFactory.MarkerBuildResult;
import io.pfaumc.bluemapfoliaregions.performance.ReportWindow;
import io.pfaumc.bluemapfoliaregions.performance.VisualizationMode;
import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.threadedregions.TickRegions;
import io.papermc.paper.threadedregions.TickRegions.TickRegionData;
import io.papermc.paper.threadedregions.TickRegions.TickRegionSectionData;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.logging.Level;

public class BlueMapFoliaRegionsPlugin extends JavaPlugin {
    private static final long INITIAL_DELAY_TICKS = 20L;

    private final ConcurrentMap<String, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MapUpdateStatus> mapStatuses = new ConcurrentHashMap<>();
    private final RegionMarkerFactory markerFactory = new RegionMarkerFactory(1 << TickRegions.getRegionChunkShift());
    private volatile PluginConfiguration configuration;
    private volatile BlueMapAPI currentApi;
    private BlueMapFoliaRegionsCommand command;
    private volatile boolean shuttingDown = false;

    @Override
    public void onEnable() {
        this.shuttingDown = false;
        saveDefaultConfig();
        this.configuration = PluginConfiguration.from(this);
        registerPermissions();
        registerCommand();

        BlueMapAPI.onEnable(this::onBlueMapEnable);
        BlueMapAPI.onDisable(this::onBlueMapDisable);
    }

    @Override
    public void onDisable() {
        this.shuttingDown = true;
        cancelAllTasks();
        this.mapStatuses.clear();
        BlueMapAPI.getInstance().ifPresent(this::removeAllMarkerSets);
        unregisterCommand();
        this.currentApi = null;
    }

    private void onBlueMapEnable(BlueMapAPI api) {
        if (this.shuttingDown) {
            return;
        }

        this.currentApi = api;
        cancelAllTasks();
        for (BlueMapMap map : api.getMaps()) {
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    this,
                    (t) -> {
                        if (!this.shuttingDown && isEnabled()) {
                            updateRegionMarkersSafely(map);
                        }
                    },
                    INITIAL_DELAY_TICKS,
                    this.configuration.updateIntervalTicks()
            );

            ScheduledTask previous = this.tasks.put(taskKey(map), task);
            if (previous != null) {
                previous.cancel();
            }
        }
    }

    private void onBlueMapDisable(BlueMapAPI api) {
        cancelAllTasks();
        this.mapStatuses.clear();
        removeAllMarkerSets(api);
        if (this.currentApi == api) {
            this.currentApi = null;
        }
    }

    public boolean reloadPluginConfiguration() {
        PluginConfiguration previousConfiguration = this.configuration;
        reloadConfig();
        this.configuration = PluginConfiguration.from(this);

        BlueMapAPI api = this.currentApi;
        if (api == null) {
            return false;
        }

        cancelAllTasks();
        this.mapStatuses.clear();
        removeAllMarkerSets(api, previousConfiguration.markerSetId());
        onBlueMapEnable(api);
        return true;
    }

    private void cancelAllTasks() {
        for (ScheduledTask task : this.tasks.values()) {
            task.cancel();
        }
        this.tasks.clear();
    }

    private void removeAllMarkerSets(BlueMapAPI api) {
        for (BlueMapMap map : api.getMaps()) {
            removeMarkerSet(map, this.configuration.markerSetId());
        }
    }

    private void removeAllMarkerSets(BlueMapAPI api, String markerSetId) {
        for (BlueMapMap map : api.getMaps()) {
            removeMarkerSet(map, markerSetId);
        }
    }

    private void removeMarkerSet(BlueMapMap map, String markerSetId) {
        try {
            Bukkit.getGlobalRegionScheduler().run(this, (t) -> map.getMarkerSets().remove(markerSetId));
        } catch (IllegalPluginAccessException ignored) {
            map.getMarkerSets().remove(markerSetId);
        }
    }

    private void updateRegionMarkersSafely(BlueMapMap map) {
        long startedAt = System.nanoTime();
        Instant updateTime = Instant.now();
        String mapKey = taskKey(map);
        try {
            MarkerBuildResult result = updateRegionMarkers(map, updateTime);
            this.mapStatuses.put(mapKey, new MapUpdateStatus(
                    mapKey,
                    result.snapshots().size(),
                    result.markers().size(),
                    updateTime,
                    elapsedMillis(startedAt),
                    null
            ));
        } catch (RuntimeException exception) {
            this.mapStatuses.compute(mapKey, (ignored, previous) -> new MapUpdateStatus(
                    mapKey,
                    previous == null ? 0 : previous.regionCount(),
                    previous == null ? 0 : previous.markerCount(),
                    updateTime,
                    elapsedMillis(startedAt),
                    exception.getClass().getSimpleName() + ": " + String.valueOf(exception.getMessage())
            ));
            getLogger().log(Level.WARNING, "Failed to update Folia region markers for BlueMap map: " + mapKey, exception);
        }
    }

    private MarkerBuildResult updateRegionMarkers(BlueMapMap map, Instant capturedAt) {
        PluginConfiguration activeConfiguration = this.configuration;
        MarkerSet markerSet = MarkerSet.builder()
                .label(activeConfiguration.markerSetLabel())
                .defaultHidden(activeConfiguration.defaultHidden())
                .toggleable(activeConfiguration.toggleable())
                .build();

        BlueMapWorld blueMapWorld = map.getWorld();
        String id = blueMapWorld.getId();
        Optional<World> worldOptional = resolveWorld(blueMapWorld);
        if (worldOptional.isEmpty()) {
            getLogger().warning("World not found for BlueMap world id: " + id);
            map.getMarkerSets().remove(activeConfiguration.markerSetId());
            throw new IllegalStateException("Bukkit world not found for BlueMap world id " + id);
        }

        World world = worldOptional.get();
        ThreadedRegionizer<TickRegionData, TickRegionSectionData> regioniser =
                ((CraftWorld) world).getHandle().regioniser;

        MarkerBuildResult result = this.markerFactory.createMarkers(
                regioniser,
                activeConfiguration,
                world.getName(),
                capturedAt
        );
        markerSet.getMarkers().putAll(result.markers());
        map.getMarkerSets().put(activeConfiguration.markerSetId(), markerSet);
        return result;
    }

    public RuntimeStatus getRuntimeStatus() {
        List<MapUpdateStatus> statuses = new ArrayList<>(this.mapStatuses.values());
        statuses.sort(Comparator.comparing(MapUpdateStatus::mapKey));
        return new RuntimeStatus(
                this.currentApi != null,
                this.tasks.size(),
                this.configuration.updateIntervalTicks(),
                this.configuration.visualization().mode(),
                this.configuration.visualization().reportWindow(),
                List.copyOf(statuses)
        );
    }

    public int refreshRegionMarkers() {
        BlueMapAPI api = this.currentApi;
        if (api == null || this.shuttingDown || !isEnabled()) {
            return 0;
        }

        int scheduled = 0;
        for (BlueMapMap map : api.getMaps()) {
            try {
                Bukkit.getGlobalRegionScheduler().run(this, (task) -> updateRegionMarkersSafely(map));
                scheduled++;
            } catch (IllegalPluginAccessException exception) {
                getLogger().log(Level.WARNING, "Could not schedule manual refresh for " + taskKey(map), exception);
            }
        }
        return scheduled;
    }

    private static double elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000.0D;
    }

    private String taskKey(BlueMapMap map) {
        return map.getWorld().getId() + ":" + map.getId();
    }

    private Optional<World> resolveWorld(BlueMapWorld blueMapWorld) {
        Optional<Path> saveFolder = getSaveFolder(blueMapWorld);
        if (saveFolder.isPresent()) {
            Optional<World> byPath = findByWorldPath(Bukkit.getWorlds(), World::getWorldPath, saveFolder.get());
            if (byPath.isPresent()) {
                return byPath;
            }
        }

        return resolveWorldByLegacyId(blueMapWorld.getId());
    }

    @SuppressWarnings("deprecation")
    private Optional<Path> getSaveFolder(BlueMapWorld blueMapWorld) {
        try {
            return Optional.of(blueMapWorld.getSaveFolder());
        } catch (UnsupportedOperationException ignored) {
            return Optional.empty();
        }
    }

    static <T> Optional<T> findByWorldPath(
            Collection<T> worlds,
            Function<T, Path> pathProvider,
            Path blueMapSaveFolder
    ) {
        Path expectedPath = blueMapSaveFolder.toAbsolutePath().normalize();
        return worlds.stream()
                .filter((world) -> pathProvider.apply(world).toAbsolutePath().normalize().equals(expectedPath))
                .findFirst();
    }

    private Optional<World> resolveWorldByLegacyId(String id) {
        int hashIndex = id.indexOf('#');
        if (hashIndex != -1) {
            String worldName = id.substring(0, hashIndex);
            String dimensionId = id.substring(hashIndex + 1);
            Optional<World> byNameOrUuid = resolveWorldByNameOrUuid(worldName)
                    .filter((world) -> world.getKey().toString().equals(dimensionId));
            if (byNameOrUuid.isPresent()) {
                return byNameOrUuid;
            }
        }

        return resolveWorldByNameOrUuid(id);
    }

    private Optional<World> resolveWorldByNameOrUuid(String id) {
        World byName = Bukkit.getWorld(id);
        return byName != null ? Optional.of(byName) : resolveWorldByUuid(id);
    }

    private Optional<World> resolveWorldByUuid(String id) {
        try {
            return Optional.ofNullable(Bukkit.getWorld(UUID.fromString(id)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private void registerPermissions() {
        addPermission("bluemapfoliaregions.command");
        addPermission("bluemapfoliaregions.reload");
        addPermission("bluemapfoliaregions.refresh");
        addPermission("bluemapfoliaregions.status");
    }

    private void addPermission(String name) {
        if (getServer().getPluginManager().getPermission(name) == null) {
            getServer().getPluginManager().addPermission(new Permission(name, PermissionDefault.OP));
        }
    }

    private void registerCommand() {
        CommandMap commandMap = getServer().getCommandMap();
        this.command = new BlueMapFoliaRegionsCommand(this);
        commandMap.register(getName().toLowerCase(), this.command);
    }

    private void unregisterCommand() {
        if (this.command != null) {
            this.command.unregister(getServer().getCommandMap());
            this.command = null;
        }
    }

    public record RuntimeStatus(
            boolean blueMapEnabled,
            int scheduledMapCount,
            long updateIntervalTicks,
            VisualizationMode visualizationMode,
            ReportWindow reportWindow,
            List<MapUpdateStatus> maps
    ) {}

    public record MapUpdateStatus(
            String mapKey,
            int regionCount,
            int markerCount,
            Instant lastUpdate,
            double updateDurationMillis,
            String lastError
    ) {
        public boolean successful() {
            return this.lastError == null;
        }
    }
}
