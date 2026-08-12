package nino.gravekeeper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nino.gravekeeper.manager.GraveManager;
import nino.gravekeeper.model.GraveData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class GraveCommand {

    private final Plugin plugin;
    private final GraveManager graveManager;

    public GraveCommand(Plugin plugin, GraveManager graveManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
    }

    public void register(Commands commands) {
        commands.register(
            Commands.literal("grave")
                .executes(this::list)
                .then(Commands.literal("list").executes(this::list))
                .then(Commands.literal("tp")
                    .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .executes(this::teleport)))
                .build(),
            "Lists and teleports to your active graves."
        );
    }

    private int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        List<GraveData> graves = graveManager.gravesOf(player.getUniqueId());
        if (graves.isEmpty()) {
            String message = plugin.getConfig().getString("messages.no-graves", "&7You currently have no open graves.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return Command.SINGLE_SUCCESS;
        }

        player.sendMessage(Component.text("Your graves:", NamedTextColor.GOLD));
        for (int i = 0; i < graves.size(); i++) {
            GraveData grave = graves.get(i);
            long remainingSeconds = Math.max(0, (grave.expiresAtMillis() - System.currentTimeMillis()) / 1000);
            player.sendMessage(Component.text((i + 1) + ") ", NamedTextColor.YELLOW)
                .append(Component.text(grave.worldName() + " " + (int) grave.x() + " " + (int) grave.y() + " " + (int) grave.z(), NamedTextColor.WHITE))
                .append(Component.text(" - expires in " + remainingSeconds + "s", NamedTextColor.GRAY)));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int teleport(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        int index = IntegerArgumentType.getInteger(ctx, "index");
        List<GraveData> graves = graveManager.gravesOf(player.getUniqueId());
        if (index < 1 || index > graves.size()) {
            String message = plugin.getConfig().getString("messages.invalid-index", "&cInvalid grave index.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return Command.SINGLE_SUCCESS;
        }

        GraveData grave = graves.get(index - 1);
        World world = Bukkit.getWorld(grave.worldName());
        if (world == null) {
            return Command.SINGLE_SUCCESS;
        }

        Location location = new Location(world, grave.x(), grave.y(), grave.z());
        player.getScheduler().run(plugin, task -> {
            player.teleportAsync(location);
            String message = plugin.getConfig().getString("messages.teleported", "&aYou have been teleported to your grave.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }, null);
        return Command.SINGLE_SUCCESS;
    }
}
