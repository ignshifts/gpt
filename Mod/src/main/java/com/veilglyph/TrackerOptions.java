package com.veilglyph;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TrackerOptions {

    private static final Set<ShulkerBoxTracker.TrackedType> ENABLED = ConcurrentHashMap.newKeySet();

    /** Default: only shulker boxes and ender chests to avoid lag. */
    private static final Set<ShulkerBoxTracker.TrackedType> DEFAULT_ENABLED = EnumSet.of(
            ShulkerBoxTracker.TrackedType.SHULKER_BOX,
            ShulkerBoxTracker.TrackedType.ENDER_CHEST
    );

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("veilglyph_tracked.txt");
    }

    public static boolean isEnabled(ShulkerBoxTracker.TrackedType type) {
        if (ENABLED.isEmpty()) {
            synchronized (TrackerOptions.class) {
                if (ENABLED.isEmpty()) load();
            }
        }
        return ENABLED.contains(type);
    }

    public static void setEnabled(ShulkerBoxTracker.TrackedType type, boolean enabled) {
        if (enabled) ENABLED.add(type);
        else ENABLED.remove(type);
    }

    public static boolean toggle(ShulkerBoxTracker.TrackedType type) {
        if (ENABLED.contains(type)) {
            ENABLED.remove(type);
            return false;
        } else {
            ENABLED.add(type);
            return true;
        }
    }

    public static void load() {
        ENABLED.clear();
        ENABLED.addAll(DEFAULT_ENABLED);
        Path path = getConfigPath();
        if (!Files.exists(path)) return;
        try {
            for (String line : Files.readAllLines(path)) {
                String name = line.trim();
                if (name.isEmpty() || name.startsWith("#")) continue;
                try {
                    ENABLED.add(ShulkerBoxTracker.TrackedType.valueOf(name));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    public static void save() {
        Path path = getConfigPath();
        try {
            StringBuilder sb = new StringBuilder();
            for (ShulkerBoxTracker.TrackedType t : ShulkerBoxTracker.TrackedType.values()) {
                if (ENABLED.contains(t)) sb.append(t.name()).append('\n');
            }
            Files.writeString(path, sb.toString());
        } catch (IOException ignored) {}
    }
}
