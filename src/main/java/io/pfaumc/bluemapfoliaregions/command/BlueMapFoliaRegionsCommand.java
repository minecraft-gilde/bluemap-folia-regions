package io.pfaumc.bluemapfoliaregions.command;

import io.pfaumc.bluemapfoliaregions.BlueMapFoliaRegionsPlugin;
import io.pfaumc.bluemapfoliaregions.BlueMapFoliaRegionsPlugin.MapUpdateStatus;
import io.pfaumc.bluemapfoliaregions.BlueMapFoliaRegionsPlugin.RuntimeStatus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class BlueMapFoliaRegionsCommand extends Command {
    private static final String BASE_PERMISSION = "bluemapfoliaregions.command";
    private static final String RELOAD_PERMISSION = "bluemapfoliaregions.reload";
    private static final String REFRESH_PERMISSION = "bluemapfoliaregions.refresh";
    private static final String STATUS_PERMISSION = "bluemapfoliaregions.status";
    private static final DateTimeFormatter STATUS_TIME_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withZone(ZoneId.systemDefault());

    private final BlueMapFoliaRegionsPlugin plugin;

    public BlueMapFoliaRegionsCommand(BlueMapFoliaRegionsPlugin plugin) {
        super("bluemapfoliaregions");
        this.plugin = plugin;
        setAliases(List.of("bmfr"));
        setDescription("Manage BlueMap Folia Regions");
        setUsage("/bluemapfoliaregions <reload|refresh|status>");
        setPermission(BASE_PERMISSION);
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(BASE_PERMISSION)) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(RELOAD_PERMISSION)) {
                sender.sendMessage("You do not have permission to reload this plugin.");
                return true;
            }

            boolean blueMapEnabled = this.plugin.reloadPluginConfiguration();
            sender.sendMessage("BlueMap Folia Regions configuration reloaded.");
            if (!blueMapEnabled) {
                sender.sendMessage("BlueMap is not enabled right now, so the new settings will apply when it comes online.");
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("refresh")) {
            if (!sender.hasPermission(REFRESH_PERMISSION)) {
                sender.sendMessage("You do not have permission to refresh the markers.");
                return true;
            }

            RuntimeStatus status = this.plugin.getRuntimeStatus();
            if (!status.blueMapEnabled()) {
                sender.sendMessage("BlueMap is not enabled right now.");
                return true;
            }

            int mapCount = this.plugin.refreshRegionMarkers();
            sender.sendMessage("Scheduled a Folia region marker refresh for " + mapCount + " BlueMap map(s).");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            if (!sender.hasPermission(STATUS_PERMISSION)) {
                sender.sendMessage("You do not have permission to view the plugin status.");
                return true;
            }

            sendStatus(sender);
            return true;
        }

        sender.sendMessage("Usage: /" + commandLabel + " <reload|refresh|status>");
        return true;
    }

    private void sendStatus(CommandSender sender) {
        RuntimeStatus status = this.plugin.getRuntimeStatus();
        sender.sendMessage("BlueMap Folia Regions status:");
        sender.sendMessage("- BlueMap: " + (status.blueMapEnabled() ? "enabled" : "disabled"));
        sender.sendMessage("- Scheduled maps: " + status.scheduledMapCount());
        sender.sendMessage("- Update interval: " + status.updateIntervalTicks() / 20.0D + " seconds");
        if (status.maps().isEmpty()) {
            sender.sendMessage("- No map update has completed yet.");
            return;
        }

        for (MapUpdateStatus map : status.maps()) {
            sender.sendMessage(String.format(
                    Locale.ROOT,
                    "- %s: %d regions, %d markers, %.2f ms, %s",
                    map.mapKey(),
                    map.regionCount(),
                    map.markerCount(),
                    map.updateDurationMillis(),
                    STATUS_TIME_FORMAT.format(map.lastUpdate())
            ));
            if (!map.successful()) {
                sender.sendMessage("  Last update failed: " + map.lastError());
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return Stream.of(
                            permittedCompletion(sender, RELOAD_PERMISSION, "reload"),
                            permittedCompletion(sender, REFRESH_PERMISSION, "refresh"),
                            permittedCompletion(sender, STATUS_PERMISSION, "status")
                    )
                    .filter((completion) -> completion != null && completion.startsWith(partial))
                    .toList();
        }

        return List.of();
    }

    private static String permittedCompletion(CommandSender sender, String permission, String completion) {
        return sender.hasPermission(permission) ? completion : null;
    }
}
