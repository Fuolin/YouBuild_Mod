package com.youbuild.consumeditem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class Cost {
    public static IfCost.ifcost costItems(Item needitem, BlockPos boxPos, Level level) {
        // 参数验证
        IfCost.ifcost ifcost = new IfCost.ifcost();
        if (level == null || boxPos == null || needitem == null) {
            return ifcost;
        }

        // 获取方块实体的IItemHandler能力
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, boxPos, null);
        if (handler == null) {
            return ifcost; // 非有效物品容器
        }

        // 遍历所有槽位寻找目标物品
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);

            // 检查物品是否匹配
            if (stack.getItem() == needitem && !stack.isEmpty()) {
                // 尝试消耗一个物品（模拟=false表示实际执行）
                ItemStack extracted = handler.extractItem(slot, 1, false);

                //消耗Cardiac
                // 如果成功消耗物品则返回true
                if (!extracted.isEmpty()) {
                    ifcost.setIfCost(true,false);
                    if (costCardiac(handler)){
                        ifcost.setIfCost(true,true);
                    }
                    return ifcost;
                }
            }
        }

        // 未找到或消耗物品失败
        return ifcost;
    }

    private static boolean costCardiac(IItemHandler handler) {
        /*ItemStack cardiacstack = new ItemStack(YouBuild.Cardiac_ITEM.get(), 1);

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);

            // 检查物品是否匹配
            if (stack.getItem() == cardiacstack.getItem() && !stack.isEmpty()) {
                // 尝试消耗cardia（模拟=false表示实际执行）
                ItemStack extracted = handler.extractItem(slot, 1, false);

                if (!extracted.isEmpty()) {
                    return true;
                }
            }
        }
        return false;*/
        return true;
    }
}


