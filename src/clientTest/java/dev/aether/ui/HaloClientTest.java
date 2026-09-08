package dev.aether.ui;

import dev.aether.config.AetherConfig;
import dev.aether.modules.visuals.FreecamManager;
import dev.aether.renderer.CosmeticWorldRenderer;
import dev.aether.renderer.HaloRenderer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;

public final class HaloClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(true)
                .adjustSettings(settings -> {
                    settings.setName("Aether halo regressions");
                    settings.setAllowCommands(true);
                }).create()) {
            world.getClientLevel().waitForChunksDownload();
            world.getServer().runCommand("gamemode creative @p");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("fill -12 100 -12 12 100 12 stone_bricks");
            world.getServer().runCommand("tp @p 0 101 0 180 -10");
            context.runOnClient(client -> {
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                client.options.hideGui = true;
                AetherConfig.HAT_ENABLED.set(false);
                AetherConfig.DRAGON_WINGS_ENABLED.set(false);
                AetherConfig.HALO_ENABLED.set(true);
                AetherConfig.HALO_SPEED.set(0f);
            });
            context.waitTicks(10);
            for (int style = 0; style < 3; style++) {
                int selection = style;
                context.runOnClient(client -> AetherConfig.HALO_STYLE.set(selection));
                context.takeScreenshot("halo-style-" + style);
                assertVisible(context, true);
            }

            context.runOnClient(client -> {
                AetherConfig.HALO_STYLE.set(0);
                client.options.keyShift.setDown(true);
            });
            context.waitFor(client -> client.player.isCrouching());
            context.takeScreenshot("halo-crouching");
            assertVisible(context, true);
            context.runOnClient(client -> {
                client.options.keyShift.setDown(false);
                AetherConfig.DRAGON_WINGS_ENABLED.set(true);
                AetherConfig.HALO_STYLE.set(1);
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            });
            context.waitTicks(5);
            context.takeScreenshot("halo-with-wings");

            context.runOnClient(client -> client.options.hideGui = false);
            context.setScreen(() -> new MainGUI(new MainGUI.LaunchTarget(0, "Fun", true)));
            context.runOnClient(client -> {
                MainGUI screen = (MainGUI) client.screen;
                var groups = screen.getActiveModuleSubTab().groups();
                int halo = -1;
                for (int i = 0; i < groups.size(); i++) {
                    if (groups.get(i).getRawName().equals("Halo")) halo = i;
                }
                if (halo < 0 || groups.get(halo).getSettings().size() != 7) {
                    throw new AssertionError("Halo settings must be available under Fun");
                }
                screen.selectModuleCategory(halo + 1);
            });
            context.waitTicks(10);
            context.takeScreenshot(TestScreenshotOptions.of("halo-settings").withSize(1280, 720));
            context.setScreen(() -> null);

            context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
            assertVisible(context, false);
            context.runOnClient(client -> FreecamManager.setEnabled(true));
            assertVisible(context, true);
            context.runOnClient(client -> {
                FreecamManager.setEnabled(false);
                AetherConfig.STREAMER_MODE.set(true);
                if (CosmeticWorldRenderer.hasVisibleEffects()) {
                    throw new AssertionError("Streamer mode must hide cosmetics");
                }
                AetherConfig.STREAMER_MODE.set(false);
            });
            context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            world.getServer().runCommand("effect give @p invisibility infinite 0 true");
            context.waitFor(client -> client.player.isInvisible());
            assertVisible(context, false);
            world.getServer().runCommand("effect clear @p");
            context.waitFor(client -> !client.player.isInvisible());
            assertVisible(context, true);
            world.getServer().runCommand("gamemode spectator @p");
            context.waitFor(client -> client.player.isSpectator());
            assertVisible(context, false);
            world.getServer().runCommand("gamemode creative @p");
            context.waitFor(client -> !client.player.isSpectator());
            context.runOnClient(client -> AetherConfig.HALO_ENABLED.set(false));
            assertVisible(context, false);
            context.runOnClient(client -> AetherConfig.HALO_ENABLED.set(true));
            assertVisible(context, true);
            System.out.println("AETHER_HALO_TEST: PASS three styles, crouching, wings, settings, cameras, streamer mode, invisibility, spectator and toggles");
        } finally {
            context.runOnClient(client -> {
                client.options.keyShift.setDown(false);
                FreecamManager.setEnabled(false);
                AetherConfig.STREAMER_MODE.set(false);
                AetherConfig.HALO_ENABLED.set(false);
                AetherConfig.DRAGON_WINGS_ENABLED.set(false);
                CosmeticWorldRenderer.close();
            });
        }
    }

    private static void assertVisible(ClientGameTestContext context, boolean expected) {
        context.runOnClient(client -> {
            if (HaloRenderer.visible(client) != expected) {
                throw new AssertionError("Unexpected halo visibility: expected " + expected);
            }
        });
    }
}
