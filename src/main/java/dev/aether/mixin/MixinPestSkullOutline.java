package dev.aether.mixin;

import dev.aether.bootstrap.AetherBootstrapHooks;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStandRenderer.class)
public class MixinPestSkullOutline {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ArmorStand;Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;F)V",
            at = @At("TAIL"))
    private void aether$hideOutlineStand(ArmorStand entity, ArmorStandRenderState state, float partialTick, CallbackInfo ci) {
        // Only the equipped skull is the pest; the invisible support stand must not acquire a silhouette.
        if (entity.isInvisible() && AetherBootstrapHooks.pestOutlineColor(entity) != 0) state.isMarker = true;
    }
}
