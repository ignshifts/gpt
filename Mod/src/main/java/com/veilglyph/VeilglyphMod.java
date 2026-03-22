package com.veilglyph;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class VeilglyphMod implements ClientModInitializer {

    public static final String MOD_ID = "veilglyph";
    /** When false, no scanning and no rendering — use to eliminate lag. */
    public static boolean modEnabled = true;
    public static boolean tracersEnabled = true;

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of(MOD_ID, MOD_ID));

    private static KeyBinding toggleModKey;
    private static KeyBinding toggleTracersKey;
    private static KeyBinding openScreenKey;
    private static KeyBinding nimbusKey;

    @Override
    public void onInitializeClient() {
        TrackerOptions.load();
        com.veilglyph.nimbus.NimbusConfig.load();
        toggleModKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.veilglyph.toggle_mod",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                CATEGORY
        ));

        toggleTracersKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.veilglyph.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY
        ));

        openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.veilglyph.screen",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));

        nimbusKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.veilglyph.nimbus",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleModKey.wasPressed()) {
                modEnabled = !modEnabled;
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal(modEnabled ? "VeilGlyph: \u00a7aMod ON" : "VeilGlyph: \u00a7cMod OFF (no lag)"),
                            true
                    );
                }
            }
            if (modEnabled && toggleTracersKey.wasPressed()) {
                tracersEnabled = !tracersEnabled;
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal(tracersEnabled ? "VeilGlyph: \u00a7aTracers ON" : "VeilGlyph: \u00a7cTracers OFF"),
                            true
                    );
                }
            }
            if (openScreenKey.wasPressed()) {
                client.setScreen(new VeilglyphMainScreen());
            }
            if (nimbusKey.wasPressed()) {
                client.setScreen(new com.veilglyph.nimbus.NimbusScreen(client.currentScreen));
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(ShulkerBoxRenderer::render);
        WorldRenderEvents.AFTER_ENTITIES.register(com.veilglyph.nimbus.InvisiblePlayerRenderer::render);
        ClientTickEvents.END_CLIENT_TICK.register(ShulkerBoxTracker::tick);
        ClientTickEvents.END_CLIENT_TICK.register(com.veilglyph.nimbus.FreecamHandler::tick);
    }
}
