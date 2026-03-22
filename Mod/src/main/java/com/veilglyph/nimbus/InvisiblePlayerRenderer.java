package com.veilglyph.nimbus;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class InvisiblePlayerRenderer {

    private static final float[] TRACER_COLOR = {0.85f, 0.25f, 0.90f};

    public static void render(WorldRenderContext context) {
        if (!NimbusConfig.seeInvisibleEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        Camera camera = context.gameRenderer().getCamera();
        Vec3d cam = camera.getPos();
        MatrixStack matrices = context.matrices();

        VertexConsumer line = consumers.getBuffer(RenderLayer.getDebugLineStrip(10.0));

        matrices.push();
        MatrixStack.Entry entry = matrices.peek();

        Box searchBox = Box.from(cam).expand(256);
        for (PlayerEntity player : client.world.getEntitiesByType(EntityType.PLAYER, searchBox, e -> e != client.player)) {
            if (!player.hasStatusEffect(StatusEffects.INVISIBILITY)) continue;

            double px = player.getX();
            double py = player.getY();
            double pz = player.getZ();

            float tx = (float) (px - cam.x);
            float ty = (float) (py + player.getHeight() / 2.0 - cam.y);
            float tz = (float) (pz - cam.z);

            Vector3f dir = new Vector3f(tx, ty, tz);
            if (dir.lengthSquared() > 0.001f) dir.normalize();
            else dir.set(0, 1, 0);

            float r = TRACER_COLOR[0], g = TRACER_COLOR[1], b = TRACER_COLOR[2];

            float off = 0.03f;
            for (int pass = 0; pass < 3; pass++) {
                float ox = pass == 0 ? 0 : (pass == 1 ? off : -off);
                float oy = pass == 0 ? 0 : off;
                float oz = pass == 0 ? 0 : (pass == 2 ? off : 0);
                line.vertex(entry, ox, oy, oz)
                        .color(r, g, b, 1.0f)
                        .normal(entry, dir.x, dir.y, dir.z);
                line.vertex(entry, tx + ox, ty + oy, tz + oz)
                        .color(r, g, b, 1.0f)
                        .normal(entry, dir.x, dir.y, dir.z);
            }

            float hw = player.getWidth() / 2.0f;
            float h = player.getHeight();
            drawPlayerBox(entry, line,
                    (float)(px - cam.x) - hw, (float)(py - cam.y), (float)(pz - cam.z) - hw,
                    hw * 2, h,
                    r, g, b, 0.8f);
        }

        matrices.pop();
    }

    private static void drawPlayerBox(MatrixStack.Entry entry, VertexConsumer consumer,
                                      float x, float y, float z, float w, float h,
                                      float r, float g, float b, float a) {
        float x1 = x + w, y1 = y + h, z1 = z + w;
        float n = 0, one = 1;
        v(consumer, entry, x, y, z, r, g, b, a, n, one, n);
        v(consumer, entry, x1, y, z, r, g, b, a, n, one, n);
        v(consumer, entry, x1, y, z1, r, g, b, a, n, one, n);
        v(consumer, entry, x, y, z1, r, g, b, a, n, one, n);
        v(consumer, entry, x, y, z, r, g, b, a, n, one, n);
        v(consumer, entry, x, y, z, r, g, b, a, n, one, n);
        v(consumer, entry, x, y1, z, r, g, b, a, n, one, n);
        v(consumer, entry, x1, y, z, r, g, b, a, n, one, n);
        v(consumer, entry, x1, y1, z, r, g, b, a, n, one, n);
        v(consumer, entry, x1, y, z1, r, g, b, a, n, one, n);
        v(consumer, entry, x1, y1, z1, r, g, b, a, n, one, n);
        v(consumer, entry, x, y, z1, r, g, b, a, n, one, n);
        v(consumer, entry, x, y1, z1, r, g, b, a, n, one, n);
        v(consumer, entry, x, y1, z1, r, g, b, a, n, one, n);
        v(consumer, entry, x, y1, z, r, g, b, a, n, one, n);
        v(consumer, entry, x1, y1, z, r, g, b, a, n, one, n);
        v(consumer, entry, x1, y1, z1, r, g, b, a, n, one, n);
        v(consumer, entry, x, y1, z1, r, g, b, a, n, one, n);
        v(consumer, entry, x, y1, z, r, g, b, a, n, one, n);
    }

    private static void v(VertexConsumer c, MatrixStack.Entry e,
                          float x, float y, float z, float r, float g, float b, float a,
                          float nx, float ny, float nz) {
        c.vertex(e, x, y, z).color(r, g, b, a).normal(e, nx, ny, nz);
    }
}
