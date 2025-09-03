package com.youbuild.blueprinthandler;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.util.BlockInfo;
import com.youbuild.consumeditem.Cost;
import com.youbuild.consumeditem.IfCost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import java.util.*;

import static com.youbuild.Config.readConfig;
import static com.youbuild.blueprinthandler.RotationMirrorHandler.*;


public class BlueprintHandler {
    /**
     * 服务器端比较逻辑
     */
    public static List<BlockPos> findMismatchedBlocksServer(
            Blueprint blueprint,
            Level level,
            BlockPos anchorPos,
            RotationMirror rotationMirror
    ) {
        List<BlockPos> mismatches = new ArrayList<>();

        // 获取蓝图的主要参考点
        BlockPos primaryOffset = blueprint.getPrimaryBlockOffset();

        // 使用蓝图的缓存方块信息
        Map<BlockPos, BlockInfo> blockInfoMap = blueprint.getBlockInfoAsMap();

        // 遍历缓存中的所有方块信息
        for (Map.Entry<BlockPos, BlockInfo> entry : blockInfoMap.entrySet()) {
            BlockPos relativePos = entry.getKey();
            BlockInfo blockInfo = entry.getValue();

            // 获取蓝图中的方块状态
            BlockState blueprintState = blockInfo.getState();

            // 跳过 isAirOrVoid 类型的方块
            if (isAirOrVoid(blueprintState)) {
                continue;
            }

            //获取变化后的世界坐标
            BlockPos worldPos = GetWorldPos(anchorPos, relativePos,primaryOffset, rotationMirror);

            // 获取世界中的方块状态
            BlockState worldState = level.getBlockState(worldPos);

            // 比较状态
            if (!areBlockStatesEqual(blueprintState, worldState,rotationMirror,level,worldPos)) {
                mismatches.add(worldPos);
            }
        }
        return mismatches;
    }

    /**
     * 比较方块状态（忽略无关属性可在此调整）
     */
    private static boolean areBlockStatesEqual(BlockState state1, BlockState state2,RotationMirror rotationmirror,Level level,BlockPos worldPos) {

        if (state1 == state2) return true;
        if (state1 == null || state2 == null) return false;

        // 方块类型
        if (state1.getBlock() != state2.getBlock()) return false;

        // 判断方块类型并分别应用旋转/镜像
        if(hasAnyDirectionProperty(state1)){
            state1 = rotationmirror.applyToBlockState(state1, level, worldPos);
            List<DirectionProperty> props1 = getDirectionProperties(state1);
            List<DirectionProperty> props2 = getDirectionProperties(state2);
            for (DirectionProperty prop1 : props1) {
                // 找到state2中同名的方向属性
                Optional<DirectionProperty> prop2Opt = props2.stream()
                        .filter(prop -> prop.getName().equals(prop1.getName()))
                        .findFirst();

                if (prop2Opt.isEmpty()) {
                    return false; // 属性名称不匹配
                }

                DirectionProperty prop2 = prop2Opt.get();
                // 比较属性值
                if (state1.getValue(prop1) != state2.getValue(prop2)) {
                    return false;
                }
            }
        }

        //可以插入其他判断条件

        return true;
    }
    public enum BuildFailureType {
        NONE,
        BLOCKED,          // 位置被阻挡
        MISSING_ITEM,     // 物品不足
        BLUEPRINT_ERROR,  // 蓝图数据错误
        PLACEMENT_FAILED  // 放置失败
    }

    public static class BuildState {
        private boolean complete = false;
        private BuildFailureType failureType = BuildFailureType.NONE;
        private BlockPos problemPos = null;
        private int processedCount = 0;
        private int totalBlocks = 0;
        private BlockState requiredState = null;

        public boolean isComplete() { return complete; }
        public void setComplete(boolean complete) { this.complete = complete; }

        public BuildFailureType getFailureType() { return failureType; }
        public void setFailureType(BuildFailureType type) { this.failureType = type; }

        public BlockPos getProblemPos() { return problemPos; }
        public void setProblemPos(BlockPos pos) { this.problemPos = pos; }

        public int getProcessedCount() { return processedCount; }
        public void setProcessedCount(int count) { this.processedCount = count; }

        public int getTotalBlocks() { return totalBlocks; }
        public void setTotalBlocks(int total) { this.totalBlocks = total; }

        public BlockState getRequiredState() { return requiredState; }
        public void setRequiredState(BlockState state) { this.requiredState = state; }
    }

    public static BuildState buildingBlock(
            BuildState state,
            Blueprint blueprint,
            Level level,
            BlockPos anchorPos,
            BlockPos boxPos,
            RotationMirror rotationMirror,
            ServerPlayer player) {

        // 初始化总方块数
        if (state.getTotalBlocks() == 0) {
            state.setTotalBlocks(countBuildableBlocks(blueprint));
        }

        BlockPos primaryOffset = blueprint.getPrimaryBlockOffset();
        Map<BlockPos, BlockInfo> blockInfoMap = blueprint.getBlockInfoAsMap();

        // 创建有序列表（按Y坐标排序）
        List<Map.Entry<BlockPos, BlockInfo>> entries = blockInfoMap.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey().getY()))
                .toList();

        // 建造循环
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<BlockPos, BlockInfo> entry = entries.get(i);
            BlockInfo blockInfo = entry.getValue();
            BlockState blueprintState = blockInfo.getState();

            // 跳过空气方块
            if (isAirOrVoid(blueprintState)) {
                state.setProcessedCount(i + 1);
                continue;
            }

            BlockPos relativePos = entry.getKey();
            BlockPos worldPos = GetWorldPos(anchorPos, relativePos, primaryOffset, rotationMirror);

            // 检查位置是否有效
            if (!level.isInWorldBounds(worldPos)) {
                state.setFailureType(BuildFailureType.BLOCKED);
                state.setProblemPos(worldPos);
                state.setProcessedCount(i);
                return state;
            }

            BlockState worldState = level.getBlockState(worldPos);
            BlockState rotatedState = null;
            if (blueprintState != null) {
                rotatedState = rotationMirror.applyToBlockState(blueprintState, level, worldPos);
            }

            // 跳过已正确放置的方块
            if (areBlockStatesEqual(rotatedState, worldState, rotationMirror, level, worldPos)) {
                state.setProcessedCount(i + 1);
                continue;
            }

            // 检查位置阻挡
            if (!canPlaceAtPosition(level, worldPos, worldState)) {
                state.setFailureType(BuildFailureType.BLOCKED);
                state.setProblemPos(worldPos);
                state.setProcessedCount(i);
                return state;
            }

            // 检查物品
            IfCost.ifcost ifcost = null;
            if (blueprintState != null) {
                ifcost = hasRequiredItems(blueprintState, player, boxPos, level);
            }
            if (ifcost != null && !ifcost.getIfCostCardiac()) {
                state.setFailureType(BuildFailureType.MISSING_ITEM);
                state.setProblemPos(boxPos);
                state.setRequiredState(Blocks.AIR.defaultBlockState());
                if (!ifcost.getIfCostItem()) state.setRequiredState(rotatedState);
                state.setProcessedCount(i);
                return state;
            }

            // 尝试放置方块
            if (!placeBlock(level, worldPos, rotatedState, player)) {
                state.setFailureType(BuildFailureType.PLACEMENT_FAILED);
                state.setProblemPos(worldPos);
                state.setProcessedCount(i);
                return state;
            }

            // 更新已处理计数
            state.setProcessedCount(i + 1);
        }

        // 所有方块处理完成
        state.setComplete(true);
        return state;
    }

    // ========== 辅助方法 ==========

    private static int countBuildableBlocks(Blueprint blueprint) {
        return (int) blueprint.getBlockInfoAsMap().values().stream()
                .filter(info -> !isAirOrVoid(info.getState()))
                .count();
    }

    private static boolean isAirOrVoid(BlockState state) {
        boolean other = false;
        List<BlockState> xblockState = readConfig();
        if (xblockState != null) {
            for (BlockState blockState : xblockState) {
                if (state == blockState) {
                    other = true;
                    break;
                }
            }
        }
        return state == null || state.isAir() || other;
    }

    private static boolean canPlaceAtPosition(Level level, BlockPos pos, BlockState existingState) {
        return existingState.isAir() || existingState.canBeReplaced();
    }

    private static IfCost.ifcost hasRequiredItems(BlockState state, ServerPlayer player, BlockPos boxPos, Level level) {
        return Cost.costItems(state.getBlock().asItem(),boxPos,level);
    }

    private static boolean placeBlock(Level level, BlockPos pos, BlockState state, ServerPlayer player) {
        return level.setBlock(pos, state, Block.UPDATE_ALL);
    }

}