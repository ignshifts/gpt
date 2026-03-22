package com.veilglyph.nimbus;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class FreecamHandler {

    public static boolean active = false;
    private static double camX, camY, camZ;
    private static float camYaw, camPitch;
    private static Vec3d frozenPlayerPos;

    private static final float BASE_SPEED = 0.5f;

    public static void toggle() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        active = !active;
        if (active) {
            camX = client.player.getX();
            camY = client.player.getEyeY();
            camZ = client.player.getZ();
            camYaw = client.player.getYaw();
            camPitch = client.player.getPitch();
            frozenPlayerPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
        } else {
            frozenPlayerPos = null;
        }
    }

    public static void tick(MinecraftClient client) {
        if (!active) return;
        if (client.player == null || client.world == null) {
            deactivate();
            return;
        }
        if (client.currentScreen != null) return;

        camYaw = client.player.getYaw();
        camPitch = client.player.getPitch();

        float forward = 0, strafe = 0, vertical = 0;
        if (client.options.forwardKey.isPressed()) forward += 1;
        if (client.options.backKey.isPressed()) forward -= 1;
        if (client.options.leftKey.isPressed()) strafe += 1;
        if (client.options.rightKey.isPressed()) strafe -= 1;
        if (client.options.jumpKey.isPressed()) vertical += 1;
        if (client.options.sneakKey.isPressed()) vertical -= 1;

        if (forward == 0 && strafe == 0 && vertical == 0) return;

        float speed = BASE_SPEED;
        if (client.options.sprintKey.isPressed()) speed *= 3;

        double yawRad = Math.toRadians(camYaw);
        double pitchRad = Math.toRadians(camPitch);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        double sinYaw = Math.sin(yawRad);
        double cosYaw = Math.cos(yawRad);

        camX += (-sinYaw * cosPitch * forward + cosYaw * strafe) * speed;
        camY += (-sinPitch * forward + vertical) * speed;
        camZ += (cosYaw * cosPitch * forward + sinYaw * strafe) * speed;
    }

    public static Vec3d getCamPos() {
        return new Vec3d(camX, camY, camZ);
    }

    public static float getCamYaw() { return camYaw; }
    public static float getCamPitch() { return camPitch; }
    public static Vec3d getFrozenPlayerPos() { return frozenPlayerPos; }

    public static void deactivate() {
        active = false;
        frozenPlayerPos = null;
    }
}
