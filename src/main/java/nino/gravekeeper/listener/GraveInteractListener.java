package nino.gravekeeper.listener;

import nino.gravekeeper.manager.GraveManager;
import nino.gravekeeper.model.GraveData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GraveInteractListener implements Listener {

    private final JavaPlugin plugin;
    private final GraveManager graveManager;
    private final Map<UUID, UUID> openInventories = new HashMap<>();

    public GraveInteractListener(JavaPlugin plugin, GraveManager graveManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!graveManager.isGraveMarker(event.getRightClicked())) {
            return;
        }
        UUID graveId = graveManager.markerGraveId(event.getRightClicked());
        if (graveId == null) {
            return;
        }
        GraveData grave = graveManager.get(graveId);
        if (grave == null) {
            return;
        }

        Player player = event.getPlayer();
        boolean isOwner = grave.ownerId().equals(player.getUniqueId());
        boolean adminBypass = plugin.getConfig().getBoolean("allow-admin-access", true)
                && player.hasPermission("gravekeeper.admin");

        if (!isOwner && !adminBypass) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cThis is not your grave.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            event.setCancelled(true);
            return;
        }

        int size = nextInventorySize(grave.items().size());
        Inventory inventory = plugin.getServer().createInventory(null, size,
                "Grave - " + grave.ownerName());
        for (int i = 0; i < grave.items().size() && i < size; i++) {
            ItemStack item = grave.items().get(i);
            if (item != null) {
                inventory.setItem(i, item);
            }
        }

        openInventories.put(player.getUniqueId(), graveId);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        UUID graveId = openInventories.remove(player.getUniqueId());
        if (graveId == null) {
            return;
        }
        GraveData grave = graveManager.get(graveId);
        if (grave == null) {
            return;
        }

        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack item : event.getInventory().getContents()) {
            remaining.add(item);
        }

        boolean levelGranted = remaining.stream().allMatch(item -> item == null || item.getType().isAir());
        if (levelGranted && grave.experienceLevel() > 0) {
            player.giveExpLevels(grave.experienceLevel());
        }

        grave.items().clear();
        grave.items().addAll(remaining);
        graveManager.closeGraveIfEmpty(grave);
    }

    private int nextInventorySize(int itemCount) {
        int size = ((itemCount + 8) / 9) * 9;
        return Math.max(27, Math.min(54, size));
    }
}
