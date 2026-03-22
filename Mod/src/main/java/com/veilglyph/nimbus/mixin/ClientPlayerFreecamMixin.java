package com.veilglyph.nimbus.mixin;

import com.veilglyph.nimbus.FreecamHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerFreecamMixin {

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void veilglyph$blockMovementPackets(CallbackInfo ci) {
        if (FreecamHandler.active) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void veilglyph$freezeBeforeTick(CallbackInfo ci) {
        if (FreecamHandler.active) {
            Vec3d frozen = FreecamHandler.getFrozenPlayerPos();
            if (frozen != null) {
                ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
                self.setPosition(frozen);
                self.setVelocity(Vec3d.ZERO);
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void veilglyph$freezeAfterTick(CallbackInfo ci) {
        if (FreecamHandler.active) {
            Vec3d frozen = FreecamHandler.getFrozenPlayerPos();
            if (frozen != null) {
                ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
                self.setPosition(frozen);
                self.setVelocity(Vec3d.ZERO);
            }
        }
    }
}
