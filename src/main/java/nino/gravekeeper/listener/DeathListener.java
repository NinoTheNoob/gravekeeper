package nino.gravekeeper.listener;

import nino.gravekeeper.manager.GraveManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class DeathListener implements Listener {

    private final GraveManager graveManager;

    public DeathListener(GraveManager graveManager) {
        this.graveManager = graveManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                items.add(item.clone());
            }
        }

        event.getDrops().clear();
        event.setDroppedExp(0);

        graveManager.createGrave(player.getUniqueId(), player.getName(),
                player.getLocation(), items, player.getLevel());
    }
}
