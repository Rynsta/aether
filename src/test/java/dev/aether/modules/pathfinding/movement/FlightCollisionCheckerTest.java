package dev.aether.modules.pathfinding.movement;

import dev.aether.modules.pathfinding.pathfinder.AStarPathfinder;
import dev.aether.modules.pathfinding.pathing.NeighborStrategies;
import dev.aether.modules.pathfinding.pathing.configuration.PathfinderConfiguration;
import dev.aether.modules.pathfinding.pathing.processing.impl.FlyPathProcessor;
import dev.aether.modules.pathfinding.wrapper.PathPosition;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlightCollisionCheckerTest {
    @Test
    void detectsFenceTopsExtendingOutOfTheBlockBelowTheFeet() {
        var checker = checker(List.of(new AABB(0.375, -1, 0.375, 0.625, 0.5, 0.625)));
        assertFalse(checker.hasClearance(pos(0, 0, 0)));
        assertTrue(checker.hasClearance(pos(0, 1, 0)));
    }

    @Test
    void rejectsDiagonalTrunkCornersEvenWithBothEndpointsOpen() {
        var checker = checker(List.of(new AABB(1, 0, 0, 2, 3, 1)));
        assertTrue(checker.hasClearance(pos(0, 0, 0)));
        assertTrue(checker.hasClearance(pos(1, 0, 1)));
        assertFalse(checker.isClear(pos(0, 0, 0), pos(1, 0, 1)));
        assertFalse(checker.isClear(pos(1, 0, 1), pos(0, 0, 0)));
    }

    @Test
    void detectsThinBranchesAndLowCanopiesWithAMarginAtTheSides() {
        assertFalse(checker(List.of(new AABB(0, 1.9, 0, 1, 3, 1))).hasClearance(pos(0, 0, 0)));
        assertFalse(checker(List.of(new AABB(0.84, 0.5, 2, 0.9, 0.6, 2.01)))
                .isClear(pos(0, 0, 0), pos(0, 0, 5)));
        var checker = checker(List.of(new AABB(1, 0, 0, 2, 3, 1)));
        assertTrue(checker.isNearObstacle(pos(0, 0, 0)));
        assertFalse(checker.isNearObstacle(pos(-2, 0, 0)));
    }

    @Test
    void allowsOneBlockWideTwoBlockHighPassages() {
        var checker = checker(List.of(new AABB(-1, -1, -1, 2, 0, 8),
                new AABB(-1, 2, -1, 2, 3, 8), new AABB(-1, 0, -1, 0, 2, 8),
                new AABB(1, 0, -1, 2, 2, 8)));
        assertTrue(checker.isClear(pos(0, 0, 0), pos(0, 0, 6)));
    }

    @Test
    void findsCollisionFreeRoutesThroughDenseStaggeredTreesInBothDirections() {
        List<AABB> obstacles = new ArrayList<>();
        obstacles.add(new AABB(-20, -1, -20, 30, 0, 40));
        for (int x = -6; x <= 6; x += 3) {
            for (int z = 3; z <= 21; z += 4) {
                int shift = (z / 4) % 2;
                obstacles.add(new AABB(x + shift, 0, z, x + shift + 1, 6, z + 1));
                obstacles.add(new AABB(x + shift - 1, 4, z - 1, x + shift + 2, 7, z + 2));
            }
        }
        assertRoute(obstacles, pos(0, 2, 0), pos(0, 2, 25));
        assertRoute(obstacles, pos(0, 2, 25), pos(0, 2, 0));
        assertRoute(obstacles, pos(0, 8, 0), pos(0, 2, 25));
    }

    @Test
    void routesOverAFenceAndUnderALowRoofWithoutUsingTheFenceCell() {
        var obstacles = List.of(new AABB(-3, -1, -1, 4, 0, 12),
                new AABB(-3, 0, 4, 4, 1.5, 4.3), new AABB(-3, 4, 3, 4, 5, 10));
        assertRoute(obstacles, pos(0, 0, 0), pos(0, 0, 11));
    }

    private static void assertRoute(List<AABB> obstacles, PathPosition start, PathPosition goal) {
        var checker = checker(obstacles);
        var config = PathfinderConfiguration.builder().provider((position, context) -> null)
                .processors(List.of(new FlyPathProcessor(checker)))
                .neighborStrategy(NeighborStrategies.HORIZONTAL_DIAGONAL_AND_VERTICAL)
                .maxIterations(30000).maxLength(1000).fallback(false).async(false).build();
        var result = new AStarPathfinder(config).findPath(start, goal).toCompletableFuture().join();
        assertTrue(result.successful(), () -> "Failed route " + start + " -> " + goal);
        PathPosition previous = start;
        for (PathPosition step : result.getPath()) {
            assertTrue(checker.isClear(previous, step), () -> "Obstructed step: " + step);
            previous = step;
        }
        assertEquals(goal, previous);
    }

    private static FlightCollisionChecker checker(List<AABB> obstacles) {
        return new FlightCollisionChecker(search -> obstacles.stream().filter(search::intersects).toList());
    }

    private static PathPosition pos(int x, int y, int z) {
        return new PathPosition(x, y, z);
    }
}
