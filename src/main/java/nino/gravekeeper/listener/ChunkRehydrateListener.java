package nino.gravekeeper.listener;

import nino.gravekeeper.manager.GraveManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public final class ChunkRehydrateListener implements Listener {

    private final GraveManager graveManager;

    public ChunkRehydrateListener(GraveManager graveManager) {
        this.graveManager = graveManager;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        graveManager.rehydrateChunk(event.getChunk());
    }
}
