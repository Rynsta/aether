package dev.aether.modules.pathfinding.execution;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlightPathClearanceTest {
    private static final AABB PLAYER = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
    private static final Vec3 HOP = new Vec3(0, 0, 12);

    @Test
    void rejectsAWallEvenWhenTheHopDestinationIsOpen() {
        assertFalse(clear(HOP, new AABB(-2, -2, 5, 2, 5, 6)));
        assertTrue(clear(HOP, new AABB(-2, -2, 13, 2, 5, 14)));
    }

    @Test
    void detectsFeetAndHeadObstaclesOutsideTheEyeRay() {
        assertFalse(clear(HOP, new AABB(-1, 0, 4, 1, 0.5, 5)));
        assertFalse(clear(HOP, new AABB(-1, 1.7, 4, 1, 3, 5)));
    }

    @Test
    void detectsSideEdgesAndThinBeamsBetweenCornerRays() {
        assertFalse(clear(HOP, new AABB(0.29, 0, 4, 1, 2, 5)));
        assertFalse(clear(HOP, new AABB(0.08, 0.6, 4, 0.12, 0.7, 4.01)));
        assertTrue(clear(HOP, new AABB(0.31, 0, 4, 1, 2, 5)));
    }

    @Test
    void checksTheLandingSpaceIncludingTheFrontOfThePlayer() {
        assertFalse(clear(HOP, new AABB(-1, 0, 12.2, 1, 2, 13)));
    }

    @Test
    void blocksAscendingAndDescendingHopsAtAnyAltitude() {
        AABB ceiling = new AABB(-2, 3, 3, 2, 4, 8);
        AABB roof = new AABB(-2, -3, 3, 2, -2, 8);
        assertFalse(clear(new Vec3(0, 6, 10), ceiling));
        assertFalse(clear(new Vec3(0, -6, 10), roof));
        assertFalse(clear(new Vec3(0, 12, 0), new AABB(-1, 4, -1, 1, 5, 1)));
    }

    @Test
    void sweepsDiagonalCornersWithoutBlockingObstaclesAwayFromTheRoute() {
        assertFalse(clear(new Vec3(8, 0, 8), new AABB(3.2, 0, 3.7, 4.2, 3, 4.7)));
        assertFalse(clear(new Vec3(-8, 0, -8), new AABB(-4.2, 0, -4.7, -3.2, 3, -3.7)));
        assertTrue(clear(new Vec3(8, 0, 8), new AABB(0, 0, 4, 1, 3, 5)));
    }

    @Test
    void allowsFlightAlongTouchingFloorsAndWalls() {
        assertTrue(clear(HOP,
                new AABB(-1, -1, -1, 1, 0, 15),
                new AABB(0.3, 0, -1, 1, 3, 15)));
        assertFalse(clear(new Vec3(0, -0.1, 12), new AABB(-1, -1, -1, 1, 0, 15)));
    }

    @Test
    void checksTheCurrentBodyWhenThereIsNoMovement() {
        assertTrue(clear(Vec3.ZERO));
        assertFalse(clear(Vec3.ZERO, new AABB(-0.1, 0.5, -0.1, 0.1, 0.6, 0.1)));
    }

    @Test
    void boundsCollisionQueriesAlongLongRoutesAndChecksTheLastSegment() {
        AABB obstacle = new AABB(59.9, 59.9, 59.9, 60.1, 60.1, 60.1);
        assertFalse(FlightPathClearance.isClear(PLAYER, new Vec3(60, 60, 60), search -> {
            assertTrue(search.getXsize() < 5);
            assertTrue(search.getYsize() < 6);
            assertTrue(search.getZsize() < 5);
            return search.intersects(obstacle) ? List.of(obstacle) : List.of();
        }));
    }

    private static boolean clear(Vec3 travel, AABB... obstacles) {
        return FlightPathClearance.isClear(PLAYER, travel, search -> Arrays.stream(obstacles)
                .filter(search::intersects).toList());
    }
}
