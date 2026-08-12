package nino.gravekeeper;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import nino.gravekeeper.command.GraveCommand;
import nino.gravekeeper.command.GravekeeperAdminCommand;
import nino.gravekeeper.listener.ChunkRehydrateListener;
import nino.gravekeeper.listener.DeathListener;
import nino.gravekeeper.listener.GraveAccessListener;
import nino.gravekeeper.manager.GraveManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Gravekeeper extends JavaPlugin {

    private GraveManager graveManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        graveManager = new GraveManager(this);
        graveManager.loadAll();

        getServer().getPluginManager().registerEvents(new DeathListener(graveManager), this);
        getServer().getPluginManager().registerEvents(new GraveAccessListener(this, graveManager), this);
        getServer().getPluginManager().registerEvents(new ChunkRehydrateListener(graveManager), this);

        GraveCommand graveCommand = new GraveCommand(this, graveManager);
        GravekeeperAdminCommand adminCommand = new GravekeeperAdminCommand(this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            graveCommand.register(event.registrar());
            adminCommand.register(event.registrar());
        });

        getLogger().info("Gravekeeper has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Gravekeeper has been disabled.");
    }

    public GraveManager getGraveManager() {
        return graveManager;
    }
}
