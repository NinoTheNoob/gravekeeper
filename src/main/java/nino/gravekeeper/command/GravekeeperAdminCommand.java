package nino.gravekeeper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public final class GravekeeperAdminCommand {

    private final JavaPlugin plugin;

    public GravekeeperAdminCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(Commands commands) {
        commands.register(
            Commands.literal("gravekeeper")
                .then(Commands.literal("reload")
                    .requires(source -> source.getSender().hasPermission("gravekeeper.admin"))
                    .executes(this::reload))
                .build(),
            "Reloads the Gravekeeper configuration."
        );
    }

    private int reload(CommandContext<CommandSourceStack> ctx) {
        plugin.reloadConfig();
        CommandSourceStack source = ctx.getSource();
        String message = plugin.getConfig().getString("messages.reload", "&aConfiguration reloaded successfully.");
        if (source.getSender() instanceof org.bukkit.entity.Player) {
            source.getSender().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        } else {
            source.getSender().sendMessage(Component.text("Gravekeeper config reloaded.", NamedTextColor.GREEN));
        }
        return Command.SINGLE_SUCCESS;
    }
}
