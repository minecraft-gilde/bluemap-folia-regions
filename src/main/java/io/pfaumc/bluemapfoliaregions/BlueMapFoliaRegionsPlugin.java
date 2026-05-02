package io.pfaumc.bluemapfoliaregions;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import io.pfaumc.bluemapfoliaregions.command.BlueMapFoliaRegionsCommand;
import io.pfaumc.bluemapfoliaregions.config.PluginConfiguration;
import io.pfaumc.bluemapfoliaregions.marker.RegionMarkerFactory;
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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BlueMapFoliaRegionsPlugin extends JavaPlugin {
    private static final long INITIAL_DELAY_TICKS = 20L;

    private final ConcurrentMap<String, ScheduledTask> tasks = new ConcurrentHashMap<>();
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
                            updateRegionMarkers(map);
                        }
                    },
                    INITIAL_DELAY_TICKS,
                    this.configuration.updateIntervalTicks()
            );

            ScheduledTask previous = this.tasks.put(map.getId(), task);
            if (previous != null) {
                previous.cancel();
            }
        }
    }

    private void onBlueMapDisable(BlueMapAPI api) {
        cancelAllTasks();
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

    private void updateRegionMarkers(BlueMapMap map) {
        PluginConfiguration activeConfiguration = this.configuration;
        MarkerSet markerSet = MarkerSet.builder()
                .label(activeConfiguration.markerSetLabel())
                .defaultHidden(activeConfiguration.defaultHidden())
                .toggleable(activeConfiguration.toggleable())
                .build();

        String id = map.getWorld().getId();
        Optional<World> worldOptional = resolveWorld(id);
        if (worldOptional.isEmpty()) {
            getLogger().warning("World not found for BlueMap world id: " + id);
            map.getMarkerSets().remove(activeConfiguration.markerSetId());
            return;
        }

        World world = worldOptional.get();
        ThreadedRegionizer<TickRegionData, TickRegionSectionData> regioniser =
                ((CraftWorld) world).getHandle().regioniser;

        Map<String, ShapeMarker> markers = this.markerFactory.createMarkers(regioniser, activeConfiguration);
        markerSet.getMarkers().putAll(markers);
        map.getMarkerSets().put(activeConfiguration.markerSetId(), markerSet);
    }

    private Optional<World> resolveWorld(String id) {
        int hashIndex = id.indexOf('#');
        if (hashIndex != -1) {
            String worldName = id.substring(0, hashIndex);
            World byName = Bukkit.getWorld(worldName);
            if (byName != null) {
                return Optional.of(byName);
            }
        }

        try {
            UUID uuid = UUID.fromString(id);
            World byUuid = Bukkit.getWorld(uuid);
            if (byUuid != null) {
                return Optional.of(byUuid);
            }
        } catch (IllegalArgumentException ignored) {
            // The BlueMap world id is not a UUID.
        }

        return Optional.ofNullable(Bukkit.getWorld(id));
    }

    private void registerPermissions() {
        addPermission("bluemapfoliaregions.command");
        addPermission("bluemapfoliaregions.reload");
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
}
