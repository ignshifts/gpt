package com.veilglyph.nimbus;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NimbusConfig {

    public static boolean reachEnabled = false;
    public static float reachBlocks = 6.0f;
    public static boolean seeInvisibleEnabled = false;
    /** When true and freecam is active, block finder scan center and distances use camera position instead of player. */
    public static boolean trackerUseFreecamPosition = false;

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("veilglyph_nimbus.txt");
    }

    public static void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) return;
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split("=", 2);
                if (parts.length != 2) continue;
                String key = parts[0].trim().toLowerCase();
                String value = parts[1].trim();
                switch (key) {
                    case "reach_enabled" -> reachEnabled = Boolean.parseBoolean(value);
                    case "reach_blocks" -> reachBlocks = Math.max(3f, Math.min(20f, Float.parseFloat(value)));
                    case "see_invisible" -> seeInvisibleEnabled = Boolean.parseBoolean(value);
                    case "tracker_use_freecam_position" -> trackerUseFreecamPosition = Boolean.parseBoolean(value);
                }
            }
        } catch (IOException | NumberFormatException ignored) {}
    }

    public static void save() {
        Path path = getConfigPath();
        try {
            String content = """
                # VeilGlyph Nimbus (extras) config
                reach_enabled=%s
                reach_blocks=%s
                see_invisible=%s
                tracker_use_freecam_position=%s
                """.formatted(reachEnabled, reachBlocks, seeInvisibleEnabled, trackerUseFreecamPosition);
            Files.writeString(path, content);
        } catch (IOException ignored) {}
    }
}
