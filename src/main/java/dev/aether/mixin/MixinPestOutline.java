package dev.aether.mixin;

import dev.aether.bootstrap.AetherBootstrapHooks;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinPestOutline {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void aether$pestOutline(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        int color = AetherBootstrapHooks.pestOutlineColor(entity);
        if (color != 0) state.outlineColor = color;
    }
}
