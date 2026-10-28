package com.example.chestesp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Meldet Spawner in der Naehe (einmal pro Spawner, solange man in Reichweite bleibt). */
public final class SpawnerAlert {
    /** Alarm-Radius in Bloecken. */
    public static final int RADIUS = 48;

    private static final Set<BlockPos> notified = new HashSet<>();
    private static ClientLevel lastLevel;
    private static int cooldown;

    private SpawnerAlert() {}

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level != lastLevel) {
            notified.clear();
            lastLevel = level;
        }
        if (!ChestEspClient.spawnerAlertEnabled || level == null || mc.player == null) return;
        if (++cooldown < 10) return;   // alle 0,5 Sekunden
        cooldown = 0;

        BlockPos pp = mc.player.blockPosition();
        int pcx = pp.getX() >> 4;
        int pcz = pp.getZ() >> 4;
        int cr = (RADIUS >> 4) + 1;
        double r2 = (double) RADIUS * RADIUS;

        Set<BlockPos> inRange = new HashSet<>();

        for (int cx = pcx - cr; cx <= pcx + cr; cx++) {
            for (int cz = pcz - cr; cz <= pcz + cr; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    if (!(e.getValue() instanceof SpawnerBlockEntity)) continue;
                    BlockPos pos = e.getKey().immutable();
                    if (pos.distSqr(pp) > r2) continue;
                    inRange.add(pos);
                    if (notified.add(pos)) {
                        alert(mc, pos, Math.sqrt(pos.distSqr(pp)));
                    }
                }
            }
        }

        // Spawner, die ausser Reichweite sind, duerfen spaeter erneut melden
        notified.retainAll(inRange);
    }

    private static void alert(Minecraft mc, BlockPos pos, double dist) {
        String text = String.format("Spawner in der N\u00e4he! X:%d Y:%d Z:%d (%.0f Bl\u00f6cke)",
                pos.getX(), pos.getY(), pos.getZ(), dist);
        mc.player.displayClientMessage(Component.literal(text), false); // Chat
        mc.player.displayClientMessage(Component.literal(text), true);  // Actionbar
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f));
    }
}
