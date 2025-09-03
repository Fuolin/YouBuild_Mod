package com.youbuild.blueprinthandler;

import com.ldtteam.structurize.api.RotationMirror;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;

public class RotationMirrorHandler {

    public static BlockPos GetWorldPos(BlockPos anchorPos, BlockPos relativePos, BlockPos primaryOffset, RotationMirror rotationMirror){
        // 计算以主要参考点为中心的局部坐标
        BlockPos localPos = relativePos.subtract(primaryOffset);

        // 应用旋转/镜像变换到局部坐标
        BlockPos transformedLocalPos = rotationMirror.applyToPos(localPos);

        // 将变换后的局部坐标转回世界坐标（加上锚点位置）
        return anchorPos.offset(transformedLocalPos);
    }

    static boolean hasAnyDirectionProperty(BlockState state) {
        // 遍历方块的所有状态属性
        for (Property<?> property : state.getProperties()) {
            // 检查属性是否为DirectionProperty类型（包括其派生类）
            if (property instanceof DirectionProperty) {
                return true;
            }
        }
        // 没有找到任何方向属性
        return false;
    }

    static List<DirectionProperty> getDirectionProperties(BlockState state) {
        List<DirectionProperty> props = new ArrayList<>();
        for (Property<?> prop : state.getProperties()) {
            if (prop instanceof DirectionProperty) {
                props.add((DirectionProperty) prop);
            }
        }
        return props;
    }

}
