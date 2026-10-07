package com.spawnerbeacon.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.spawnerbeacon.BeaconConfig;
import com.spawnerbeacon.BeamState;
import com.spawnerbeacon.SpawnerBeaconClient;
import com.spawnerbeacon.spawner.SpawnerInfo;
import com.spawnerbeacon.spawner.SpawnerTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/** Beacon-style geometric beam using the official 26.1 custom pipeline pattern. */
public final class BeamRenderer {
    private static final RenderPipeline THROUGH_WALLS = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(SpawnerBeaconClient.MOD_ID, "pipeline/beam_through_walls"))
                    .withDepthStencilState(Optional.empty())
                    .build());

    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static volatile List<BeamState> beams = List.of();
    private static MappableRingBuffer vertexBuffer;
    private static boolean closed;

    private BeamRenderer() {}

    public static void register() {
        LevelRenderEvents.END_EXTRACTION.register(BeamRenderer::extract);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(BeamRenderer::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> close());
    }

    private static void extract(LevelExtractionContext context) {
        BeaconConfig cfg = BeaconConfig.get();
        if (!cfg.enabled || !cfg.dimensionEnabled(context.level())) {
            beams = List.of();
            return;
        }

        Vec3 camera = context.camera().position();
        double maxDistance = cfg.renderDistanceChunks * 16.0;
        double maxDistanceSqr = maxDistance * maxDistance;
        ArrayList<BeamState> next = new ArrayList<>();
        long now = System.nanoTime();

        for (SpawnerInfo info : SpawnerTracker.snapshot()) {
            if (!cfg.typeEnabled(info.type())) continue;
            Vec3 center = info.pos().getCenter();
            double distanceSqr = center.distanceToSqr(camera);
            if (distanceSqr > maxDistanceSqr) continue;

            int color = cfg.rainbow ? rainbowColor(now, info.pos().hashCode()) : cfg.colorFor(info.type());
            float r = ((color >> 16) & 255) / 255f;
            float g = ((color >> 8) & 255) / 255f;
            float b = (color & 255) / 255f;
            float fade = cfg.distanceFade ? (float) Math.max(0.12, 1.0 - Math.sqrt(distanceSqr) / maxDistance) : 1f;
            float pulse = cfg.animate ? (float) (0.90 + 0.10 * Math.sin(now / 260_000_000.0 + info.pos().hashCode())) : 1f;
            float alpha = (float) cfg.opacity * fade * pulse;
            float width = (float) cfg.thicknessFor(info.type());
            next.add(new BeamState(center.x, info.pos().getY(), center.z, cfg.maxY, width * 0.5f,
                    r, g, b, alpha, fade, info.type()));
        }
        beams = List.copyOf(next);
    }

    private static int rainbowColor(long nanos, int seed) {
        float h = ((nanos / 8_000_000_000f) + (seed & 255) / 255f) % 1f;
        float s = 0.70f, v = 1.0f;
        float i = h * 6f;
        int sector = (int) Math.floor(i);
        float f = i - sector;
        float p = v * (1f - s);
        float q = v * (1f - s * f);
        float t = v * (1f - s * (1f - f));
        float r, g, b;
        switch (sector % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    private static void render(LevelRenderContext context) {
        List<BeamState> list = beams;
        if (closed || list.isEmpty()) return;

        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        BufferBuilder buffer = new BufferBuilder(ALLOCATOR, THROUGH_WALLS.getVertexFormatMode(), THROUGH_WALLS.getVertexFormat());

        for (BeamState beam : list) {
            float outer = beam.halfWidth();
            float inner = Math.max(0.035f, outer * 0.38f);
            addBox(matrices.last().pose(), buffer, (float) beam.x() - outer, (float) beam.y(), (float) beam.z() - outer,
                    (float) beam.x() + outer, (float) beam.topY(), (float) beam.z() + outer,
                    beam.r(), beam.g(), beam.b(), beam.a() * 0.34f);
            addBox(matrices.last().pose(), buffer, (float) beam.x() - inner, (float) beam.y(), (float) beam.z() - inner,
                    (float) beam.x() + inner, (float) beam.topY(), (float) beam.z() + inner,
                    Math.min(1f, beam.r() + 0.18f), Math.min(1f, beam.g() + 0.18f), Math.min(1f, beam.b() + 0.18f), beam.a());
        }
        matrices.popPose();
        drawThroughWalls(Minecraft.getInstance(), buffer);
    }

    private static void addBox(Matrix4fc m, BufferBuilder b, float minX, float minY, float minZ,
                               float maxX, float maxY, float maxZ, float r, float g, float bl, float a) {
        b.addVertex(m, minX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(m, maxX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(m, minX, minY, maxZ).setColor(r, g, bl, a);
    }

    private static void drawThroughWalls(Minecraft client, BufferBuilder buffer) {
        MeshData built = buffer.buildOrThrow();
        MeshData.DrawState draw = built.drawState();
        VertexFormat format = draw.format();
        GpuBuffer vertices = upload(draw, format, built);
        draw(client, THROUGH_WALLS, built, draw, vertices, format);
        vertexBuffer.rotate();
    }

    private static GpuBuffer upload(MeshData.DrawState draw, VertexFormat format, MeshData built) {
        int size = draw.vertexCount() * format.getVertexSize();
        if (vertexBuffer == null || vertexBuffer.size() < size) {
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = new MappableRingBuffer(() -> SpawnerBeaconClient.MOD_ID + " beam vertex buffer",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size);
        }
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mapped = encoder.mapBuffer(vertexBuffer.currentBuffer().slice(0, built.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(built.vertexBuffer(), mapped.data());
        }
        return vertexBuffer.currentBuffer();
    }

    private static void draw(Minecraft client, RenderPipeline pipeline, MeshData built, MeshData.DrawState draw,
                             GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;
        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            built.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(built.indexBuffer());
            indexType = built.drawState().indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer shape = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shape.getBuffer(draw.indexCount());
            indexType = shape.type();
        }
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> SpawnerBeaconClient.MOD_ID + " beam render pass",
                client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
                client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indices, indexType);
            pass.drawIndexed(0 / format.getVertexSize(), 0, draw.indexCount(), 1);
        }
        built.close();
    }

    private static void close() {
        closed = true;
        ALLOCATOR.close();
        if (vertexBuffer != null) { vertexBuffer.close(); vertexBuffer = null; }
    }
}
