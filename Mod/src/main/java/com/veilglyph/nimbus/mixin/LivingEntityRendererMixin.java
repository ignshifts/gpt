package com.veilglyph.nimbus.mixin;

import com.veilglyph.nimbus.NimbusConfig;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void veilglyph$showInvisiblePlayers(LivingEntityRenderState state, CallbackInfoReturnable<Boolean> cir) {
        if (NimbusConfig.seeInvisibleEnabled && state != null && state.entityType == EntityType.PLAYER) {
            cir.setReturnValue(true);
        }
    }
}
