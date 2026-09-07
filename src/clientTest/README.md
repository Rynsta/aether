Run the pathfinding client regressions with JDK 25 and a working display:

```sh
./gradlew -I scripts/test_pathfinding_client.gradle runClient --no-daemon
```

The test creates a world in `build/pathfinding-client-test/run` and runs generated
routes over slabs, stairs, jump ledges with and without carpet, and a 40-block
vertical drop. Inclines run at normal and four times normal movement speed. Failures stop the Gradle task;
success logs `AETHER_PATHFINDING_TEST: PASS all seven generated movement courses`.

These tests are opt-in and their classes are excluded from the released mod.

Run the skybox client checks separately:

```sh
./gradlew -I scripts/test_skybox_client.gradle runClient --no-daemon
```

This creates an isolated world in `build/skybox-client-test/run`, renders all five
presets with world geometry, captures the settings panel at two sizes, checks
disabling and re-enabling, and visits the End and Nether with boss fog checks.
Success logs `AETHER_SKYBOX_TEST: PASS`. Screenshots are saved
by the Fabric client test runner. The OpenGL unit tests also save shader previews
in `build/reports/skybox` and report GPU timings at 1920x1080.
