package com.youbuild.blueprinthandler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.youbuild.YouBuild;

@Mod(Red.ModId)
public class Red {
    static final String ModId = YouBuild.ModId;
    private static final Set<BlockPos> targetPositions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final float LINE_WIDTH = 2.0f;
    private static final float OUTLINE_OFFSET = 0.002f; // 轻微偏移避免Z冲突

    public Red(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(this::onRenderLevel);
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || targetPositions.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        // 预计算所有相邻关系
        Map<BlockPos, Set<BlockPos>> neighborMap = buildNeighborMap();

        for (BlockPos pos : targetPositions) {
            renderBlockOutline(poseStack, bufferSource, pos, neighborMap, cameraPos);
        }

        bufferSource.endBatch();
    }

    private Map<BlockPos, Set<BlockPos>> buildNeighborMap() {
        Map<BlockPos, Set<BlockPos>> neighborMap = new HashMap<>();

        for (BlockPos pos : targetPositions) {
            Set<BlockPos> neighbors = new HashSet<>();
            for (BlockPos neighbor : getNeighbors(pos)) {
                if (targetPositions.contains(neighbor)) {
                    neighbors.add(neighbor);
                }
            }
            neighborMap.put(pos, neighbors);
        }

        return neighborMap;
    }

    private List<BlockPos> getNeighbors(BlockPos pos) {
        return List.of(
                pos.north(), pos.south(),
                pos.east(), pos.west(),
                pos.above(), pos.below()
        );
    }

    private void renderBlockOutline(PoseStack poseStack, MultiBufferSource bufferSource,
                                    BlockPos pos, Map<BlockPos, Set<BlockPos>> neighborMap, Vec3 cameraPos) {
        Set<BlockPos> neighbors = neighborMap.getOrDefault(pos, Collections.emptySet());

        // 创建带轻微偏移的AABB
        AABB box = new AABB(pos).inflate(OUTLINE_OFFSET);
        AABB relativeBox = box.move(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumer builder = bufferSource.getBuffer(RenderType.debugLineStrip(LINE_WIDTH));
        float r = 1.0f, g = 0.0f, b = 0.0f, a = 1.0f;

        // 基于相邻方块决定渲染哪些边
        boolean northBlocked = neighbors.contains(pos.north());
        boolean southBlocked = neighbors.contains(pos.south());
        boolean eastBlocked = neighbors.contains(pos.east());
        boolean westBlocked = neighbors.contains(pos.west());
        boolean upBlocked = neighbors.contains(pos.above());
        boolean downBlocked = neighbors.contains(pos.below());

        double minX = relativeBox.minX;
        double minY = relativeBox.minY;
        double minZ = relativeBox.minZ;
        double maxX = relativeBox.maxX;
        double maxY = relativeBox.maxY;
        double maxZ = relativeBox.maxZ;

        poseStack.pushPose();

        // 只渲染暴露的边缘
        // 顶部边缘
        if (!upBlocked) {
            renderLine(builder, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a); // 上北
            renderLine(builder, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a); // 上东
            renderLine(builder, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a); // 上南
            renderLine(builder, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a); // 上西
        }

        // 底部边缘
        if (!downBlocked) {
            renderLine(builder, minX, minY, minZ, maxX, minY, minZ, r, g, b, a); // 下北
            renderLine(builder, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a); // 下东
            renderLine(builder, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a); // 下南
            renderLine(builder, minX, minY, maxZ, minX, minY, minZ, r, g, b, a); // 下西
        }

        // 垂直边缘
        if (!northBlocked) renderLine(builder, minX, minY, minZ, minX, maxY, minZ, r, g, b, a); // 西北
        if (!northBlocked) renderLine(builder, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a); // 东北
        if (!southBlocked) renderLine(builder, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a); // 东南
        if (!southBlocked) renderLine(builder, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a); // 西南

        if (!westBlocked) renderLine(builder, minX, minY, minZ, minX, maxY, minZ, r, g, b, a); // 西北
        if (!westBlocked) renderLine(builder, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a); // 西南
        if (!eastBlocked) renderLine(builder, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a); // 东北
        if (!eastBlocked) renderLine(builder, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a); // 东南

        poseStack.popPose();
    }

    private void renderLine(VertexConsumer builder, double x1, double y1, double z1,
                            double x2, double y2, double z2, float r, float g, float b, float a) {
        builder.addVertex((float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
        builder.addVertex((float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
    }

    public static void addBlockPositions(Collection<BlockPos> positions) {
        if (positions != null && !positions.isEmpty()) {
            targetPositions.addAll(positions);
        }
    }

    public static void clearAllBlocks() {
        targetPositions.clear();
    }

    public static Set<BlockPos> getTargetPositions() {
        return Collections.unmodifiableSet(targetPositions);
    }
}