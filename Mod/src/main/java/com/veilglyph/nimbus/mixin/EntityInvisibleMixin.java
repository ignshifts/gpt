package com.veilglyph.nimbus.mixin;

import com.veilglyph.nimbus.NimbusConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityInvisibleMixin {

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void veilglyph$showInvisiblePlayers(CallbackInfoReturnable<Boolean> cir) {
        if (NimbusConfig.seeInvisibleEnabled && (Object) this instanceof PlayerEntity) {
            cir.setReturnValue(false);
        }
    }
}
