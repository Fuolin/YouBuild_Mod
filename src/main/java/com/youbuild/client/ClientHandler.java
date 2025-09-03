package com.youbuild.client;

import com.youbuild.YouBuild;
import com.youbuild.blueprinthandler.BlueprintHandler;
import com.youbuild.blueprinthandler.Red;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ClientHandler {
    public static void handleComparisonResult(List<BlockPos> mismatches) {
        Red.clearAllBlocks();
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) return;
        if (!mismatches.isEmpty()) {
            mc.player.sendSystemMessage(
                    Component.translatable("blueprint.mismatch_found", mismatches.size()));
            Red.addBlockPositions(mismatches);
        }
    }

    public static void handleBuildResult(
            boolean isComplete,
            BlueprintHandler.BuildFailureType failureType,
            BlockPos problemPos,
            int processedCount,
            int totalBlocks,
            BlockState requiredState
    ) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.level == null || mc.player == null) return;

            if (isComplete) {
                mc.player.sendSystemMessage(Component.translatable("blueprint.build_complete"));
                Red.clearAllBlocks();
                return;
            }

            // 计算进度百分比
            int count = processedCount - 1;
            int progressPercent = totalBlocks > 0 ? (count * 100) / totalBlocks : 0;
            mc.player.sendSystemMessage(
                    Component.translatable("blueprint.progress", count, totalBlocks, progressPercent));

            switch (failureType) {
                case BLOCKED:
                    Red.clearAllBlocks();
                    mc.player.sendSystemMessage(
                            Component.translatable("blueprint.blocked_position"));
                    if (problemPos != null) {
                        Red.addBlockPositions(List.of(problemPos));
                    }
                    break;

                case MISSING_ITEM:
                    Red.clearAllBlocks();
                    if (problemPos != null){
                        Red.addBlockPositions(List.of(problemPos));
                        mc.player.sendSystemMessage(
                                Component.translatable("blueprint.chest_position", problemPos.toShortString()));
                    }

                    if (requiredState != null) {
                        Item blockItem = requiredState.getBlock().asItem();
                        ItemStack stack = new ItemStack(blockItem);
                        if(requiredState == Blocks.AIR.defaultBlockState())stack=new ItemStack(YouBuild.Cardiac_ITEM.get(), 1);
                        mc.player.sendSystemMessage(Component.translatable("blueprint.missing_item")
                                .append(stack.getDisplayName()));
                    }
                    break;

                case BLUEPRINT_ERROR:
                    Red.clearAllBlocks();
                    mc.player.sendSystemMessage(Component.literal("blueprint.blueprint_error"));
                    break;

                case PLACEMENT_FAILED:
                    Red.clearAllBlocks();
                    mc.player.sendSystemMessage(Component.literal("blueprint.placement_failed"));
                    if (problemPos != null) {
                        Red.addBlockPositions(List.of(problemPos));
                    }
                    break;

                case NONE:
                    // 没有错误，只是进度更新
                    Red.clearAllBlocks();
                    break;
            }
        });
    }
}