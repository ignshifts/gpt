package com.veilglyph;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TrackerOptionsScreen extends Screen {

    private final Screen parent;
    private static final int CHECKBOX_WIDTH = 200;
    private static final int ROW = 22;
    private static final int HEADER_HEIGHT = 40;
    private static final int FOOTER_HEIGHT = 34;

    private final List<ButtonWidget> typeButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public TrackerOptionsScreen(Screen parent) {
        super(Text.literal("Tracked types"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        typeButtons.clear();
        int centerX = width / 2;

        ShulkerBoxTracker.TrackedType[] types = ShulkerBoxTracker.TrackedType.values();
        int totalContentHeight = types.length * ROW;
        int visibleHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
        maxScroll = Math.max(0, totalContentHeight - visibleHeight);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        for (ShulkerBoxTracker.TrackedType type : types) {
            String label = typeLabel(type);
            int x = centerX - CHECKBOX_WIDTH / 2;
            ButtonWidget btn = ButtonWidget.builder(
                    Text.literal((TrackerOptions.isEnabled(type) ? "\u00a7a[ON] " : "\u00a7c[OFF] ") + label),
                    b -> {
                        TrackerOptions.toggle(type);
                        b.setMessage(Text.literal((TrackerOptions.isEnabled(type) ? "\u00a7a[ON] " : "\u00a7c[OFF] ") + label));
                        TrackerOptions.save();
                    }
            ).dimensions(x, 0, CHECKBOX_WIDTH, 20).build();
            typeButtons.add(btn);
            addDrawableChild(btn);
        }

        repositionButtons();

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
            if (client != null) client.setScreen(parent);
        }).dimensions(centerX - 50, height - 28, 100, 20).build());
    }

    private void repositionButtons() {
        int y = HEADER_HEIGHT - scrollOffset;
        for (ButtonWidget btn : typeButtons) {
            btn.setY(y);
            btn.visible = (y + 20) > HEADER_HEIGHT && y < (height - FOOTER_HEIGHT);
            y += ROW;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int oldOffset = scrollOffset;
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * ROW)));
        if (scrollOffset != oldOffset) {
            repositionButtons();
        }
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0101010);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Toggle which types are tracked (reduces lag)"), width / 2, 24, 0xFFAAAAAA);

        context.enableScissor(0, HEADER_HEIGHT, width, height - FOOTER_HEIGHT);
        super.render(context, mouseX, mouseY, delta);
        context.disableScissor();

        if (maxScroll > 0) {
            int trackX = width / 2 + CHECKBOX_WIDTH / 2 + 8;
            int trackTop = HEADER_HEIGHT;
            int trackBottom = height - FOOTER_HEIGHT;
            int trackHeight = trackBottom - trackTop;
            context.fill(trackX, trackTop, trackX + 4, trackBottom, 0x40FFFFFF);

            float ratio = (float) scrollOffset / maxScroll;
            int thumbHeight = Math.max(20, trackHeight * trackHeight / (trackHeight + maxScroll));
            int thumbY = trackTop + (int) ((trackHeight - thumbHeight) * ratio);
            context.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0xA0FFFFFF);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static String typeLabel(ShulkerBoxTracker.TrackedType type) {
        if (type == ShulkerBoxTracker.TrackedType.SHULKER_BOX) return "Shulker Box";
        return switch (type) {
            case ENDER_CHEST -> "Ender Chest";
            case PISTON -> "Piston";
            case STICKY_PISTON -> "Sticky Piston";
            case HOPPER -> "Hopper";
            case DISPENSER -> "Dispenser";
            case BARREL -> "Barrel";
            case CHEST -> "Chest";
            case SIGN -> "Sign";
            case ITEM_FRAME -> "Item Frame";
            case SPAWNER -> "Monster Spawner";
            case VILLAGER -> "Villager";
            case ANCIENT_DEBRIS -> "Ancient Debris";
            case GILDED_BLACKSTONE -> "Gilded Blackstone";
            case ENCHANTING_TABLE -> "Enchanting Table";
            case NETHERITE_BLOCK -> "Block of Netherite";
            case DIAMOND_BLOCK -> "Block of Diamond";
            case CONDUIT -> "Conduit";
            case PIGLIN -> "Piglin";
            case END_PORTAL_FRAME -> "End Portal Frame";
            case END_PORTAL -> "End Portal";
            case DEEPSLATE_EMERALD_ORE -> "Deepslate Emerald Ore";
            case EMERALD_ORE -> "Emerald Ore";
            case IRON_ORE -> "Iron Ore";
            case DEEPSLATE_IRON_ORE -> "Deepslate Iron Ore";
            case DIAMOND_ORE -> "Diamond Ore";
            case DEEPSLATE_DIAMOND_ORE -> "Deepslate Diamond Ore";
            case GOLD_ORE -> "Gold Ore";
            case DEEPSLATE_GOLD_ORE -> "Deepslate Gold Ore";
            case REDSTONE_ORE -> "Redstone Ore";
            case DEEPSLATE_REDSTONE_ORE -> "Deepslate Redstone Ore";
            default -> type.name();
        };
    }
}
