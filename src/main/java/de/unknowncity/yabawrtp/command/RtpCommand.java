package de.unknowncity.yabawrtp.command;

import de.unknowncity.astralib.paper.api.command.PaperCommand;
import de.unknowncity.yabawrtp.YABAWRTPPlugin;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.location.Location2D;
import org.incendo.cloud.context.CommandContext;
import org.spongepowered.configurate.NodePath;

import java.util.logging.Level;

import static org.incendo.cloud.bukkit.parser.PlayerParser.playerParser;
import static org.incendo.cloud.bukkit.parser.WorldParser.worldParser;
import static org.incendo.cloud.bukkit.parser.location.Location2DParser.location2DParser;
import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;

public class RtpCommand extends PaperCommand<YABAWRTPPlugin> {
    public RtpCommand(YABAWRTPPlugin plugin) {
        super(plugin);
    }

    @Override
    public void apply(CommandManager<CommandSender> commandManager) {
        commandManager.command(commandManager.commandBuilder("rtp")
                .permission("yabawrtp.command.rtp")
                .required("player", playerParser())
                .required("world", worldParser())
                .optional("min-radius", integerParser())
                .optional("max-radius", integerParser())
                .optional("origin", location2DParser())
                .handler(this::handleRTP)
        );
    }

    private void handleRTP(@NonNull CommandContext<CommandSender> context) {
        var player = context.<Player>get("player");
        var world = context.<World>get("world");
        var settings = plugin.configuration().worldSettings(world.getName());
        var minRadius = context.getOrDefault("min-radius", settings.radius().min());
        var maxRadius = context.getOrDefault("max-radius", settings.radius().max());
        var origin = context.getOrDefault("origin", Location2D.from(
                world,
                settings.origin().x(),
                settings.origin().z())
        );


        plugin.messenger().sendMessage(player, NodePath.path("command", "rtp", "searching"));

        if (context.contains("min-radius")) {
            resolveSaveLocation(player, world, minRadius, maxRadius, origin);
        } else {
            var locationOpt = plugin.abstractDelegateLocationFactoryBean().getCachedSafeLocation(world);
            if (locationOpt.isEmpty()) {
                resolveSaveLocation(player, world, minRadius, maxRadius, origin);
                return;
            }

            var saveLocation = locationOpt.get();
            player.teleportAsync(saveLocation);
            plugin.messenger().sendMessage(player, NodePath.path("command", "rtp", "success"));
        }
    }

    private void resolveSaveLocation(Player player, World world, Integer minRadius, Integer maxRadius, Location2D origin) {
        var location = plugin.abstractDelegateLocationFactoryBean().findSaveLocation(world.getName(), minRadius, maxRadius, origin.getBlockX(), origin.getBlockZ());

        if (location.isEmpty()) {
            plugin.getLogger().log(Level.WARNING, "Error while trying to find a safe location for " + player.getName());
            plugin.messenger().sendMessage(player, NodePath.path("command", "rtp", "error"));
            return;
        }

        var saveLocation = location.get();
        player.teleportAsync(saveLocation);
        plugin.messenger().sendMessage(player, NodePath.path("command", "rtp", "success"));
    }
}
