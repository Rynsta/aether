# Efficient

Efficient is the recommended starting point: unhurried camera movement with a wide
pest search cone. Reapply it from Humanization to load the bundled values on a new
installation. Existing editable preset files retain their values; replace
`config/aether/humanization-presets/efficient.json` with this version to update an
older installation.

- Turns use matching quadratic ease-in/out, a 100 ms base duration, and 6 ms per
  degree for larger angles (a 640 ms minimum for 90 degrees).
- Vacuum tracking uses 190 ms smoothing and a 360°/s ceiling. Hunting uses 135 ms
  and 420°/s to retain room for the shorter stun/throw windows.
- Tracking noise is a 2–6% variation in each correction. Pest aim drift is 1.0 and
  the above-target pitch spans 25–55°; crop alignment remains exact to avoid
  shifting farming lanes.
- Pest FOV is 90° so pests are picked up without a wide sweep first. Visitor and
  pest exchange cones stay narrow at 12° and 4°.
- GUI actions, swaps, rewarps and pest reactions have nonzero timing ranges, and
  farming pauses while George is called. Bazaar warning countdowns always take
  precedence over those delays.

The preset includes movement speed and follow distances, but does not change pest
routing, equipment, farm layout, failsafes, or which automation modules are enabled.
These settings are a starting point for natural-looking motion, not a guarantee
of human-identical input. Latency, frame rate and mouse sensitivity affect the result.

# Pest visuals

Pest ESP offers Box and Glow Outline modes. Both follow rendered movement; Glow
Outline uses Minecraft's entity outline pass on the pest skull, without outlining
its invisible support stand. Tracers render once per frame above the world.

Pest Target HUD is independently toggleable under Pest Manager or Visuals → HUD.
It starts below the crosshair and supports the HUD editor's drag and Ctrl-resize.
Rows wrap into columns and scale down when needed to fit below the crosshair.

Health comes from server nameplates. When a maximum is absent, the bar uses the
highest health observed while that pest is loaded. Hunting progress reflects the
current stamina/reel bar, which can reset between reels; missing readings show an
indeterminate bar. The HUD does not estimate a catch percentage from elapsed time.
