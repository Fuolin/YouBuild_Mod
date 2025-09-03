package com.youbuild.util;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.blueprints.v1.BlueprintUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class BlueprintSerializer {

    /**/
    /* 将蓝图序列化为NBT字符串*/
    public static String blueprintToString(Blueprint blueprint) {
        CompoundTag nbt = BlueprintUtil.writeBlueprintToNBT(blueprint);
        return nbt.toString();
    }

    /**
     * 从NBT字符串反序列化蓝图
     */
    @Nullable
    public static Blueprint blueprintFromString(String data, Level level) {
        try {
            CompoundTag nbt = TagParser.parseTag(data);
            return BlueprintUtil.readBlueprintFromNBT(nbt, level.registryAccess());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}