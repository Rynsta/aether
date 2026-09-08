package dev.aether.ui;

import dev.aether.config.AetherConfig;
import dev.aether.renderer.AetherCape;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.player.PlayerModelPart;

public final class CapeClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(true)
                .adjustSettings(settings -> {
                    settings.setName("Aether cape regressions");
                    settings.setAllowCommands(true);
                }).create()) {
            world.getClientLevel().waitForChunksDownload();
            world.getServer().runCommand("gamemode creative @p");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("fill -12 100 -12 12 100 12 stone_bricks");
            world.getServer().runCommand("tp @p 0 101 0 180 12");
            context.runOnClient(client -> {
                if (!AetherConfig.CAPE_ENABLED.getDefault() || AetherConfig.HALO_GLOW.getDefault() != 1f
                        || AetherConfig.HALO_TILT.getDefault() != 0f) {
                    throw new AssertionError("Unexpected cape or halo defaults");
                }
                AetherConfig.CAPE_ENABLED.reset();
                AetherConfig.HAT_ENABLED.set(false);
                AetherConfig.HALO_ENABLED.set(false);
                AetherConfig.DRAGON_WINGS_ENABLED.set(false);
                client.options.hideGui = true;
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                client.options.setModelPart(PlayerModelPart.CAPE, true);
                var original = client.player.getSkin().cape();
                int id = client.player.getId();
                if (AetherCape.textureFor(id, original) != AetherCape.TEXTURE
                        || AetherCape.textureFor(id + 1, original) != original) {
                    throw new AssertionError("Only the local player's cape should be replaced");
                }
                AetherConfig.CAPE_ENABLED.set(false);
                if (AetherCape.textureFor(id, original) != original) {
                    throw new AssertionError("Disabling the cape must restore the original texture");
                }
                AetherConfig.CAPE_ENABLED.set(true);
                AetherConfig.STREAMER_MODE.set(true);
                if (AetherCape.textureFor(id, original) != original) {
                    throw new AssertionError("Streamer mode must restore the original cape");
                }
                AetherConfig.STREAMER_MODE.set(false);
                if (client.player.getSkin().cape() != original) {
                    throw new AssertionError("The cape override must not alter player skin or elytra textures");
                }
            });
            context.waitTicks(10);
            context.takeScreenshot("aether-cape-standing");
            context.runOnClient(client -> client.options.keyUp.setDown(true));
            context.waitTicks(12);
            context.takeScreenshot("aether-cape-walking");
            context.runOnClient(client -> {
                client.options.keyUp.setDown(false);
                client.options.keyShift.setDown(true);
            });
            context.waitFor(client -> client.player.isCrouching());
            context.takeScreenshot("aether-cape-crouching");
            context.runOnClient(client -> {
                client.options.keyShift.setDown(false);
                client.options.setModelPart(PlayerModelPart.CAPE, false);
            });
            context.waitTicks(5);
            context.takeScreenshot("aether-cape-hidden-by-skin-settings");
            context.runOnClient(client -> client.options.setModelPart(PlayerModelPart.CAPE, true));
            world.getServer().runCommand("item replace entity @p armor.chest with minecraft:elytra");
            context.waitTicks(5);
            context.takeScreenshot("aether-cape-elytra");
            world.getServer().runCommand("item replace entity @p armor.chest with minecraft:air");
            context.runOnClient(client -> {
                client.options.hideGui = false;
                AetherConfig.HALO_ENABLED.set(true);
                AetherConfig.HALO_GLOW.reset();
                AetherConfig.HALO_TILT.reset();
            });
            context.setScreen(() -> new MainGUI(new MainGUI.LaunchTarget(0, "Fun", true)));
            context.runOnClient(client -> {
                MainGUI screen = (MainGUI) client.screen;
                var groups = screen.getActiveModuleSubTab().groups();
                for (int i = 0; i < groups.size(); i++) {
                    if (groups.get(i).getRawName().equals("Aether Cape")) {
                        screen.selectModuleCategory(i + 1);
                        return;
                    }
                }
                throw new AssertionError("Aether Cape must be available under Fun");
            });
            context.waitTicks(5);
            context.takeScreenshot(TestScreenshotOptions.of("aether-cape-settings").withSize(1280, 720));
            context.setScreen(() -> null);
            System.out.println("AETHER_CAPE_TEST: PASS defaults, local player, toggles, streamer mode, skin preservation, movement, skin settings and elytra");
        } finally {
            context.runOnClient(client -> {
                client.options.keyUp.setDown(false);
                client.options.keyShift.setDown(false);
                AetherConfig.STREAMER_MODE.set(false);
            });
        }
    }
}
