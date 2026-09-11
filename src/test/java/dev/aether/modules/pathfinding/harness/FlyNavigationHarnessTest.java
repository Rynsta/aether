package dev.aether.modules.pathfinding.harness;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class FlyNavigationHarnessTest {

    private record Scenario(String name, BlockWorld world, Vec3 start, float yaw, BlockPos goal, int maxTicks) {
    }

    private static List<Scenario> scenarios() {
        List<Scenario> scenarios = new ArrayList<>();
        scenarios.add(new Scenario("greenhouse entry", Structures.greenhouse(),
                new Vec3(7.5, 66.15, -5.5), 0.0f, new BlockPos(11, 65, 9), 800));
        scenarios.add(new Scenario("greenhouse crossing", Structures.greenhouse(),
                new Vec3(1.5, 65.15, 1.5), 0.0f, new BlockPos(13, 65, 9), 800));
        scenarios.add(new Scenario("greenhouse aisle", Structures.greenhouse(),
                new Vec3(2.5, 65.15, 3.5), 90.0f, new BlockPos(12, 65, 7), 800));
        scenarios.add(new Scenario("doorway hop", Structures.doorwayHop(),
                new Vec3(6.5, 65.15, 3.5), 0.0f, new BlockPos(9, 67, 15), 800));
        scenarios.add(new Scenario("narrow bend", Structures.narrowBend(),
                new Vec3(2.5, 65.15, 2.5), 0.0f, new BlockPos(16, 67, 10), 1200));
        scenarios.add(new Scenario("field wall", Structures.fieldWall(),
                new Vec3(0.5, 67.15, 0.5), 0.0f, new BlockPos(0, 67, 20), 800));
        return scenarios;
    }

    @Test
    void reportFlightQualityAcrossTightStructures() {
        List<FlightTrial.Result> results = new ArrayList<>();
        for (Scenario scenario : scenarios()) {
            long start = System.nanoTime();
            FlightTrial.Result result = FlightTrial.run(scenario.name(), scenario.world(), scenario.start(),
                    scenario.goal(), scenario.yaw(), scenario.maxTicks());
            results.add(result);
            System.out.println(result + String.format("  [%.0fms]", (System.nanoTime() - start) / 1e6));
            for (String note : result.notes()) {
                System.out.println("        replan: " + note);
            }
        }

        long reached = results.stream().filter(FlightTrial.Result::reached).count();
        double seconds = results.stream().mapToDouble(FlightTrial.Result::seconds).sum();
        System.out.printf("TOTAL reached=%d/%d time=%.1fs waypoints=%d replans=%d stalled=%d%n",
                reached, results.size(), seconds,
                results.stream().mapToInt(FlightTrial.Result::totalWaypoints).sum(),
                results.stream().mapToInt(FlightTrial.Result::repaths).sum(),
                results.stream().mapToInt(FlightTrial.Result::stallTicks).sum());
    }
}
