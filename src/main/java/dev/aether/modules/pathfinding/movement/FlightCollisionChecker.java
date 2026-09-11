package dev.aether.modules.pathfinding.movement;

import dev.aether.modules.pathfinding.execution.FlightPathClearance;
import dev.aether.modules.pathfinding.wrapper.PathPosition;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class FlightCollisionChecker {
    private static final AABB BODY = new AABB(-0.35, 0, -0.35, 0.35, 1.8, 0.35);
    private final Function<AABB, Iterable<AABB>> collisions;
    private final Long2ByteOpenHashMap clearanceCache = new Long2ByteOpenHashMap();
    private final Long2ByteOpenHashMap proximityCache = new Long2ByteOpenHashMap();

    public FlightCollisionChecker(WalkabilityChecker checker) {
        var cache = new Long2ObjectOpenHashMap<List<AABB>>();
        collisions = search -> {
            List<AABB> result = new ArrayList<>();
            for (int x = Mth.floor(search.minX) - 1; x <= Mth.floor(search.maxX) + 1; x++) {
                for (int z = Mth.floor(search.minZ) - 1; z <= Mth.floor(search.maxZ) + 1; z++) {
                    for (int y = Mth.floor(search.minY) - 1; y <= Mth.floor(search.maxY) + 1; y++) {
                        var pos = new BlockPos(x, y, z);
                        List<AABB> boxes = cache.get(pos.asLong());
                        if (boxes == null) {
                            if (checker.getLevel().isOutsideBuildHeight(y) || !checker.getLevel().hasChunkAt(pos)
                                    || checker.isDangerous(x, y, z)) {
                                boxes = List.of(new AABB(pos));
                            } else {
                                boxes = checker.getState(x, y, z).getCollisionShape(checker.getLevel(), pos)
                                        .toAabbs().stream().map(box -> box.move(pos)).toList();
                            }
                            cache.put(pos.asLong(), boxes);
                        }
                        for (AABB box : boxes) {
                            if (box.intersects(search)) result.add(box);
                        }
                    }
                }
            }
            return result;
        };
    }

    FlightCollisionChecker(Function<AABB, Iterable<AABB>> collisions) {
        this.collisions = collisions;
    }

    public static FlightCollisionChecker over(Function<AABB, Iterable<AABB>> collisions) {
        return new FlightCollisionChecker(collisions);
    }

    public boolean hasClearance(PathPosition position) {
        long key = BlockPos.asLong(position.flooredX(), position.flooredY(), position.flooredZ());
        byte cached = clearanceCache.get(key);
        if (cached != 0) return cached == 2;
        boolean clear = isClear(position, position);
        clearanceCache.put(key, clear ? (byte) 2 : (byte) 1);
        return clear;
    }

    public boolean isClear(PathPosition from, PathPosition to) {
        Vec3 start = waypoint(from);
        return FlightPathClearance.isClear(BODY.move(start), waypoint(to).subtract(start), collisions);
    }

    public boolean isNearObstacle(PathPosition position) {
        long key = BlockPos.asLong(position.flooredX(), position.flooredY(), position.flooredZ());
        byte cached = proximityCache.get(key);
        if (cached != 0) return cached == 2;
        boolean nearby = !FlightPathClearance.isClear(BODY.move(waypoint(position)).inflate(0.5), Vec3.ZERO, collisions);
        proximityCache.put(key, nearby ? (byte) 2 : (byte) 1);
        return nearby;
    }

    public static Vec3 waypoint(PathPosition position) {
        return new Vec3(position.flooredX() + 0.5, position.flooredY() + 0.15, position.flooredZ() + 0.5);
    }
}
