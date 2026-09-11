package dev.aether.modules.pathfinding.movement;

import dev.aether.modules.pathfinding.Node;
import dev.aether.modules.pathfinding.wrapper.PathPosition;

import java.util.ArrayList;
import java.util.List;

public final class FlightPathSmoother {
    // creative flight only trims height in 0.15 steps, so a merged climb is flown as a band around
    // the ideal slope rather than along it, and only merges with room for that band are safe
    private static final double SLOPE_MARGIN = 0.15;

    @FunctionalInterface
    public interface Clearance {
        boolean isClear(PathPosition from, PathPosition to, double verticalMargin);
    }

    private FlightPathSmoother() {}

    public static List<Node> smooth(List<Node> path, Clearance clear) {
        if (path.isEmpty()) return path;
        List<Node> result = new ArrayList<>();
        result.add(path.getFirst());
        int anchor = 0;
        while (anchor < path.size() - 1) {
            PathPosition from = path.get(anchor).position;
            int lastValid = anchor + 1;
            if (!clear.isClear(from, path.get(lastValid).position, 0.0)) return List.of();
            for (int candidate = anchor + 2; candidate < path.size(); candidate++) {
                PathPosition to = path.get(candidate).position;
                double margin = to.flooredY() == from.flooredY() ? 0.0 : SLOPE_MARGIN;
                if (!clear.isClear(from, to, margin)) break;
                lastValid = candidate;
            }
            result.add(path.get(lastValid));
            anchor = lastValid;
        }
        return result;
    }
}
