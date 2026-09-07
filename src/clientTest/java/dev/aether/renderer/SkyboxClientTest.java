package dev.aether.renderer;

import dev.aether.config.AetherConfig;
import dev.aether.modules.visuals.Skybox;
import dev.aether.ui.MainGUI;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CloudStatus;
import net.minecraft.world.level.Level;

public final class SkyboxClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(true)
                .adjustSettings(settings -> {
                    settings.setName("Aether skybox regressions");
                    settings.setAllowCommands(true);
                }).create()) {
            world.getClientLevel().waitForChunksDownload();
            world.getServer().runCommand("gamemode spectator @p");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("fill -16 102 -30 16 102 8 moss_block");
            world.getServer().runCommand("fill -8 103 -12 -6 115 -10 stone_bricks");
            world.getServer().runCommand("fill 6 103 -22 8 111 -20 stone_bricks");
            world.getServer().runCommand("tp @p 0 110 0 180 -15");
            context.runOnClient(client -> {
                client.options.cloudStatus().set(CloudStatus.FANCY);
                client.options.hideGui = true;
                AetherConfig.SKYBOX_ENABLED.set(true);
                AetherConfig.SKYBOX_SPEED.set(0f);
            });
            context.waitTicks(5);
            for (int preset = 0; preset < Skybox.PRESETS.size(); preset++) {
                int selection = preset;
                context.runOnClient(client -> AetherConfig.SKYBOX_PRESET.set(selection));
                context.takeScreenshot("skybox-" + Skybox.PRESETS.get(preset).toLowerCase().replace(' ', '-'));
                assertRendered(context, true);
            }

            context.runOnClient(client -> client.options.hideGui = false);
            context.setScreen(() -> new MainGUI(new MainGUI.LaunchTarget(0, "Skybox", true)));
            context.waitTicks(10);
            context.takeScreenshot("skybox-settings");
            context.takeScreenshot(TestScreenshotOptions.of("skybox-settings-wide").withSize(1280, 720));
            context.setScreen(() -> null);
            context.runOnClient(client -> client.options.hideGui = true);

            context.runOnClient(client -> AetherConfig.SKYBOX_ENABLED.set(false));
            context.takeScreenshot("skybox-disabled");
            assertRendered(context, false);
            context.runOnClient(client -> AetherConfig.SKYBOX_ENABLED.set(true));
            context.takeScreenshot("skybox-reenabled");
            assertRendered(context, true);

            world.getServer().runCommand("execute in minecraft:the_end run tp @p 0 110 0 180 -15");
            context.waitFor(client -> client.level != null && client.level.dimension().equals(Level.END));
            world.getClientLevel().waitForChunksDownload();
            context.takeScreenshot("skybox-end-boss-fog");
            assertRendered(context, false);
            world.getServer().runCommand("tp @p 1200 110 1200 180 -15");
            context.waitFor(client -> !client.gui.getBossOverlay().shouldCreateWorldFog());
            world.getClientLevel().waitForChunksDownload();
            context.takeScreenshot("skybox-end");
            assertRendered(context, true);

            world.getServer().runCommand("execute in minecraft:the_nether run tp @p 0 110 0 180 -15");
            context.waitFor(client -> client.level != null && client.level.dimension().equals(Level.NETHER));
            world.getClientLevel().waitForChunksDownload();
            context.takeScreenshot("skybox-nether");
            assertRendered(context, false);
            System.out.println("AETHER_SKYBOX_TEST: PASS five presets, settings, toggle restoration, boss fog, End and Nether");
        } finally {
            context.runOnClient(client -> {
                AetherConfig.SKYBOX_ENABLED.set(false);
                AetherConfig.SKYBOX_SPEED.set(1f);
                AetherConfig.SKYBOX_PRESET.set(0);
                SkyboxRenderer.close();
            });
        }
    }

    private static void assertRendered(ClientGameTestContext context, boolean expected) {
        context.runOnClient(client -> {
            if (SkyboxRenderer.hidesVanillaClouds() != expected) {
                throw new AssertionError("Unexpected sky replacement state: expected " + expected);
            }
        });
    }
}
