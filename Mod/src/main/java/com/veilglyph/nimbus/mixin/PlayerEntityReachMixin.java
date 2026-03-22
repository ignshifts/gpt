package com.veilglyph.nimbus.mixin;

import com.veilglyph.nimbus.FreecamHandler;
import com.veilglyph.nimbus.NimbusConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityReachMixin {

    @Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
    private void veilglyph$fakeSpectator(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamHandler.active && (Object) this instanceof ClientPlayerEntity) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void veilglyph$overrideBlockReach(CallbackInfoReturnable<Double> cir) {
        if (NimbusConfig.reachEnabled) {
            cir.setReturnValue((double) NimbusConfig.reachBlocks);
        }
    }

    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void veilglyph$overrideEntityReach(CallbackInfoReturnable<Double> cir) {
        if (NimbusConfig.reachEnabled) {
            cir.setReturnValue((double) NimbusConfig.reachBlocks);
        }
    }
}
