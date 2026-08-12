package nino.gravekeeper.manager;

import net.kyori.adventure.text.Component;
import nino.gravekeeper.model.GraveData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GraveManager {

    private final JavaPlugin plugin;
    private final GraveStorage storage;
    private final NamespacedKey graveIdKey;
    private final NamespacedKey ownerIdKey;
    private final Map<UUID, GraveData> graves = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> gravesByOwner = new ConcurrentHashMap<>();

    public GraveManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storage = new GraveStorage(plugin);
        this.graveIdKey = new NamespacedKey(plugin, "grave-id");
        this.ownerIdKey = new NamespacedKey(plugin, "owner-id");
    }

    public NamespacedKey graveIdKey() {
        return graveIdKey;
    }

    public NamespacedKey ownerIdKey() {
        return ownerIdKey;
    }

    public void loadAll() {
        for (GraveData grave : storage.loadAll()) {
            register(grave);
        }
    }

    private void register(GraveData grave) {
        graves.put(grave.graveId(), grave);
        gravesByOwner.computeIfAbsent(grave.ownerId(), key -> new ArrayList<>()).add(grave.graveId());
    }

    private void unregister(GraveData grave) {
        graves.remove(grave.graveId());
        List<UUID> owned = gravesByOwner.get(grave.ownerId());
        if (owned != null) {
            owned.remove(grave.graveId());
            if (owned.isEmpty()) {
                gravesByOwner.remove(grave.ownerId());
            }
        }
    }

    public GraveData get(UUID graveId) {
        return graves.get(graveId);
    }

    public List<GraveData> gravesOf(UUID ownerId) {
        List<UUID> ids = gravesByOwner.getOrDefault(ownerId, List.of());
        List<GraveData> result = new ArrayList<>();
        for (UUID id : ids) {
            GraveData grave = graves.get(id);
            if (grave != null) {
                result.add(grave);
            }
        }
        result.sort(Comparator.comparingLong(GraveData::createdAtMillis));
        return result;
    }

    public Location locationOf(GraveData grave) {
        World world = Bukkit.getWorld(grave.worldName());
        if (world == null) {
            return null;
        }
        return new Location(world, grave.x(), grave.y(), grave.z());
    }

    public GraveData createGrave(UUID ownerId, String ownerName, Location deathLocation,
                                  List<ItemStack> items, int experienceLevel) {
        long now = System.currentTimeMillis();
        long lifetimeSeconds = plugin.getConfig().getLong("grave-lifetime-seconds", 300L);
        UUID graveId = UUID.randomUUID();
        Location blockLocation = deathLocation.getBlock().getLocation();
        GraveData grave = new GraveData(graveId, ownerId, ownerName,
                blockLocation.getWorld().getName(), blockLocation.getX(), blockLocation.getY(),
                blockLocation.getZ(), experienceLevel, now, now + lifetimeSeconds * 1000L);
        register(grave);
        storage.save(grave);
        spawnGrave(grave, items);
        scheduleExpiry(grave, lifetimeSeconds * 20L);
        return grave;
    }

    public void spawnGrave(GraveData grave, List<ItemStack> items) {
        Location location = locationOf(grave);
        if (location == null) {
            return;
        }
        World world = location.getWorld();
        Bukkit.getServer().getRegionScheduler().execute(plugin, location, () -> {
            Block block = location.getBlock();
            block.setType(Material.CHEST, false);
            if (block.getState() instanceof Chest chest) {
                chest.getPersistentDataContainer().set(graveIdKey, PersistentDataType.STRING, grave.graveId().toString());
                chest.getPersistentDataContainer().set(ownerIdKey, PersistentDataType.STRING, grave.ownerId().toString());
                chest.update(true);
                for (ItemStack item : items) {
                    if (item == null) {
                        continue;
                    }
                    Map<Integer, ItemStack> leftover = chest.getBlockInventory().addItem(item);
                    for (ItemStack overflow : leftover.values()) {
                        block.getWorld().dropItemNaturally(location, overflow);
                    }
                }
            }
            spawnLabel(grave, world, location);
        });
    }

    private void spawnLabel(GraveData grave, World world, Location chestLocation) {
        if (findLabel(grave.graveId()) != null) {
            return;
        }
        Location labelLocation = chestLocation.clone().add(0.5, 1.1, 0.5);
        ArmorStand label = (ArmorStand) world.spawnEntity(labelLocation, EntityType.ARMOR_STAND);
        label.setInvisible(true);
        label.setGravity(false);
        label.setMarker(true);
        label.setSmall(true);
        label.setCustomNameVisible(true);
        label.customName(Component.text(grave.ownerName() + "'s Grave"));
        label.getPersistentDataContainer().set(graveIdKey, PersistentDataType.STRING, grave.graveId().toString());
        label.getPersistentDataContainer().set(ownerIdKey, PersistentDataType.STRING, grave.ownerId().toString());
    }

    private ArmorStand findLabel(UUID graveId) {
        GraveData grave = graves.get(graveId);
        if (grave == null) {
            return null;
        }
        Location location = locationOf(grave);
        if (location == null) {
            return null;
        }
        for (Entity entity : location.getWorld().getNearbyEntities(location, 2, 2, 2)) {
            if (entity instanceof ArmorStand armorStand) {
                String storedId = armorStand.getPersistentDataContainer().get(graveIdKey, PersistentDataType.STRING);
                if (storedId != null && storedId.equals(graveId.toString())) {
                    return armorStand;
                }
            }
        }
        return null;
    }

    public void scheduleExpiry(GraveData grave, long delayTicks) {
        Location location = locationOf(grave);
        if (location == null) {
            return;
        }
        Bukkit.getServer().getRegionScheduler().runDelayed(plugin, location,
                task -> expireGrave(grave.graveId()), Math.max(1, delayTicks));
    }

    public void expireGrave(UUID graveId) {
        GraveData grave = graves.get(graveId);
        if (grave == null) {
            return;
        }
        Location location = locationOf(grave);
        if (location == null) {
            unregister(grave);
            storage.delete(graveId);
            return;
        }
        Bukkit.getServer().getRegionScheduler().execute(plugin, location, () -> {
            String onExpire = plugin.getConfig().getString("on-expire", "DROP");
            Block block = location.getBlock();
            if ("DROP".equalsIgnoreCase(onExpire) && block.getState() instanceof Chest chest) {
                for (ItemStack item : chest.getBlockInventory().getContents()) {
                    if (item != null) {
                        block.getWorld().dropItemNaturally(location, item);
                    }
                }
            }
            removeGrave(grave);
        });
    }

    public void removeGrave(GraveData grave) {
        Location location = locationOf(grave);
        if (location != null) {
            Block block = location.getBlock();
            if (isGraveChest(block)) {
                block.setType(Material.AIR, false);
            }
        }
        removeLabelOnly(grave);
    }

    public void removeLabelOnly(GraveData grave) {
        ArmorStand label = findLabel(grave.graveId());
        if (label != null) {
            label.remove();
        }
        unregister(grave);
        storage.delete(grave.graveId());
    }

    public boolean isGraveChest(Block block) {
        if (block.getState() instanceof Chest chest) {
            return chest.getPersistentDataContainer().has(graveIdKey, PersistentDataType.STRING);
        }
        return false;
    }

    public UUID chestGraveId(Block block) {
        if (block.getState() instanceof Chest chest) {
            String value = chest.getPersistentDataContainer().get(graveIdKey, PersistentDataType.STRING);
            return value != null ? UUID.fromString(value) : null;
        }
        return null;
    }

    public void rehydrateChunk(org.bukkit.Chunk chunk) {
        for (GraveData grave : graves.values()) {
            if (!grave.worldName().equals(chunk.getWorld().getName())) {
                continue;
            }
            int chunkX = ((int) Math.floor(grave.x())) >> 4;
            int chunkZ = ((int) Math.floor(grave.z())) >> 4;
            if (chunkX == chunk.getX() && chunkZ == chunk.getZ() && findLabel(grave.graveId()) == null) {
                Location location = locationOf(grave);
                if (location != null) {
                    spawnLabel(grave, location.getWorld(), location);
                }
            }
        }
    }
}
