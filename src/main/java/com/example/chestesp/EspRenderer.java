package com.example.chestesp;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Zeichnet Umrandungen und Tracer durch Waende.
 * Linien werden als kamerazugewandte duenne Quads gebaut und mit einer eigenen
 * Pipeline ohne Tiefentest gezeichnet (Muster aus der Fabric-Doku fuer 1.21.11).
 */
public final class EspRenderer {
    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(ChestEspClient.MOD_ID, "pipeline/esp_through_walls"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build());

    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(1 << 20);
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private static final float[] SUS_COLOR = {1f, 0.1f, 0.7f};

    private static BufferBuilder buffer;
    private static MappableRingBuffer vertexBuffer;

    private EspRenderer() {}

    // ------------------------------------------------------------------ Extraktion

    public static void render(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        Freecam.syncRotation(mc);

        boolean doEsp = ChestEspClient.espEnabled;
        boolean doSus = ChestEspClient.susChunksEnabled && !SusChunks.entries().isEmpty();
        if (!doEsp && !doSus) return;

        ClientLevel level = mc.level;
        Entity camEntity = mc.getCameraEntity();
        if (level == null || camEntity == null || mc.player == null) return;

        PoseStack matrices = context.matrices();
        Vec3 cam = context.worldState().cameraRenderState.pos;
        Matrix4fc pose = matrices.last().pose();

        // Blickrichtung fuer den Tracer-Startpunkt (kurz vor der Kamera)
        double yawR = Math.toRadians(camEntity.getViewYRot(1f));
        double pitchR = Math.toRadians(camEntity.getViewXRot(1f));
        double sx = -Math.sin(yawR) * Math.cos(pitchR) * 0.4;
        double sy = -Math.sin(pitchR) * 0.4;
        double sz = Math.cos(yawR) * Math.cos(pitchR) * 0.4;

        buffer = new BufferBuilder(ALLOCATOR, PIPELINE.getVertexFormatMode(), PIPELINE.getVertexFormat());
        int found = 0;

        if (doEsp) {
            int viewDist = mc.options.renderDistance().get();
            int ccx = Mth.floor(cam.x) >> 4;
            int ccz = Mth.floor(cam.z) >> 4;

            for (int cx = ccx - viewDist; cx <= ccx + viewDist; cx++) {
                for (int cz = ccz - viewDist; cz <= ccz + viewDist; cz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) continue;

                    for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                        float[] c = colorFor(e.getValue());
                        if (c == null) continue;
                        BlockPos p = e.getKey();
                        found++;

                        // Koordinaten relativ zur Kamera
                        double x1 = p.getX() - cam.x, y1 = p.getY() - cam.y, z1 = p.getZ() - cam.z;
                        box(pose, buffer, x1, y1, z1, x1 + 1, y1 + 1, z1 + 1, c);

                        // Tracer zur Mitte
                        if (ChestEspClient.tracersEnabled) {
                            line(pose, buffer, sx, sy, sz,
                                    x1 + 0.5, y1 + 0.5, z1 + 0.5,
                                    c[0], c[1], c[2], 0.85f, 0.001, 0.00015);
                        }
                    }
                }
            }
        }

        if (doSus) {
            for (SusChunks.Entry s : SusChunks.entries()) {
                found++;
                double x1 = s.cx() * 16.0 - cam.x;
                double z1 = s.cz() * 16.0 - cam.z;
                double y1 = s.minY() - cam.y;
                double y2 = s.maxY() + 1 - cam.y;
                box(pose, buffer, x1, y1, z1, x1 + 16, y2, z1 + 16, SUS_COLOR);

                if (ChestEspClient.tracersEnabled) {
                    line(pose, buffer, sx, sy, sz,
                            x1 + 8, (y1 + y2) * 0.5, z1 + 8,
                            SUS_COLOR[0], SUS_COLOR[1], SUS_COLOR[2], 0.85f, 0.001, 0.00015);
                }
            }
        }

        if (found == 0) {
            // Nichts gezeichnet: Puffer verwerfen
            MeshData empty = buffer.build();
            if (empty != null) empty.close();
            buffer = null;
            return;
        }

        draw(mc);
    }

    /** Zeichnet die 12 Kanten einer Box (Koordinaten relativ zur Kamera). */
    private static void box(Matrix4fc pose, BufferBuilder buf,
                            double x1, double y1, double z1, double x2, double y2, double z2,
                            float[] c) {
        double[][] v = {
                {x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}, {x1, y1, z2},
                {x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}
        };
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        for (int[] ed : edges) {
            line(pose, buf,
                    v[ed[0]][0], v[ed[0]][1], v[ed[0]][2],
                    v[ed[1]][0], v[ed[1]][1], v[ed[1]][2],
                    c[0], c[1], c[2], 1f, 0.0025, 0.0003);
        }
    }

    private static float[] colorFor(BlockEntity be) {
        if (be instanceof SpawnerBlockEntity) return new float[]{0.2f, 1f, 0.2f};
        if (be instanceof TrappedChestBlockEntity) return new float[]{1f, 0.2f, 0.2f};
        if (be instanceof ChestBlockEntity) return new float[]{1f, 0.65f, 0.1f};
        if (be instanceof EnderChestBlockEntity) return new float[]{0.7f, 0.3f, 1f};
        if (ChestEspClient.includeBarrels && be instanceof BarrelBlockEntity) return new float[]{0.6f, 0.4f, 0.2f};
        if (ChestEspClient.includeShulkers && be instanceof ShulkerBoxBlockEntity) return new float[]{0.2f, 0.9f, 0.9f};
        return null;
    }

    /**
     * Zeichnet eine Linie als duenne, zur Kamera zeigende Flaeche (beide Windungen,
     * damit kein Culling stoert). Alle Koordinaten sind relativ zur Kamera.
     */
    private static void line(Matrix4fc pose, BufferBuilder buf,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             float r, float g, float b, float a,
                             double halfWidthMin, double halfWidthPerDist) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double mx = (x1 + x2) * 0.5, my = (y1 + y2) * 0.5, mz = (z1 + z2) * 0.5;
        double dist = Math.sqrt(mx * mx + my * my + mz * mz);

        // Seitenvektor = Richtung x Blickvektor zur Mitte
        double px = dy * mz - dz * my;
        double py = dz * mx - dx * mz;
        double pz = dx * my - dy * mx;
        double len = Math.sqrt(px * px + py * py + pz * pz);
        if (len < 1.0e-9) {
            px = 0; py = 1; pz = 0; len = 1;
        }
        double hw = Math.max(halfWidthMin, dist * halfWidthPerDist);
        px = px / len * hw;
        py = py / len * hw;
        pz = pz / len * hw;

        vtx(buf, pose, x1 + px, y1 + py, z1 + pz, r, g, b, a);
        vtx(buf, pose, x1 - px, y1 - py, z1 - pz, r, g, b, a);
        vtx(buf, pose, x2 - px, y2 - py, z2 - pz, r, g, b, a);
        vtx(buf, pose, x2 + px, y2 + py, z2 + pz, r, g, b, a);

        vtx(buf, pose, x2 + px, y2 + py, z2 + pz, r, g, b, a);
        vtx(buf, pose, x2 - px, y2 - py, z2 - pz, r, g, b, a);
        vtx(buf, pose, x1 - px, y1 - py, z1 - pz, r, g, b, a);
        vtx(buf, pose, x1 + px, y1 + py, z1 + pz, r, g, b, a);
    }

    private static void vtx(BufferBuilder buf, Matrix4fc pose, double x, double y, double z,
                            float r, float g, float b, float a) {
        buf.addVertex(pose, (float) x, (float) y, (float) z).setColor(r, g, b, a);
    }

    // ------------------------------------------------------------------ Zeichnen

    private static void draw(Minecraft client) {
        MeshData built = buffer.buildOrThrow();
        MeshData.DrawState state = built.drawState();
        VertexFormat format = state.format();

        GpuBuffer vertices = upload(state, format, built);
        drawBuffer(client, built, state, vertices, format);

        vertexBuffer.rotate();
        buffer = null;
    }

    private static GpuBuffer upload(MeshData.DrawState state, VertexFormat format, MeshData built) {
        int size = state.vertexCount() * format.getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < size) {
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = new MappableRingBuffer(() -> ChestEspClient.MOD_ID + " esp vertex buffer",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size);
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView view = encoder.mapBuffer(
                vertexBuffer.currentBuffer().slice(0, built.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(built.vertexBuffer(), view.data());
        }
        return vertexBuffer.currentBuffer();
    }

    private static void drawBuffer(Minecraft client, MeshData built, MeshData.DrawState state,
                                   GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (PIPELINE.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            built.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
            indices = PIPELINE.getVertexFormat().uploadImmediateIndexBuffer(built.indexBuffer());
            indexType = built.drawState().indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer seq = RenderSystem.getSequentialBuffer(PIPELINE.getVertexFormatMode());
            indices = seq.getBuffer(state.indexCount());
            indexType = seq.type();
        }

        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> ChestEspClient.MOD_ID + " esp rendering",
                client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
                client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indices, indexType);
            pass.drawIndexed(0, 0, state.indexCount(), 1);
        }

        built.close();
    }

    public static void close() {
        ALLOCATOR.close();
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
