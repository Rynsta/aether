package dev.aether.modules.pathfinding.execution;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class WalkingAimSpline {
    private static final double SAMPLE_SPACING = 0.15;
    private static final double CORNER_RADIUS = 1.0;

    private final List<Vec3> anchors;
    private final List<Vec3> samples = new ArrayList<>();
    private final List<Double> parameters = new ArrayList<>();
    private final List<Double> distances = new ArrayList<>();
    private double progress;
    private int index;

    public WalkingAimSpline(List<Vec3> feetAnchors) {
        anchors = List.copyOf(feetAnchors);
        if (anchors.isEmpty()) {
            return;
        }
        append(anchors.getFirst(), 0.0);
        for (int i = 1; i + 1 < anchors.size(); i++) {
            Vec3 before = anchors.get(i - 1);
            Vec3 corner = anchors.get(i);
            Vec3 after = anchors.get(i + 1);
            double incoming = before.distanceTo(corner);
            double outgoing = corner.distanceTo(after);
            double entryRatio = incoming > 1.0e-6 ? Math.min(0.5, CORNER_RADIUS / incoming) : 0.0;
            double exitRatio = outgoing > 1.0e-6 ? Math.min(0.5, CORNER_RADIUS / outgoing) : 0.0;
            Vec3 entry = corner.lerp(before, entryRatio);
            Vec3 exit = corner.lerp(after, exitRatio);
            append(entry, i - entryRatio);
            int steps = Math.max(4, (int) Math.ceil((entry.distanceTo(corner) + corner.distanceTo(exit))
                    / SAMPLE_SPACING));
            for (int step = 1; step <= steps; step++) {
                double t = (double) step / steps;
                Vec3 point = entry.scale((1.0 - t) * (1.0 - t))
                        .add(corner.scale(2.0 * (1.0 - t) * t))
                        .add(exit.scale(t * t));
                append(point, i - entryRatio + (entryRatio + exitRatio) * t);
            }
        }
        if (anchors.size() > 1) {
            append(anchors.getLast(), anchors.size() - 1.0);
        }
    }

    public Vec3 aimPoint(Vec3 playerFeet, int pursuitSegment, double lookaheadBlocks, double eyeHeight) {
        if (samples.isEmpty()) {
            return playerFeet.add(0.0, eyeHeight, 0.0);
        }
        int segment = Math.max(0, Math.min(pursuitSegment, anchors.size() - 1));
        double minParameter = Math.max(0.0, segment - 0.5);
        double maxParameter = Math.min(anchors.size() - 1.0, segment + 1.5);
        double closestDistance = Double.POSITIVE_INFINITY;
        double projectedProgress = progress;
        for (int i = lowerSample(minParameter); i + 1 < samples.size(); i++) {
            if (parameters.get(i) > maxParameter) {
                break;
            }
            Vec3 a = samples.get(i);
            Vec3 delta = samples.get(i + 1).subtract(a);
            double parameterSpan = parameters.get(i + 1) - parameters.get(i);
            double minT = parameterSpan > 1.0e-9
                    ? Math.max(0.0, (minParameter - parameters.get(i)) / parameterSpan) : 0.0;
            double maxT = parameterSpan > 1.0e-9
                    ? Math.min(1.0, (maxParameter - parameters.get(i)) / parameterSpan) : 1.0;
            if (minT > maxT) {
                continue;
            }
            Vec3 offset = playerFeet.subtract(a);
            double horizontalLength = delta.horizontalDistanceSqr();
            boolean vertical = horizontalLength < 1.0e-12;
            double length = vertical ? delta.lengthSqr() : horizontalLength;
            if (length < 1.0e-12) {
                continue;
            }
            double t = vertical ? offset.dot(delta) / length
                    : (offset.x * delta.x + offset.z * delta.z) / length;
            t = Math.max(minT, Math.min(maxT, t));
            Vec3 error = offset.subtract(delta.scale(t));
            double distance = vertical ? error.lengthSqr() : error.horizontalDistanceSqr();
            if (distance < closestDistance - 1.0e-9) {
                closestDistance = distance;
                projectedProgress = distances.get(i) + (distances.get(i + 1) - distances.get(i)) * t;
            }
        }
        progress = Math.max(progress, projectedProgress);
        double targetDistance = Math.min(distances.getLast(), progress + Math.max(0.0, lookaheadBlocks));
        index = lowerDistance(targetDistance);
        if (index + 1 >= samples.size()) {
            return samples.getLast().add(0.0, eyeHeight, 0.0);
        }
        double length = distances.get(index + 1) - distances.get(index);
        double t = length > 1.0e-9 ? (targetDistance - distances.get(index)) / length : 0.0;
        return samples.get(index).lerp(samples.get(index + 1), t).add(0.0, eyeHeight, 0.0);
    }

    public List<Vec3> points(double eyeHeight) {
        return samples.stream().map(point -> point.add(0.0, eyeHeight, 0.0)).toList();
    }

    public int index() {
        return index;
    }

    private void append(Vec3 point, double parameter) {
        double distance = samples.isEmpty() ? 0.0 : distances.getLast() + samples.getLast().distanceTo(point);
        samples.add(point);
        parameters.add(parameter);
        distances.add(distance);
    }

    private int lowerSample(double parameter) {
        return lowerIndex(parameters, parameter);
    }

    private int lowerDistance(double distance) {
        return lowerIndex(distances, distance);
    }

    private static int lowerIndex(List<Double> values, double target) {
        int low = 0;
        int high = values.size() - 1;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (values.get(middle) <= target) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }
}
