package com.youbuild.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Cardiac extends Item {

    public Cardiac(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static final Map<Player, BlockPos> lastClickedPositions = new ConcurrentHashMap<>();

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // 客户端视觉效果
        if (world.isClientSide() && player.isShiftKeyDown()) {
            // 添加粒子效果
            for (int i = 0; i < 5; i++) {
                world.addParticle(ParticleTypes.HEART,
                        player.getX(),
                        player.getY() + 1.5,
                        player.getZ(),
                        (player.getRandom().nextDouble() - 0.5) * 0.2,
                        0.1,
                        (player.getRandom().nextDouble() - 0.5) * 0.2);
            }
        }

        return InteractionResultHolder.pass(itemStack);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();

        if (player != null && player.isShiftKeyDown()) {
            BlockPos pos = context.getClickedPos();

            // 只在服务端执行操作
            if (!world.isClientSide()) {
                lastClickedPositions.put(player, pos);

                player.sendSystemMessage(Component.translatable(
                        "msg.youbuild.position_selected",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()
                ));

                // 添加冷却时间
                player.getCooldowns().addCooldown(this, 10); // 0.5秒冷却
            }
            return InteractionResult.SUCCESS; // 阻止后续交互
        }
        return InteractionResult.PASS; // 允许正常方块交互
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // 添加翻译后的工具提示
        tooltip.add(Component.translatable("tooltip.youbuild.cardiac")
                .withStyle(ChatFormatting.GRAY));
        if (flag.hasShiftDown()) {
            // 添加详细说明（使用不同颜色区分）
            tooltip.add(Component.translatable("tooltip.youbuild.cardiac.detail.line1")
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.youbuild.cardiac.detail.line2")
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.youbuild.cardiac.detail.line3")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            // 提示玩家按住Shift查看更多
            tooltip.add(Component.translatable("tooltip.youbuild.cardiac.moreinfo")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    /**
     * 获取玩家最后选择的箱子位置
     */
    public static BlockPos getLastClickedPosition(Player player) {
        return lastClickedPositions.get(player);
    }

    /**
     * 清理玩家数据
     */
    public static void clearPlayerPosition(Player player) {
        lastClickedPositions.remove(player);
    }

    /**
     * 注册事件监听器
     */
    public static void registerEventListeners() {
        // 玩家登出事件
        NeoForge.EVENT_BUS.addListener(Cardiac::onPlayerLoggedOut);
    }

    /**
     * 处理玩家登出事件
     */
    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clearPlayerData(event.getEntity());
    }

    /**
     * 处理玩家切换维度事件
     */
    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearPlayerData(event.getEntity());
    }

    /**
     * 处理玩家重生事件
     */
    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        clearPlayerData(event.getEntity());
    }

    /**
     * 清理玩家数据并记录日志
     */
    private static void clearPlayerData(Player player) {
        if (player instanceof ServerPlayer) {
            BlockPos previous = lastClickedPositions.remove(player);
            if (previous != null) {
                System.out.println("Cleared Cardiac position for " + player.getName().getString());
            }
        }
    }

}