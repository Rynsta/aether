package dev.aether.modules.pathfinding.execution;

import net.minecraft.world.phys.Vec3;

import java.util.List;

final class WalkingRoute {
    private final List<Vec3> points;
    private final double[] distances;

    WalkingRoute(List<Vec3> points) {
        this.points = List.copyOf(points);
        distances = new double[points.size()];
        for (int i = 1; i < points.size(); i++) {
            distances[i] = distances[i - 1] + points.get(i).distanceTo(points.get(i - 1));
        }
    }

    int advance(Vec3 feet, int segment) {
        while (segment + 1 < points.size()) {
            Vec3 from = points.get(segment);
            Vec3 to = points.get(segment + 1);
            if (Math.abs(feet.y - to.y) > 0.65) break;
            double distance = feet.subtract(to).horizontalDistance();
            double length = to.subtract(from).horizontalDistance();
            boolean passed = length > 1.0e-6 && horizontalProjection(feet, from, to) >= 1.0
                    && lateralDistance(feet, from, to) <= 0.45 && distance <= 1.0;
            if (distance > 0.25 && !passed) break;
            segment++;
        }
        return segment;
    }

    double progress(Vec3 feet, int segment) {
        if (points.isEmpty()) return 0.0;
        if (segment + 1 >= points.size()) return distances[distances.length - 1];
        Vec3 from = points.get(segment);
        Vec3 to = points.get(segment + 1);
        Vec3 delta = to.subtract(from);
        double t = delta.horizontalDistanceSqr() < 1.0e-6
                ? (Math.abs(delta.y) < 1.0e-6 ? 0.0 : (feet.y - from.y) / delta.y)
                : horizontalProjection(feet, from, to);
        return distances[segment] + Math.clamp(t, 0.0, 1.0) * (distances[segment + 1] - distances[segment]);
    }

    double distance(Vec3 feet, int segment) {
        if (points.isEmpty()) return 0.0;
        double best = Double.POSITIVE_INFINITY;
        int start = Math.max(0, segment - 1);
        int end = Math.min(points.size() - 1, segment + 2);
        for (int i = start; i < end; i++) {
            Vec3 from = points.get(i);
            Vec3 to = points.get(i + 1);
            double t = Math.clamp(horizontalProjection(feet, from, to), 0.0, 1.0);
            best = Math.min(best, feet.subtract(from.lerp(to, t)).horizontalDistance());
        }
        return Double.isFinite(best) ? best : feet.subtract(points.getLast()).horizontalDistance();
    }

    static boolean reachedGoal(Vec3 feet, Vec3 goal, double tolerance) {
        return feet.subtract(goal).horizontalDistance() <= tolerance && Math.abs(feet.y - goal.y) <= 0.75;
    }

    boolean endsAt(Vec3 goal) {
        if (points.isEmpty()) return false;
        Vec3 end = points.getLast();
        return Math.floor(end.x) == Math.floor(goal.x)
                && Math.floor(end.y) == Math.floor(goal.y)
                && Math.floor(end.z) == Math.floor(goal.z);
    }

    private static double horizontalProjection(Vec3 feet, Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double lengthSquared = dx * dx + dz * dz;
        return lengthSquared < 1.0e-6 ? 0.0
                : ((feet.x - from.x) * dx + (feet.z - from.z) * dz) / lengthSquared;
    }

    private static double lateralDistance(Vec3 feet, Vec3 from, Vec3 to) {
        return Math.abs((feet.x - from.x) * (to.z - from.z) - (feet.z - from.z) * (to.x - from.x))
                / to.subtract(from).horizontalDistance();
    }
}
