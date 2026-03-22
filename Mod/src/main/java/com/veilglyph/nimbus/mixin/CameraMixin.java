package com.veilglyph.nimbus.mixin;

import com.veilglyph.nimbus.FreecamHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow protected abstract void setPos(Vec3d pos);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void veilglyph$freecamOverride(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (FreecamHandler.active) {
            setPos(FreecamHandler.getCamPos());
            setRotation(FreecamHandler.getCamYaw(), FreecamHandler.getCamPitch());
        }
    }
}
