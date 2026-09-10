package dev.aether.modules.pathfinding.movement;

import dev.aether.modules.pathfinding.Node;
import dev.aether.modules.pathfinding.wrapper.PathPosition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlightPathSmootherTest {
    @Test
    void preservesClimbBeforeCrossingACanopyAndDescentAfterIt() {
        var path = List.of(node(0, 0, 0), node(0, 1, 0), node(0, 2, 0),
                node(0, 2, 1), node(0, 2, 2), node(0, 1, 2), node(0, 0, 2));
        assertEquals(List.of(path.get(0), path.get(2), path.get(4), path.get(6)),
                FlightPathSmoother.smooth(path, (from, to) -> true));
    }

    @Test
    void preservesATurnAroundAnObstacle() {
        var path = List.of(node(0, 0, 0), node(0, 0, 1), node(1, 0, 1));
        assertEquals(path, FlightPathSmoother.smooth(path,
                (from, to) -> from.flooredX() == to.flooredX() || from.flooredZ() == to.flooredZ()));
    }

    @Test
    void rejectsBlockedOriginalEdgesIncludingTwoNodeRoutes() {
        var path = List.of(node(0, 0, 0), node(0, 0, 1));
        assertTrue(FlightPathSmoother.smooth(path, (from, to) -> false).isEmpty());
        assertTrue(FlightPathSmoother.smooth(List.of(node(0, 0, 0), node(0, 0, 1), node(0, 0, 2)),
                (from, to) -> to.flooredZ() < 2).isEmpty());
    }

    @Test
    void collapsesOpenHorizontalRoutes() {
        var path = List.of(node(0, 0, 0), node(0, 0, 1), node(1, 0, 2), node(2, 0, 3));
        assertEquals(List.of(path.getFirst(), path.getLast()),
                FlightPathSmoother.smooth(path, (from, to) -> true));
    }

    private static Node node(int x, int y, int z) {
        return new Node(new PathPosition(x, y, z));
    }
}
