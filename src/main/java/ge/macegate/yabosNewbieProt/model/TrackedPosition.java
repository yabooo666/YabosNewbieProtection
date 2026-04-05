package ge.macegate.yabosNewbieProt.model;

import org.bukkit.Location;

public record TrackedPosition(int blockX, int blockY, int blockZ, int yawBucket, int pitchBucket) {

    public static TrackedPosition from(Location location, boolean useBlockPosition, boolean useLookDirection) {
        int x = useBlockPosition ? location.getBlockX() : 0;
        int y = useBlockPosition ? location.getBlockY() : 0;
        int z = useBlockPosition ? location.getBlockZ() : 0;
        int yaw = useLookDirection ? Math.round(normalize(location.getYaw()) / 10.0f) : 0;
        int pitch = useLookDirection ? Math.round(location.getPitch() / 10.0f) : 0;
        return new TrackedPosition(x, y, z, yaw, pitch);
    }

    private static float normalize(float yaw) {
        float normalized = yaw % 360.0f;
        return normalized < 0 ? normalized + 360.0f : normalized;
    }
}
