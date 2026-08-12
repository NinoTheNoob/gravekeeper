package nino.gravekeeper.listener;

import nino.gravekeeper.manager.GraveManager;
import nino.gravekeeper.model.GraveData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class GraveAccessListener implements Listener {

    private final JavaPlugin plugin;
    private final GraveManager graveManager;

    public GraveAccessListener(JavaPlugin plugin, GraveManager graveManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !graveManager.isGraveChest(block)) {
            return;
        }
        if (!canAccess(event.getPlayer(), block)) {
            event.setCancelled(true);
            String message = plugin.getConfig().getString("messages.no-permission", "&cThis is not your grave.");
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!graveManager.isGraveChest(block)) {
            return;
        }
        if (!canAccess(event.getPlayer(), block)) {
            event.setCancelled(true);
            String message = plugin.getConfig().getString("messages.no-permission", "&cThis is not your grave.");
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return;
        }
        UUID graveId = graveManager.chestGraveId(block);
        if (graveId == null) {
            return;
        }
        GraveData grave = graveManager.get(graveId);
        if (grave == null) {
            return;
        }
        graveManager.removeLabelOnly(grave);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest)) {
            return;
        }
        Block block = chest.getBlock();
        if (!graveManager.isGraveChest(block)) {
            return;
        }
        UUID graveId = graveManager.chestGraveId(block);
        if (graveId == null) {
            return;
        }
        GraveData grave = graveManager.get(graveId);
        if (grave == null) {
            return;
        }
        if (isEmpty(chest) && event.getPlayer() instanceof Player player) {
            if (grave.experienceLevel() > 0) {
                player.giveExpLevels(grave.experienceLevel());
            }
            graveManager.removeGrave(grave);
        }
    }

    private boolean isEmpty(Chest chest) {
        for (org.bukkit.inventory.ItemStack item : chest.getBlockInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private boolean canAccess(Player player, Block block) {
        UUID graveId = graveManager.chestGraveId(block);
        if (graveId == null) {
            return true;
        }
        GraveData grave = graveManager.get(graveId);
        if (grave == null) {
            return true;
        }
        boolean isOwner = grave.ownerId().equals(player.getUniqueId());
        boolean adminBypass = plugin.getConfig().getBoolean("allow-admin-access", true)
                && player.hasPermission("gravekeeper.admin");
        boolean protectFromOthers = plugin.getConfig().getBoolean("protect-from-others", true);
        return isOwner || adminBypass || !protectFromOthers;
    }
}
