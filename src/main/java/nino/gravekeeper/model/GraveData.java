package nino.gravekeeper.model;

import java.util.UUID;

public final class GraveData {

    private final UUID graveId;
    private final UUID ownerId;
    private final String ownerName;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final int experienceLevel;
    private final long createdAtMillis;
    private long expiresAtMillis;

    public GraveData(UUID graveId, UUID ownerId, String ownerName, String worldName,
                      double x, double y, double z, int experienceLevel,
                      long createdAtMillis, long expiresAtMillis) {
        this.graveId = graveId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.experienceLevel = experienceLevel;
        this.createdAtMillis = createdAtMillis;
        this.expiresAtMillis = expiresAtMillis;
    }

    public UUID graveId() {
        return graveId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String ownerName() {
        return ownerName;
    }

    public String worldName() {
        return worldName;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public int experienceLevel() {
        return experienceLevel;
    }

    public long createdAtMillis() {
        return createdAtMillis;
    }

    public long expiresAtMillis() {
        return expiresAtMillis;
    }

    public void setExpiresAtMillis(long expiresAtMillis) {
        this.expiresAtMillis = expiresAtMillis;
    }
}
