package com.youbuild.ui;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.ISurvivalBlueprintHandler;
import com.youbuild.network.BlueprintComparisonPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class ComparisonUI implements ISurvivalBlueprintHandler {

    @Override
    public String getId() {
        return "youbuild:compare";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("handler.youbuild.compare");
    }

    @Override
    public boolean canHandle(Blueprint blueprint, net.minecraft.client.multiplayer.ClientLevel level, Player player, BlockPos pos, RotationMirror rotationMirror) {
        return true;
    }

    @Override
    public void handle(Blueprint blueprint, String s, String s1, boolean b, Level level, Player player, BlockPos blockPos, RotationMirror rotationMirror) {
        String fileName = blueprint.getFileName();
        try {
        PacketDistributor.sendToServer(new BlueprintComparisonPacket(blueprint, blockPos, rotationMirror,fileName));
        } catch (IllegalArgumentException e) {
            String translationKey = e.getMessage();
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable(translationKey));
            }
        }
    }
}
