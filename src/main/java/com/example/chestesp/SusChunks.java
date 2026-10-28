package com.example.chestesp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Findet "sus" Chunks: Chunks mit auffaellig vielen vom Spieler gebauten Bloecken
 * (Kisten, Enderkisten, Shulker, Hopper, Ofen, Leuchtfeuer ...) -> vermutlich eine Base.
 *
 * Jeder Block-Entity-Typ hat ein Gewicht, die Summe ergibt den Score eines Chunks.
 * Ab THRESHOLD gilt der Chunk als sus. Dorfer und Dungeons koennen gelegentlich
 * ebenfalls anschlagen - dann THRESHOLD erhoehen.
 */
public final class SusChunks {
    /** Ab diesem Score ist ein Chunk sus. */
    public static final float THRESHOLD = 6f;
    /** Maximaler Suchradius in Chunks (zusaetzlich begrenzt durch die Sichtweite). */
    public static final int MAX_RADIUS = 16;

    public record Entry(int cx, int cz, float score, int minY, int maxY) {}

    private static volatile List<Entry> entries = List.of();
    private static final Set<Long> notified = new HashSet<>();
    private static ClientLevel lastLevel;
    private static int cooldown;

    private SusChunks() {}

    public static List<Entry> entries() {
        return entries;
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level != lastLevel) {
            notified.clear();
            entries = List.of();
            lastLevel = level;
        }
        if (!ChestEspClient.susChunksEnabled || level == null || mc.player == null) {
            if (!entries.isEmpty()) entries = List.of();
            return;
        }
        if (++cooldown < 20) return;   // jede Sekunde neu scannen
        cooldown = 0;

        int radius = Math.min(mc.options.renderDistance().get(), MAX_RADIUS);
        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;

        List<Entry> result = new ArrayList<>();
        Set<Long> present = new HashSet<>();

        for (int cx = pcx - radius; cx <= pcx + radius; cx++) {
            for (int cz = pcz - radius; cz <= pcz + radius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;

                float score = 0f;
                int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
                for (var e : chunk.getBlockEntities().entrySet()) {
                    float w = weight(e.getValue());
                    if (w <= 0f) continue;
                    score += w;
                    int y = e.getKey().getY();
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
                if (score < THRESHOLD) continue;

                result.add(new Entry(cx, cz, score, minY, maxY));
                long key = key(cx, cz);
                present.add(key);
                if (ChestEspClient.susChatEnabled && notified.add(key)) {
                    mc.player.displayClientMessage(Component.literal(String.format(
                            "Sus Chunk! Chunk %d/%d (Block %d, %d) Score %.0f, Y %d-%d",
                            cx, cz, cx * 16 + 8, cz * 16 + 8, score, minY, maxY)), false);
                }
            }
        }

        // Chunks, die nicht mehr sus sind (oder weit weg), duerfen spaeter erneut melden
        notified.retainAll(present);
        entries = result;
    }

    private static float weight(BlockEntity be) {
        if (be instanceof BeaconBlockEntity) return 6f;
        if (be instanceof EnderChestBlockEntity || be instanceof ShulkerBoxBlockEntity) return 3f;
        if (be instanceof HopperBlockEntity || be instanceof EnchantingTableBlockEntity) return 2f;
        if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity
                || be instanceof AbstractFurnaceBlockEntity || be instanceof DispenserBlockEntity
                || be instanceof BrewingStandBlockEntity) return 1f;
        return 0f;   // Spawner, Betten, Schilder usw. zaehlen nicht
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }
}
