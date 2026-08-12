package nino.gravekeeper.manager;

import nino.gravekeeper.model.GraveData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EntityEquipment;
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

    public GraveData createGrave(UUID ownerId, String ownerName, Location deathLocation,
                                  List<ItemStack> items, int experienceLevel) {
        long now = System.currentTimeMillis();
        long lifetimeSeconds = plugin.getConfig().getLong("grave-lifetime-seconds", 300L);
        UUID graveId = UUID.randomUUID();
        GraveData grave = new GraveData(graveId, ownerId, ownerName,
                deathLocation.getWorld().getName(), deathLocation.getX(), deathLocation.getY(),
                deathLocation.getZ(), items, experienceLevel, now, now + lifetimeSeconds * 1000L);
        register(grave);
        storage.save(grave);
        spawnMarker(grave);
        scheduleExpiry(grave, lifetimeSeconds * 20L);
        return grave;
    }

    public void spawnMarker(GraveData grave) {
        World world = Bukkit.getWorld(grave.worldName());
        if (world == null) {
            return;
        }
        Location location = new Location(world, grave.x(), grave.y(), grave.z());
        Bukkit.getServer().getRegionScheduler().execute(plugin, location, () -> {
            if (findMarker(grave.graveId()) != null) {
                return;
            }
            ArmorStand armorStand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
            armorStand.setInvisible(true);
            armorStand.setGravity(false);
            armorStand.setMarker(true);
            armorStand.setSmall(true);
            armorStand.setCustomNameVisible(true);
            armorStand.customName(net.kyori.adventure.text.Component.text(grave.ownerName() + "'s Grave"));
            String materialName = plugin.getConfig().getString("marker-material", "PLAYER_HEAD");
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                material = Material.PLAYER_HEAD;
            }
            EntityEquipment equipment = armorStand.getEquipment();
            if (equipment != null) {
                equipment.setHelmet(new ItemStack(material));
            }
            armorStand.getPersistentDataContainer().set(graveIdKey, PersistentDataType.STRING, grave.graveId().toString());
            armorStand.getPersistentDataContainer().set(ownerIdKey, PersistentDataType.STRING, grave.ownerId().toString());
        });
    }

    private ArmorStand findMarker(UUID graveId) {
        GraveData grave = graves.get(graveId);
        if (grave == null) {
            return null;
        }
        World world = Bukkit.getWorld(grave.worldName());
        if (world == null) {
            return null;
        }
        Location location = new Location(world, grave.x(), grave.y(), grave.z());
        for (Entity entity : world.getNearbyEntities(location, 2, 2, 2)) {
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
        World world = Bukkit.getWorld(grave.worldName());
        if (world == null) {
            return;
        }
        Location location = new Location(world, grave.x(), grave.y(), grave.z());
        Bukkit.getServer().getRegionScheduler().runDelayed(plugin, location,
                task -> expireGrave(grave.graveId()), Math.max(1, delayTicks));
    }

    public void expireGrave(UUID graveId) {
        GraveData grave = graves.get(graveId);
        if (grave == null) {
            return;
        }
        World world = Bukkit.getWorld(grave.worldName());
        if (world == null) {
            unregister(grave);
            storage.delete(graveId);
            return;
        }
        Location location = new Location(world, grave.x(), grave.y(), grave.z());
        Bukkit.getServer().getRegionScheduler().execute(plugin, location, () -> {
            String onExpire = plugin.getConfig().getString("on-expire", "DROP");
            if ("DROP".equalsIgnoreCase(onExpire)) {
                for (ItemStack item : grave.items()) {
                    if (item != null) {
                        world.dropItemNaturally(location, item);
                    }
                }
            }
            removeMarker(grave);
            unregister(grave);
            storage.delete(graveId);
        });
    }

    public void removeMarker(GraveData grave) {
        ArmorStand armorStand = findMarker(grave.graveId());
        if (armorStand != null) {
            armorStand.remove();
        }
    }

    public void closeGraveIfEmpty(GraveData grave) {
        boolean empty = true;
        for (ItemStack item : grave.items()) {
            if (item != null && item.getType() != Material.AIR) {
                empty = false;
                break;
            }
        }
        if (empty) {
            removeMarker(grave);
            unregister(grave);
            storage.delete(grave.graveId());
        } else {
            storage.save(grave);
        }
    }

    public boolean isGraveMarker(Entity entity) {
        return entity.getPersistentDataContainer().has(graveIdKey, PersistentDataType.STRING);
    }

    public UUID markerGraveId(Entity entity) {
        String value = entity.getPersistentDataContainer().get(graveIdKey, PersistentDataType.STRING);
        return value != null ? UUID.fromString(value) : null;
    }

    public void rehydrateChunk(org.bukkit.Chunk chunk) {
        for (GraveData grave : graves.values()) {
            if (!grave.worldName().equals(chunk.getWorld().getName())) {
                continue;
            }
            int chunkX = ((int) Math.floor(grave.x())) >> 4;
            int chunkZ = ((int) Math.floor(grave.z())) >> 4;
            if (chunkX == chunk.getX() && chunkZ == chunk.getZ() && findMarker(grave.graveId()) == null) {
                spawnMarker(grave);
            }
        }
    }
}
