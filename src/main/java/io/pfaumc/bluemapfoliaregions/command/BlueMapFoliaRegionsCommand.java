package io.pfaumc.bluemapfoliaregions.command;

import io.pfaumc.bluemapfoliaregions.BlueMapFoliaRegionsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

public class BlueMapFoliaRegionsCommand extends Command {
    private static final String BASE_PERMISSION = "bluemapfoliaregions.command";
    private static final String RELOAD_PERMISSION = "bluemapfoliaregions.reload";

    private final BlueMapFoliaRegionsPlugin plugin;

    public BlueMapFoliaRegionsCommand(BlueMapFoliaRegionsPlugin plugin) {
        super("bluemapfoliaregions");
        this.plugin = plugin;
        setAliases(List.of("bmfr"));
        setDescription("Manage BlueMap Folia Regions");
        setUsage("/bluemapfoliaregions reload");
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

        sender.sendMessage("Usage: /" + commandLabel + " reload");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(RELOAD_PERMISSION)) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            if ("reload".startsWith(partial)) {
                return List.of("reload");
            }
        }

        return List.of();
    }
}
