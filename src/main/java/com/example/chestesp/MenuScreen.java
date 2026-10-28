package com.example.chestesp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Einfaches Menue (Standard: Rechte Shift-Taste), um alle Funktionen per Klick umzuschalten. */
public final class MenuScreen extends Screen {
    private static final int BTN_W = 150;
    private static final int BTN_H = 20;
    private static final int GAP = 6;

    public MenuScreen() {
        super(Component.literal("Chest ESP Client"));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - BTN_W - GAP / 2;
        int right = this.width / 2 + GAP / 2;
        int y = this.height / 2 - 64;
        int row = BTN_H + GAP;

        toggle(left, y, "Chest ESP", () -> ChestEspClient.espEnabled, v -> ChestEspClient.espEnabled = v);
        toggle(right, y, "Tracer", () -> ChestEspClient.tracersEnabled, v -> ChestEspClient.tracersEnabled = v);
        y += row;
        toggle(left, y, "F\u00e4sser", () -> ChestEspClient.includeBarrels, v -> ChestEspClient.includeBarrels = v);
        toggle(right, y, "Shulker", () -> ChestEspClient.includeShulkers, v -> ChestEspClient.includeShulkers = v);
        y += row;
        toggle(left, y, "Spawner-Alarm", () -> ChestEspClient.spawnerAlertEnabled, v -> ChestEspClient.spawnerAlertEnabled = v);
        toggle(right, y, "Sus Chunks", () -> ChestEspClient.susChunksEnabled, v -> ChestEspClient.susChunksEnabled = v);
        y += row;
        toggle(left, y, "Sus-Chat-Meldung", () -> ChestEspClient.susChatEnabled, v -> ChestEspClient.susChatEnabled = v);

        // Freecam: Menue schliessen und umschalten
        Button freecam = Button.builder(freecamLabel(), b -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(null);
            Freecam.toggle(mc);
            ChestEspClient.status(mc, "Freecam: " + (Freecam.isActive() ? "AN" : "AUS"));
        }).bounds(right, y, BTN_W, BTN_H).build();
        this.addRenderableWidget(freecam);
        y += row + GAP;

        this.addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> this.onClose())
                .bounds(this.width / 2 - BTN_W / 2, y, BTN_W, BTN_H).build());
    }

    private void toggle(int x, int y, String name, BooleanSupplier getter, Consumer<Boolean> setter) {
        Button button = Button.builder(label(name, getter.getAsBoolean()), b -> {
            boolean now = !getter.getAsBoolean();
            setter.accept(now);
            b.setMessage(label(name, now));
        }).bounds(x, y, BTN_W, BTN_H).build();
        this.addRenderableWidget(button);
    }

    private static Component label(String name, boolean on) {
        return Component.literal(name + ": " + (on ? "AN" : "AUS"));
    }

    private static Component freecamLabel() {
        return Component.literal("Freecam: " + (Freecam.isActive() ? "AN" : "AUS"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);   // zeichnet auch den Hintergrund
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 90, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.literal("Sus Chunks gefunden: " + SusChunks.entries().size()),
                this.width / 2, this.height / 2 + 62, 0xFFFF55AA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
