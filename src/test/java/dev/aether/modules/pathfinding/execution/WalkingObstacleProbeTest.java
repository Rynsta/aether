package dev.aether.modules.pathfinding.execution;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class WalkingObstacleProbeTest {
    private static final AABB PLAYER = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
    private static final Vec3 FORWARD = new Vec3(0, 0, 1);

    @Test
    void detectsFullBlocksBeforeHorizontalCollision() {
        var result = probe(Vec3.ZERO, new AABB(-0.5, 0, 0.9, 0.5, 1, 1.9));
        assertTrue(result.jumpRequired());
        assertEquals(0.6, result.clearance(), 0.001);
        assertEquals(1.0, result.obstacleHeight());
    }

    @Test
    void projectsMovementSpeedToFireEarlier() {
        AABB obstacle = new AABB(-0.5, 0, 1.5, 0.5, 1, 2.5);
        assertFalse(probe(Vec3.ZERO, obstacle).obstacleAhead());
        assertTrue(probe(new Vec3(0, 0, 0.35), obstacle).jumpRequired());
        assertFalse(probe(new Vec3(0, 0, -0.35), obstacle).obstacleAhead());
        assertFalse(probe(new Vec3(0.35, 0, 0), obstacle).obstacleAhead());
    }

    @Test
    void probesPlayerEdgesWhenTheCenterRayMisses() {
        var result = probe(Vec3.ZERO, new AABB(0.2, 0, 0.8, 1.2, 1, 1.8));
        assertTrue(result.jumpRequired());
    }

    @Test
    void probesWorldMovementDirectionIndependentlyOfViewYaw() {
        var world = world(new AABB(0.8, 0, -0.5, 1.8, 1, 0.5));
        assertTrue(WalkingObstacleProbe.probe(world, PLAYER, new Vec3(1, 0, 0),
                Vec3.ZERO, 0.6, 1.2, 0.75, 2).jumpRequired());
        assertFalse(WalkingObstacleProbe.probe(world, PLAYER, FORWARD,
                Vec3.ZERO, 0.6, 1.2, 0.75, 2).obstacleAhead());
    }

    @Test
    void walksSlabsAndTheFirstStairTread() {
        assertFalse(probe(Vec3.ZERO, new AABB(-0.5, 0, 0.7, 0.5, 0.5, 1.7)).obstacleAhead());
        assertFalse(probe(Vec3.ZERO,
                new AABB(-0.5, 0, 0.6, 0.5, 0.5, 1.6),
                new AABB(-0.5, 0.5, 1.1, 0.5, 1, 1.6)).obstacleAhead());
    }

    @Test
    void respectsActualStepHeight() {
        var world = world(new AABB(-0.5, 0, 0.7, 0.5, 0.5, 1.7));
        assertTrue(WalkingObstacleProbe.probe(world, PLAYER, FORWARD,
                Vec3.ZERO, 0.3, 1.2, 0.75, 2).jumpRequired());
    }

    @Test
    void rejectsFencesTallWallsAndLowCeilings() {
        assertFalse(probe(Vec3.ZERO, new AABB(-0.5, 0, 0.7, 0.5, 1.5, 1.7)).jumpRequired());
        assertFalse(probe(Vec3.ZERO, new AABB(-0.5, 0, 0.7, 0.5, 2, 1.7)).jumpRequired());
        var ceiling = probe(Vec3.ZERO,
                new AABB(-0.5, 0, 0.7, 0.5, 1, 1.7),
                new AABB(-1, 2, -1, 1, 3, 2));
        assertTrue(ceiling.obstacleAhead());
        assertFalse(ceiling.headroomClear());
        assertFalse(ceiling.jumpRequired());
    }

    @Test
    void checksHeadroomAboveTheLandingEdge() {
        var result = probe(Vec3.ZERO,
                new AABB(-0.5, 0, 0.7, 0.5, 1, 1.7),
                new AABB(-0.5, 2, 0.7, 0.5, 3, 1.7));
        assertFalse(result.jumpRequired());
        assertFalse(result.headroomClear());
    }

    @Test
    void checksFractionalFeetHeightAndBoostedJumpLimits() {
        var world = world(new AABB(-0.5, 0.5, 0.7, 0.5, 1.5, 1.7));
        var result = WalkingObstacleProbe.probe(world, PLAYER.move(0, 0.5, 0), FORWARD,
                Vec3.ZERO, 0.6, 1.2, 0.75, 2);
        assertTrue(result.jumpRequired());
        assertEquals(1.0, result.obstacleHeight());
        var highWorld = world(new AABB(-0.5, 0, 0.7, 0.5, 2, 1.7));
        assertFalse(WalkingObstacleProbe.probe(highWorld, PLAYER, FORWARD,
                Vec3.ZERO, 0.6, 1.2, 0.75, 2).jumpRequired());
        assertTrue(WalkingObstacleProbe.probe(highWorld, PLAYER, FORWARD,
                Vec3.ZERO, 0.6, 2.2, 0.75, 2).jumpRequired());
    }

    @Test
    void calculatesVanillaAndBoostedJumpApex() {
        assertEquals(1.2522, WalkingObstacleProbe.jumpHeight(0.42, 0.08), 0.0001);
        assertTrue(WalkingObstacleProbe.jumpHeight(0.62, 0.08) > 2.0);
        assertTrue(WalkingObstacleProbe.jumpHeight(0.21, 0.08) < 0.6);
    }

    @Test
    void doesNotJumpWithoutMovement() {
        assertFalse(WalkingObstacleProbe.probe(world(new AABB(-0.5, 0, 0.7, 0.5, 1, 1.7)),
                PLAYER, Vec3.ZERO, Vec3.ZERO, 0.6, 1.2, 0.75, 2).jumpRequired());
    }

    private static WalkingObstacleProbe.Result probe(Vec3 velocity, AABB... obstacles) {
        return WalkingObstacleProbe.probe(world(obstacles), PLAYER, FORWARD, velocity,
                0.6, 1.2, 0.75, 2);
    }

    private static WalkingObstacleProbe.CollisionSpace world(AABB... obstacles) {
        return new WalkingObstacleProbe.CollisionSpace() {
            @Override
            public WalkingObstacleProbe.Collision raycast(Vec3 from, Vec3 to) {
                WalkingObstacleProbe.Collision nearest = null;
                for (AABB obstacle : obstacles) {
                    Vec3 hit = obstacle.contains(from) ? from : obstacle.clip(from, to).orElse(null);
                    if (hit != null && (nearest == null
                            || from.distanceToSqr(hit) < from.distanceToSqr(nearest.position()))) {
                        nearest = new WalkingObstacleProbe.Collision(hit, obstacle.maxY);
                    }
                }
                return nearest;
            }

            @Override
            public boolean clear(AABB bounds) {
                return Arrays.stream(obstacles).noneMatch(bounds::intersects);
            }
        };
    }
}
