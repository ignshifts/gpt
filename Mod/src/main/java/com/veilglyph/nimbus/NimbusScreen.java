package com.veilglyph.nimbus;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class NimbusScreen extends Screen {

    private final Screen parent;

    public NimbusScreen(Screen parent) {
        super(Text.literal("Nimbus"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int y = 50;

        addDrawableChild(ButtonWidget.builder(
                Text.literal((NimbusConfig.reachEnabled ? "\u00a7a[ON] " : "\u00a7c[OFF] ") + "Extended Reach"),
                b -> {
                    NimbusConfig.reachEnabled = !NimbusConfig.reachEnabled;
                    b.setMessage(Text.literal((NimbusConfig.reachEnabled ? "\u00a7a[ON] " : "\u00a7c[OFF] ") + "Extended Reach"));
                }
        ).dimensions(centerX - 100, y, 200, 20).build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
            NimbusConfig.reachBlocks = Math.max(3f, NimbusConfig.reachBlocks - 1f);
            clearAndInit();
        }).dimensions(centerX - 100, y, 30, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            NimbusConfig.reachBlocks = Math.min(20f, NimbusConfig.reachBlocks + 1f);
            clearAndInit();
        }).dimensions(centerX + 70, y, 30, 20).build());
        y += 30;

        addDrawableChild(ButtonWidget.builder(
                Text.literal((NimbusConfig.seeInvisibleEnabled ? "\u00a7a[ON] " : "\u00a7c[OFF] ") + "See Invisible Players"),
                b -> {
                    NimbusConfig.seeInvisibleEnabled = !NimbusConfig.seeInvisibleEnabled;
                    b.setMessage(Text.literal((NimbusConfig.seeInvisibleEnabled ? "\u00a7a[ON] " : "\u00a7c[OFF] ") + "See Invisible Players"));
                }
        ).dimensions(centerX - 100, y, 200, 20).build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(
                Text.literal((FreecamHandler.active ? "\u00a7a[ON] " : "\u00a7c[OFF] ") + "Freecam (Spectator View)"),
                b -> {
                    FreecamHandler.toggle();
                    b.setMessage(Text.literal((FreecamHandler.active ? "\u00a7a[ON] " : "\u00a7c[OFF] ") + "Freecam (Spectator View)"));
                }
        ).dimensions(centerX - 100, y, 200, 20).build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(
                Text.literal((NimbusConfig.trackerUseFreecamPosition ? "\u00a7a[ON] " : "\u00a7c[OFF] ") + "Block finder uses freecam position"),
                b -> {
                    NimbusConfig.trackerUseFreecamPosition = !NimbusConfig.trackerUseFreecamPosition;
                    b.setMessage(Text.literal((NimbusConfig.trackerUseFreecamPosition ? "\u00a7a[ON] " : "\u00a7c[OFF] ") + "Block finder uses freecam position"));
                    NimbusConfig.save();
                }
        ).dimensions(centerX - 100, y, 200, 20).build());
        y += 40;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
            NimbusConfig.save();
            if (client != null) client.setScreen(parent);
        }).dimensions(centerX - 50, y, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0101010);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Reach: \u00a7e%.1f blocks".formatted(NimbusConfig.reachBlocks)),
                width / 2, 78, 0xFFAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        NimbusConfig.save();
        super.close();
    }
}
