package com.example.chestesp;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ChestEspClient implements ClientModInitializer {
    public static final String MOD_ID = "chestesp";

    public static boolean espEnabled = true;
    public static boolean tracersEnabled = true;
    public static boolean spawnerAlertEnabled = true;
    public static boolean includeBarrels = true;
    public static boolean includeShulkers = true;
    public static boolean susChunksEnabled = true;
    public static boolean susChatEnabled = true;

    private static KeyMapping keyEsp, keyTracers, keyFreecam, keySpawner, keySus, keyMenu;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        keyEsp = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.chestesp.toggle", InputConstants.Type.KEYSYM, InputConstants.KEY_K, category));
        keyTracers = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.chestesp.tracers", InputConstants.Type.KEYSYM, InputConstants.KEY_J, category));
        keyFreecam = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.chestesp.freecam", InputConstants.Type.KEYSYM, InputConstants.KEY_V, category));
        keySpawner = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.chestesp.spawner", InputConstants.Type.KEYSYM, InputConstants.KEY_U, category));

        keySus = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.chestesp.sus", InputConstants.Type.KEYSYM, InputConstants.KEY_G, category));
        keyMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.chestesp.menu", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));

        // START_CLIENT_TICK laeuft vor der Tastenverarbeitung des Spiels
        ClientTickEvents.START_CLIENT_TICK.register(ChestEspClient::onTick);

        WorldRenderEvents.BEFORE_TRANSLUCENT.register(EspRenderer::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> EspRenderer.close());
    }

    private static void onTick(Minecraft mc) {
        while (keyEsp.consumeClick()) {
            espEnabled = !espEnabled;
            status(mc, "Chest ESP: " + (espEnabled ? "AN" : "AUS"));
        }
        while (keyTracers.consumeClick()) {
            tracersEnabled = !tracersEnabled;
            status(mc, "Tracer: " + (tracersEnabled ? "AN" : "AUS"));
        }
        while (keySpawner.consumeClick()) {
            spawnerAlertEnabled = !spawnerAlertEnabled;
            status(mc, "Spawner-Alarm: " + (spawnerAlertEnabled ? "AN" : "AUS"));
        }
        while (keySus.consumeClick()) {
            susChunksEnabled = !susChunksEnabled;
            status(mc, "Sus Chunks: " + (susChunksEnabled ? "AN" : "AUS"));
        }
        while (keyMenu.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new MenuScreen());
        }
        while (keyFreecam.consumeClick()) {
            if (mc.player != null && mc.level != null) {
                Freecam.toggle(mc);
                status(mc, "Freecam: " + (Freecam.isActive() ? "AN" : "AUS"));
            }
        }

        Freecam.tick(mc);
        SpawnerAlert.tick(mc);
        SusChunks.tick(mc);
    }

    public static void status(Minecraft mc, String text) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(text), true);
        }
    }
}
