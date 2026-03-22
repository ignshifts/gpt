package com.veilglyph;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VeilglyphMainScreen extends Screen {

    private List<ShulkerBoxTracker.TrackedEntry> entries;
    private static final int LIST_START_X = -150;
    private static final int LIST_WIDTH = 308;
    private static final int ROW_HEIGHT = 16;
    private static final int LIST_HEADER_Y = 72;
    private static final int LIST_CONTENT_START_Y = LIST_HEADER_Y + 12 + 4;
    private static final int FOOTER_RESERVED = 24;

    private int listScrollOffset = 0;

    public VeilglyphMainScreen() {
        super(Text.literal("Storage & Block Finder"));
    }

    @Override
    protected void init() {
        super.init();
        if (!VeilglyphMod.modEnabled) {
            entries = new ArrayList<>();
            addDrawableChild(ButtonWidget.builder(Text.literal("Enable VeilGlyph"), b -> {
                VeilglyphMod.modEnabled = true;
                if (client != null) client.setScreen(null);
            }).dimensions(width / 2 - 100, height / 2 - 20, 200, 20).build());
            return;
        }
        entries = new ArrayList<>(ShulkerBoxTracker.tracked);
        entries.sort(Comparator.comparingDouble(ShulkerBoxTracker.TrackedEntry::distance));
        listScrollOffset = Math.min(listScrollOffset, Math.max(0, entries.size() - getVisibleRowCount()));
        addDrawableChild(ButtonWidget.builder(Text.literal("Filter types..."), b -> {
            if (client != null) client.setScreen(new TrackerOptionsScreen(this));
        }).dimensions(width / 2 - 50, 42, 100, 20).build());
    }

    private int getVisibleRowCount() {
        int listHeight = height - LIST_CONTENT_START_Y - FOOTER_RESERVED;
        return Math.max(0, listHeight / ROW_HEIGHT);
    }

    private int getMaxScrollOffset() {
        if (entries == null || entries.isEmpty()) return 0;
        return Math.max(0, entries.size() - getVisibleRowCount());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = getMaxScrollOffset();
        if (maxScroll <= 0) return false;
        int old = listScrollOffset;
        listScrollOffset = Math.max(0, Math.min(maxScroll, listScrollOffset - (int) Math.signum(verticalAmount) * 3));
        return listScrollOffset != old;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && entries != null && !entries.isEmpty()) {
            double mouseX = click.x();
            double mouseY = click.y();
            int startX = width / 2 + LIST_START_X;
            if (mouseX >= startX - 4 && mouseX <= startX + LIST_WIDTH && mouseY >= LIST_CONTENT_START_Y && mouseY < height - FOOTER_RESERVED) {
                int rowInView = (int) ((mouseY - LIST_CONTENT_START_Y) / ROW_HEIGHT);
                int index = listScrollOffset + rowInView;
                if (index >= 0 && index < entries.size()) {
                    BlockPos pos = entries.get(index).pos();
                    String coords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
                    if (client != null && client.keyboard != null) {
                        client.keyboard.setClipboard(coords);
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("Copied: " + coords), false);
                        }
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0101010);

        if (!VeilglyphMod.modEnabled) {
            context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a7cMod disabled — no scanning or tracers"), width / 2, height / 2 - 40, 0xFFAAAAAA);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Press \u00a7eL\u00a7r to enable, or click the button below"), width / 2, height / 2 - 28, 0xFF888888);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Locations (coordinates) — nearest first • Click row to copy coords"), width / 2, 22, 0xFFB0B0B0);

        String status = VeilglyphMod.tracersEnabled ? "\u00a7aTracers ON (visible through walls)" : "\u00a7cTracers OFF";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, 34, 0xFFAAAAAA);

        if (entries.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("No tracked blocks found in loaded chunks"), width / 2, height / 2 - 4, 0xFF888888);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        int startX = width / 2 + LIST_START_X;
        int listBottom = height - FOOTER_RESERVED;

        context.drawTextWithShadow(textRenderer, Text.literal("Type"), startX, LIST_HEADER_Y, 0xFF999999);
        context.drawTextWithShadow(textRenderer, Text.literal("Coordinates (X, Y, Z)"), startX + 90, LIST_HEADER_Y, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Distance"), startX + 255, LIST_HEADER_Y, 0xFF999999);

        int y = LIST_CONTENT_START_Y;
        context.fill(startX, y, startX + 300, y + 1, 0x40FFFFFF);
        y += 4;

        listScrollOffset = Math.min(listScrollOffset, getMaxScrollOffset());
        int visibleRows = getVisibleRowCount();

        context.enableScissor(0, LIST_CONTENT_START_Y, width, listBottom);
        for (int i = 0; i < visibleRows; i++) {
            int index = listScrollOffset + i;
            if (index >= entries.size()) break;
            ShulkerBoxTracker.TrackedEntry entry = entries.get(index);
            BlockPos pos = entry.pos();
            int rowY = y + i * ROW_HEIGHT;

            if (index % 2 == 0) {
                context.fill(startX - 4, rowY - 2, startX + 304, rowY + 12, 0x20FFFFFF);
            }

            int colorInt = getColorInt(entry.type(), entry.shulkerColor());
            context.fill(startX, rowY, startX + 10, rowY + 10, colorInt | 0xFF000000);

            String typeName = getTypeDisplayName(entry.type(), entry.shulkerColor());
            context.drawTextWithShadow(textRenderer, Text.literal(typeName), startX + 14, rowY + 1, 0xFFDDDDDD);

            String coords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            context.drawTextWithShadow(textRenderer, Text.literal(coords), startX + 100, rowY + 1, 0xFFFFFFFF);

            double dist = entry.distance();
            int distColor = dist < 16 ? 0xFF55FF55 : (dist < 64 ? 0xFFFFFF55 : 0xFFFF8855);
            context.drawTextWithShadow(textRenderer, Text.literal(String.format("%.1f m", dist)), startX + 250, rowY + 1, distColor);
        }
        context.disableScissor();

        int maxScroll = getMaxScrollOffset();
        if (maxScroll > 0) {
            int trackX = width / 2 + LIST_START_X + LIST_WIDTH + 4;
            int trackTop = LIST_CONTENT_START_Y;
            int trackBottom = listBottom;
            int trackHeight = trackBottom - trackTop;
            int totalContentHeight = entries.size() * ROW_HEIGHT;
            context.fill(trackX, trackTop, trackX + 4, trackBottom, 0x40FFFFFF);
            float ratio = (float) listScrollOffset / maxScroll;
            int thumbHeight = Math.max(20, totalContentHeight <= 0 ? trackHeight : trackHeight * trackHeight / totalContentHeight);
            int thumbY = trackTop + (int) ((trackHeight - thumbHeight) * ratio);
            context.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0xA0FFFFFF);
        }

        int footerY = height - 16;
        String total = entries.size() + " location" + (entries.size() != 1 ? "s" : "") + " — Scroll to see all, click row to copy";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(total), width / 2, footerY, 0xFF777777);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static String getTypeDisplayName(ShulkerBoxTracker.TrackedType type, DyeColor shulkerColor) {
        if (type == ShulkerBoxTracker.TrackedType.SHULKER_BOX) {
            return shulkerColor == null ? "Shulker (Default)" : "Shulker (" + capitalize(shulkerColor.name()) + ")";
        }
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

    private static int getColorInt(ShulkerBoxTracker.TrackedType type, DyeColor color) {
        if (type == ShulkerBoxTracker.TrackedType.SHULKER_BOX) {
            if (color == null) return 0x9B59B6;
            return switch (color) {
                case WHITE -> 0xF9FFFE;
                case ORANGE -> 0xF9801D;
                case MAGENTA -> 0xC74EBD;
                case LIGHT_BLUE -> 0x3AB3DA;
                case YELLOW -> 0xFED83D;
                case LIME -> 0x80C71F;
                case PINK -> 0xF38BAA;
                case GRAY -> 0x474F52;
                case LIGHT_GRAY -> 0x9D9D97;
                case CYAN -> 0x169C9C;
                case PURPLE -> 0x8932B8;
                case BLUE -> 0x3C44AA;
                case BROWN -> 0x835432;
                case GREEN -> 0x5E7C16;
                case RED -> 0xB02E26;
                case BLACK -> 0x1D1D21;
            };
        }
        return switch (type) {
            case ENDER_CHEST -> 0x542B82;
            case PISTON, STICKY_PISTON -> 0x888888;
            case HOPPER -> 0x5A5A5E;
            case DISPENSER -> 0x8B7355;
            case BARREL -> 0x73583F;
            case CHEST -> 0xA67C3D;
            case SIGN -> 0x73583F;
            case ITEM_FRAME -> 0xB38C55;
            case SPAWNER -> 0x402060;
            case VILLAGER -> 0xE6B34D;
            case ANCIENT_DEBRIS -> 0x663322;
            case GILDED_BLACKSTONE -> 0x594938;
            case ENCHANTING_TABLE -> 0x402670;
            case NETHERITE_BLOCK -> 0x38302E;
            case DIAMOND_BLOCK -> 0x408CD9;
            case CONDUIT -> 0x338CB3;
            case PIGLIN -> 0xF2A64B;
            case END_PORTAL_FRAME -> 0x1FA07A;
            case END_PORTAL -> 0x0F0F2E;
            case DEEPSLATE_EMERALD_ORE -> 0x408C55;
            case EMERALD_ORE -> 0x4DA64D;
            case IRON_ORE -> 0xA68C7F;
            case DEEPSLATE_IRON_ORE -> 0x726B6B;
            case DIAMOND_ORE -> 0x408CD9;
            case DEEPSLATE_DIAMOND_ORE -> 0x387AB8;
            case GOLD_ORE -> 0xD9A63D;
            case DEEPSLATE_GOLD_ORE -> 0xB39559;
            case REDSTONE_ORE -> 0xC04033;
            case DEEPSLATE_REDSTONE_ORE -> 0x8C382E;
            default -> 0x9B59B6;
        };
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String lower = s.toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : lower.toCharArray()) {
            if (c == ' ') {
                sb.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
