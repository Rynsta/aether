Run the pathfinding client regressions with JDK 25 and a working display:

```sh
./gradlew -I scripts/test_pathfinding_client.gradle runClient --no-daemon
```

The test creates a world in `build/pathfinding-client-test/run` and runs generated
routes over slabs, stairs, a jump ledge, and a 40-block vertical drop. Inclines run
at normal and four times normal movement speed. Failures stop the Gradle task;
success logs `AETHER_PATHFINDING_TEST: PASS all six generated movement courses`.

These tests are opt-in and their classes are excluded from the released mod.
