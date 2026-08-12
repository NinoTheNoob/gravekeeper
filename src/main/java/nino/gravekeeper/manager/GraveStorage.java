package nino.gravekeeper.manager;

import nino.gravekeeper.model.GraveData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GraveStorage {

    private final JavaPlugin plugin;
    private final File gravesFolder;

    public GraveStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gravesFolder = new File(plugin.getDataFolder(), "graves");
        if (!gravesFolder.exists()) {
            gravesFolder.mkdirs();
        }
    }

    public File fileFor(UUID graveId) {
        return new File(gravesFolder, graveId.toString() + ".yml");
    }

    public void save(GraveData grave) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("graveId", grave.graveId().toString());
        config.set("ownerId", grave.ownerId().toString());
        config.set("ownerName", grave.ownerName());
        config.set("worldName", grave.worldName());
        config.set("x", grave.x());
        config.set("y", grave.y());
        config.set("z", grave.z());
        config.set("items", grave.items());
        config.set("experienceLevel", grave.experienceLevel());
        config.set("createdAtMillis", grave.createdAtMillis());
        config.set("expiresAtMillis", grave.expiresAtMillis());
        try {
            config.save(fileFor(grave.graveId()));
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to save grave " + grave.graveId() + ": " + exception.getMessage());
        }
    }

    public void delete(UUID graveId) {
        File file = fileFor(graveId);
        if (file.exists()) {
            file.delete();
        }
    }

    public List<GraveData> loadAll() {
        List<GraveData> result = new ArrayList<>();
        File[] files = gravesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return result;
        }
        for (File file : files) {
            GraveData grave = load(file);
            if (grave != null) {
                result.add(grave);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private GraveData load(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            UUID graveId = UUID.fromString(config.getString("graveId"));
            UUID ownerId = UUID.fromString(config.getString("ownerId"));
            String ownerName = config.getString("ownerName");
            String worldName = config.getString("worldName");
            double x = config.getDouble("x");
            double y = config.getDouble("y");
            double z = config.getDouble("z");
            List<ItemStack> items = (List<ItemStack>) config.getList("items");
            int experienceLevel = config.getInt("experienceLevel");
            long createdAtMillis = config.getLong("createdAtMillis");
            long expiresAtMillis = config.getLong("expiresAtMillis");
            return new GraveData(graveId, ownerId, ownerName, worldName, x, y, z,
                    items != null ? items : new ArrayList<>(), experienceLevel,
                    createdAtMillis, expiresAtMillis);
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to load grave file " + file.getName() + ": " + exception.getMessage());
            return null;
        }
    }
}
