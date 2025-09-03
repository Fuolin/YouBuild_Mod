package com.youbuild.ui;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.ISurvivalBlueprintHandler;
import com.youbuild.blueprinthandler.BlueprintHandler;
import com.youbuild.item.Cardiac;
import com.youbuild.network.BlueprintBuildPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class BuildUI implements ISurvivalBlueprintHandler {

    @Override
    public String getId() {
        return "youbuild:build";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("handler.youbuild.build");
    }

    @Override
    public boolean canHandle(Blueprint blueprint, net.minecraft.client.multiplayer.ClientLevel level, Player player, BlockPos pos, RotationMirror rotationMirror) {
        return true;
    }

    @Override
    public void handle(Blueprint blueprint, String s, String s1, boolean b, Level level, Player player, BlockPos blockPos, RotationMirror rotationMirror) {
        BlockPos boxPos = Cardiac.getLastClickedPosition(player);
        if (boxPos != null) {
            //新建建造状态
            BlueprintHandler.BuildState buildState = new BlueprintHandler.BuildState();
            //发包
            try {
                PacketDistributor.sendToServer(new BlueprintBuildPacket(blueprint, blockPos, boxPos, rotationMirror, buildState));
            } catch (IllegalArgumentException e) {
                String translationKey = e.getMessage();
                player.sendSystemMessage(Component.translatable(translationKey));
            }
        }else player.sendSystemMessage(Component.translatable("item.cardiac_null"));
    }
}
