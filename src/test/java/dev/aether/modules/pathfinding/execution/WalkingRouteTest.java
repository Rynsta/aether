package dev.aether.modules.pathfinding.execution;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WalkingRouteTest {
    @Test
    void measuresProgressInBlocksOnLongSegments() {
        WalkingRoute route = new WalkingRoute(List.of(Vec3.ZERO, new Vec3(20, 0, 0)));
        assertEquals(0.1, route.progress(new Vec3(0.1, 0, 0), 0), 1.0e-8);
        assertEquals(5.0, route.progress(new Vec3(5, 0, 0), 0), 1.0e-8);
    }

    @Test
    void measuresDriftFromSegmentsInsteadOfSparseWaypoints() {
        WalkingRoute route = new WalkingRoute(List.of(Vec3.ZERO, new Vec3(20, 0, 0)));
        assertEquals(0.0, route.distance(new Vec3(10, 0, 0), 0), 1.0e-8);
        assertEquals(2.0, route.distance(new Vec3(10, 0, 2), 0), 1.0e-8);
    }

    @Test
    void cannotSkipAnAscentOrFinishOnTheWrongFloor() {
        WalkingRoute route = new WalkingRoute(List.of(Vec3.ZERO, new Vec3(1, 1, 0), new Vec3(2, 1, 0)));
        assertEquals(0, route.advance(new Vec3(1.1, 0, 0), 0));
        assertEquals(1, route.advance(new Vec3(1.1, 1, 0), 0));
        assertFalse(WalkingRoute.reachedGoal(new Vec3(2, 0, 0), new Vec3(2, 1, 0), 1.2));
    }

    @Test
    void preservesVerticalSegmentsUntilTheirHeightIsReached() {
        WalkingRoute route = new WalkingRoute(List.of(Vec3.ZERO, new Vec3(0, 3, 0), new Vec3(1, 3, 0)));
        assertEquals(0, route.advance(Vec3.ZERO, 0));
        assertEquals(1.5, route.progress(new Vec3(0, 1.5, 0), 0), 1.0e-8);
        assertEquals(1, route.advance(new Vec3(0, 3, 0), 0));
    }

    @Test
    void rejectsPassingAWaypointFromFarOffTheRoute() {
        WalkingRoute route = new WalkingRoute(List.of(Vec3.ZERO, new Vec3(3, 0, 0), new Vec3(3, 0, 3)));
        assertEquals(0, route.advance(new Vec3(3.1, 0, 2), 0));
        assertEquals(1, route.advance(new Vec3(3.1, 0, 0.1), 0));
    }

    @Test
    void preciseGoalsHonorTheRequestedHorizontalTolerance() {
        assertFalse(WalkingRoute.reachedGoal(new Vec3(0.4, 0, 0), Vec3.ZERO, 0.25));
        assertTrue(WalkingRoute.reachedGoal(new Vec3(0.2, 0, 0), Vec3.ZERO, 0.25));
        assertTrue(WalkingRoute.reachedGoal(new Vec3(0.2, -0.5, 0), Vec3.ZERO, 0.25));
    }

    @Test
    void partialEndpointsCannotAuthorizeMovementToTheOriginalGoal() {
        WalkingRoute route = new WalkingRoute(List.of(new Vec3(0.5, 0, 0.5), new Vec3(3.5, 0, 0.5)));
        assertFalse(route.endsAt(new Vec3(5.1, 0, 0.5)));
        assertFalse(route.endsAt(new Vec3(3.5, 1, 0.5)));
        assertTrue(route.endsAt(new Vec3(3.9, 0, 0.1)));
    }
}
