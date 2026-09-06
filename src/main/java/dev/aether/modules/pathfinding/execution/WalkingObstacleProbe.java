package dev.aether.modules.pathfinding.execution;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class WalkingObstacleProbe {
    private static final double EPSILON = 0.02;
    private static final double MAX_LOOKAHEAD = 4.0;
    private static final Result CLEAR = new Result(false, false, Double.POSITIVE_INFINITY, 0.0, true);

    public record Result(boolean obstacleAhead, boolean jumpRequired, double clearance,
                         double obstacleHeight, boolean headroomClear) {}

    record Collision(Vec3 position, double top) {}

    interface CollisionSpace {
        Collision raycast(Vec3 from, Vec3 to);
        boolean clear(AABB bounds);
    }

    private WalkingObstacleProbe() {}

    public static Result probe(Minecraft client, Vec3 movementDirection, double baseLookahead,
                               double predictionTicks, double maxJumpHeight) {
        if (client.player == null || client.level == null) {
            return CLEAR;
        }
        double jumpHeight = Math.min(maxJumpHeight + 0.125, jumpHeight(client) - EPSILON);
        return probe(collisionSpace(client), client.player.getBoundingBox(), movementDirection,
                client.player.getDeltaMovement(), client.player.maxUpStep(), jumpHeight,
                baseLookahead, predictionTicks);
    }

    public static boolean hasJumpHeadroom(Minecraft client) {
        return client.player != null && client.level != null
                && hasHeadroom(collisionSpace(client), client.player.getBoundingBox(),
                Math.min(1.0, jumpHeight(client)));
    }

    static Result probe(CollisionSpace space, AABB bounds, Vec3 movementDirection, Vec3 velocity,
                        double stepHeight, double maxJumpHeight, double baseLookahead, double predictionTicks) {
        double length = movementDirection.horizontalDistance();
        if (length < 1.0e-6) {
            return CLEAR;
        }
        Vec3 direction = new Vec3(movementDirection.x / length, 0.0, movementDirection.z / length);
        double closingSpeed = Math.max(0.0, velocity.x * direction.x + velocity.z * direction.z);
        double reach = Math.min(MAX_LOOKAHEAD, Math.max(0.05, baseLookahead)
                + closingSpeed * Math.max(0.0, predictionTicks));
        Vec3 travel = direction.scale(reach);
        double minX = bounds.minX + EPSILON;
        double maxX = bounds.maxX - EPSILON;
        double minZ = bounds.minZ + EPSILON;
        double maxZ = bounds.maxZ - EPSILON;
        double feetY = bounds.minY + EPSILON;
        Vec3[] origins = {
                new Vec3((minX + maxX) * 0.5, feetY, (minZ + maxZ) * 0.5),
                new Vec3(minX, feetY, minZ), new Vec3(minX, feetY, maxZ),
                new Vec3(maxX, feetY, minZ), new Vec3(maxX, feetY, maxZ)
        };
        double nearestDistance = Double.POSITIVE_INFINITY;
        double obstacleHeight = 0.0;
        for (Vec3 from : origins) {
            Collision collision = space.raycast(from, from.add(travel));
            if (collision == null) {
                continue;
            }
            double height = collision.top() - bounds.minY;
            if (height <= stepHeight + EPSILON) {
                continue;
            }
            double distance = Math.max(0.0, collision.position().subtract(from).dot(direction) - EPSILON);
            if (distance < nearestDistance - EPSILON) {
                nearestDistance = distance;
                obstacleHeight = height;
            } else if (distance <= nearestDistance + EPSILON) {
                obstacleHeight = Math.max(obstacleHeight, height);
            }
        }
        if (!Double.isFinite(nearestDistance)) {
            return CLEAR;
        }
        boolean heightReachable = obstacleHeight <= maxJumpHeight;
        double rise = obstacleHeight + EPSILON;
        boolean headroomClear = heightReachable && hasHeadroom(space, bounds, rise);
        if (headroomClear) {
            AABB elevated = bounds.deflate(EPSILON).move(0.0, rise, 0.0);
            double landingDistance = nearestDistance + 0.15;
            int samples = Math.max(1, (int) Math.ceil(landingDistance / 0.2));
            for (int sample = 1; sample <= samples; sample++) {
                if (!space.clear(elevated.move(direction.scale(landingDistance * sample / samples)))) {
                    headroomClear = false;
                    break;
                }
            }
        }
        return new Result(true, heightReachable && headroomClear, nearestDistance, obstacleHeight, headroomClear);
    }

    static double jumpHeight(double initialVelocity, double gravity) {
        if (initialVelocity <= 0.0 || gravity <= 0.0) {
            return 0.0;
        }
        double height = 0.0;
        double velocity = initialVelocity;
        for (int tick = 0; tick < 100 && velocity > 0.0; tick++) {
            height += velocity;
            velocity = (velocity - gravity) * 0.98;
        }
        return height;
    }

    private static double jumpHeight(Minecraft client) {
        var player = client.player;
        float jumpFactor = client.level.getBlockState(player.blockPosition()).getBlock().getJumpFactor();
        if (jumpFactor == 1.0f) {
            BlockPos support = BlockPos.containing(player.getX(), player.getY() - 0.500001, player.getZ());
            jumpFactor = client.level.getBlockState(support).getBlock().getJumpFactor();
        }
        double initialVelocity = player.getAttributeValue(Attributes.JUMP_STRENGTH) * jumpFactor
                + player.getJumpBoostPower();
        return jumpHeight(initialVelocity, player.getGravity());
    }

    private static boolean hasHeadroom(CollisionSpace space, AABB bounds, double rise) {
        return rise > 0.0 && space.clear(new AABB(bounds.minX + EPSILON, bounds.maxY,
                bounds.minZ + EPSILON, bounds.maxX - EPSILON, bounds.maxY + rise, bounds.maxZ - EPSILON));
    }

    private static CollisionSpace collisionSpace(Minecraft client) {
        return new CollisionSpace() {
            @Override
            public Collision raycast(Vec3 from, Vec3 to) {
                BlockHitResult hit = client.level.clip(new ClipContext(from, to,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
                if (hit.getType() != HitResult.Type.BLOCK) {
                    return null;
                }
                BlockPos position = hit.getBlockPos();
                var shape = client.level.getBlockState(position).getCollisionShape(client.level, position,
                        CollisionContext.of(client.player));
                if (shape.isEmpty()) {
                    return null;
                }
                Vec3 inside = hit.getLocation().add(to.subtract(from).normalize().scale(0.001))
                        .subtract(position.getX(), position.getY(), position.getZ());
                double top = Double.NEGATIVE_INFINITY;
                for (AABB box : shape.toAabbs()) {
                    if (box.inflate(0.001).contains(inside)) {
                        top = Math.max(top, box.maxY);
                    }
                }
                if (!Double.isFinite(top)) {
                    top = shape.bounds().maxY;
                }
                return new Collision(hit.getLocation(), position.getY() + top);
            }

            @Override
            public boolean clear(AABB bounds) {
                return client.level.noBlockCollision(client.player, bounds);
            }
        };
    }
}
