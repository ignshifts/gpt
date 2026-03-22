package com.veilglyph;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.List;

public class ShulkerBoxRenderer {

    public static void render(WorldRenderContext context) {
        if (!VeilglyphMod.modEnabled || !VeilglyphMod.tracersEnabled) return;

        List<ShulkerBoxTracker.TrackedEntry> entries = ShulkerBoxTracker.tracked;
        if (entries.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        Camera camera = context.gameRenderer().getCamera();
        Vec3d cam = camera.getPos();
        MatrixStack matrices = context.matrices();

        // Max line width; draw same line 3x with tiny offset for extra thickness
        VertexConsumer debugConsumer = consumers.getBuffer(RenderLayer.getDebugLineStrip(10.0));

        matrices.push();
        MatrixStack.Entry entry = matrices.peek();

        for (ShulkerBoxTracker.TrackedEntry item : entries) {
            float[] c = getColor(item.type(), item.shulkerColor());

            float tx = (float) (item.pos().getX() + 0.5 - cam.x);
            float ty = (float) (item.pos().getY() + 0.5 - cam.y);
            float tz = (float) (item.pos().getZ() + 0.5 - cam.z);

            Vector3f dir = new Vector3f(tx, ty, tz);
            if (dir.lengthSquared() > 0.001f) dir.normalize();
            else dir.set(0, 1, 0);

            // Draw tracer 3 times with small perpendicular offsets so it appears much thicker
            float off = 0.03f;
            for (int pass = 0; pass < 3; pass++) {
                float ox = pass == 0 ? 0 : (pass == 1 ? off : -off);
                float oy = pass == 0 ? 0 : off;
                float oz = pass == 0 ? 0 : (pass == 2 ? off : 0);
                debugConsumer.vertex(entry, ox, oy, oz)
                        .color(c[0], c[1], c[2], 1.0f)
                        .normal(entry, dir.x, dir.y, dir.z);
                debugConsumer.vertex(entry, tx + ox, ty + oy, tz + oz)
                        .color(c[0], c[1], c[2], 1.0f)
                        .normal(entry, dir.x, dir.y, dir.z);
            }

            double bx = item.pos().getX() - cam.x;
            double by = item.pos().getY() - cam.y;
            double bz = item.pos().getZ() - cam.z;
            drawBoxLineStrip(entry, debugConsumer, bx, by, bz, c[0], c[1], c[2], 1.0f);
        }

        matrices.pop();
    }

    private static void drawBoxLineStrip(MatrixStack.Entry entry, VertexConsumer consumer,
                                         double bx, double by, double bz,
                                         float r, float g, float b, float a) {
        float x0 = (float) bx, x1 = (float) (bx + 1);
        float y0 = (float) by, y1 = (float) (by + 1);
        float z0 = (float) bz, z1 = (float) (bz + 1);
        float n = 0, one = 1;
        vertex(consumer, entry, x0, y0, z0, r, g, b, a, n, one, n);
        vertex(consumer, entry, x1, y0, z0, r, g, b, a, n, one, n);
        vertex(consumer, entry, x1, y0, z1, r, g, b, a, n, one, n);
        vertex(consumer, entry, x0, y0, z1, r, g, b, a, n, one, n);
        vertex(consumer, entry, x0, y0, z0, r, g, b, a, n, one, n);
        vertex(consumer, entry, x0, y0, z0, r, g, b, a, n, one, n);
        vertex(consumer, entry, x0, y1, z0, r, g, b, a, n, one, n);
        vertex(consumer, entry, x1, y0, z0, r, g, b, a, n, one, n);
        vertex(consumer, entry, x1, y1, z0, r, g, b, a, n, one, n);
        vertex(consumer, entry, x1, y0, z1, r, g, b, a, n, one, n);
        vertex(consumer, entry, x1, y1, z1, r, g, b, a, n, one, n);
        vertex(consumer, entry, x0, y0, z1, r, g, b, a, n, one, n);
        vertex(consumer, entry, x0, y1, z1, r, g, b, a, n, one, n);
        vertex(consumer, entry, x0, y1, z1, r, g, b, a, n, one, n);
        vertex(consumer, entry, x0, y1, z0, r, g, b, a, n, one, n);
        vertex(consumer, entry, x1, y1, z0, r, g, b, a, n, one, n);
        vertex(consumer, entry, x1, y1, z1, r, g, b, a, n, one, n);
        vertex(consumer, entry, x0, y1, z1, r, g, b, a, n, one, n);
        vertex(consumer, entry, x0, y1, z0, r, g, b, a, n, one, n);
    }

    private static void vertex(VertexConsumer consumer, MatrixStack.Entry entry,
                               float x, float y, float z, float r, float g, float b, float a,
                               float nx, float ny, float nz) {
        consumer.vertex(entry, x, y, z).color(r, g, b, a).normal(entry, nx, ny, nz);
    }

    private static float[] getColor(ShulkerBoxTracker.TrackedType type, DyeColor shulkerColor) {
        if (type == ShulkerBoxTracker.TrackedType.SHULKER_BOX) {
            if (shulkerColor == null) return new float[]{0.61f, 0.35f, 0.71f};
            return switch (shulkerColor) {
                case WHITE -> new float[]{0.98f, 1.0f, 1.0f};
                case ORANGE -> new float[]{0.98f, 0.50f, 0.11f};
                case MAGENTA -> new float[]{0.78f, 0.31f, 0.74f};
                case LIGHT_BLUE -> new float[]{0.23f, 0.70f, 0.85f};
                case YELLOW -> new float[]{1.0f, 0.85f, 0.24f};
                case LIME -> new float[]{0.50f, 0.78f, 0.12f};
                case PINK -> new float[]{0.95f, 0.55f, 0.67f};
                case GRAY -> new float[]{0.28f, 0.31f, 0.32f};
                case LIGHT_GRAY -> new float[]{0.62f, 0.62f, 0.59f};
                case CYAN -> new float[]{0.09f, 0.61f, 0.61f};
                case PURPLE -> new float[]{0.54f, 0.20f, 0.72f};
                case BLUE -> new float[]{0.24f, 0.27f, 0.67f};
                case BROWN -> new float[]{0.51f, 0.33f, 0.20f};
                case GREEN -> new float[]{0.37f, 0.49f, 0.09f};
                case RED -> new float[]{0.69f, 0.18f, 0.15f};
                case BLACK -> new float[]{0.11f, 0.11f, 0.13f};
            };
        }
        return switch (type) {
            case ENDER_CHEST -> new float[]{0.33f, 0.20f, 0.45f};
            case PISTON, STICKY_PISTON -> new float[]{0.55f, 0.55f, 0.55f};
            case HOPPER -> new float[]{0.35f, 0.35f, 0.38f};
            case DISPENSER -> new float[]{0.55f, 0.45f, 0.35f};
            case BARREL -> new float[]{0.45f, 0.35f, 0.25f};
            case CHEST -> new float[]{0.65f, 0.50f, 0.25f};
            case SIGN -> new float[]{0.45f, 0.35f, 0.25f};
            case ITEM_FRAME -> new float[]{0.70f, 0.55f, 0.35f};
            case SPAWNER -> new float[]{0.25f, 0.15f, 0.30f};
            case VILLAGER -> new float[]{0.90f, 0.70f, 0.45f};
            case ANCIENT_DEBRIS -> new float[]{0.40f, 0.25f, 0.20f};
            case GILDED_BLACKSTONE -> new float[]{0.35f, 0.28f, 0.22f};
            case ENCHANTING_TABLE -> new float[]{0.25f, 0.15f, 0.45f};
            case NETHERITE_BLOCK -> new float[]{0.22f, 0.18f, 0.18f};
            case DIAMOND_BLOCK -> new float[]{0.25f, 0.55f, 0.85f};
            case CONDUIT -> new float[]{0.20f, 0.55f, 0.70f};
            case PIGLIN -> new float[]{0.95f, 0.65f, 0.35f};
            case DEEPSLATE_EMERALD_ORE -> new float[]{0.25f, 0.55f, 0.35f};
            case EMERALD_ORE -> new float[]{0.30f, 0.65f, 0.40f};
            case IRON_ORE -> new float[]{0.65f, 0.55f, 0.50f};
            case DEEPSLATE_IRON_ORE -> new float[]{0.45f, 0.42f, 0.42f};
            case DIAMOND_ORE -> new float[]{0.25f, 0.55f, 0.85f};
            case DEEPSLATE_DIAMOND_ORE -> new float[]{0.22f, 0.48f, 0.72f};
            case GOLD_ORE -> new float[]{0.85f, 0.65f, 0.25f};
            case DEEPSLATE_GOLD_ORE -> new float[]{0.70f, 0.58f, 0.35f};
            case REDSTONE_ORE -> new float[]{0.75f, 0.25f, 0.20f};
            case DEEPSLATE_REDSTONE_ORE -> new float[]{0.55f, 0.22f, 0.18f};
            default -> new float[]{0.61f, 0.35f, 0.71f};
        };
    }
}
