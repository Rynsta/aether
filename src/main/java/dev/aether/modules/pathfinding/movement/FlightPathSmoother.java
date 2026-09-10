package dev.aether.modules.pathfinding.movement;

import dev.aether.modules.pathfinding.Node;
import dev.aether.modules.pathfinding.wrapper.PathPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public final class FlightPathSmoother {
    private FlightPathSmoother() {}

    public static List<Node> smooth(List<Node> path, BiPredicate<PathPosition, PathPosition> clear) {
        if (path.isEmpty()) return path;
        List<Node> result = new ArrayList<>();
        result.add(path.getFirst());
        int anchor = 0;
        while (anchor < path.size() - 1) {
            PathPosition from = path.get(anchor).position;
            int lastValid = anchor + 1;
            if (!clear.test(from, path.get(lastValid).position)) return List.of();
            for (int candidate = anchor + 2; candidate < path.size(); candidate++) {
                PathPosition to = path.get(candidate).position;
                PathPosition previous = path.get(candidate - 1).position;
                if (previous.flooredY() != from.flooredY() || to.flooredY() != from.flooredY()) {
                    if (previous.flooredX() != from.flooredX() || previous.flooredZ() != from.flooredZ()
                            || to.flooredX() != from.flooredX() || to.flooredZ() != from.flooredZ()) break;
                }
                if (!clear.test(from, to)) break;
                lastValid = candidate;
            }
            result.add(path.get(lastValid));
            anchor = lastValid;
        }
        return result;
    }
}
