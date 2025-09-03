package com.youbuild.network;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.youbuild.blueprinthandler.BlueprintHandler;
import com.youbuild.util.BlueprintSerializer;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.youbuild.network.Compress.compressString;
import static com.youbuild.network.Compress.decompressToString;

public class BlueprintBuildPacket implements CustomPacketPayload {

    // 配置参数
    private static final int MAX_COMPRESSED_LENGTH = 30000;  // 压缩蓝图数据最大长度

    public static final Type<BlueprintBuildPacket> TYPE =
            new Type<>(ResourceLocation.parse("youbuild:blueprint_build"));

    // 修改后的编解码器
    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintBuildPacket> STREAM_CODEC =
            StreamCodec.composite(
                    // 1. 压缩蓝图数据 (字节数组)
                    ByteBufCodecs.BYTE_ARRAY,
                    BlueprintBuildPacket::getCompressedBlueprintData,

                    // 2. 锚点位置
                    BlockPos.STREAM_CODEC,
                    BlueprintBuildPacket::getAnchorPos,

                    // 3. box位置
                    BlockPos.STREAM_CODEC,
                    BlueprintBuildPacket::getBoxPos,

                    // 4. 旋转镜像
                    RotationMirrorCodec.STREAM_CODEC,
                    BlueprintBuildPacket::getRotationMirror,

                    // 5. 建造状态
                    BuildStateCodec.STREAM_CODEC,
                    BlueprintBuildPacket::getBuildState,

                    // 构造函数
                    BlueprintBuildPacket::new
            );

    // 字段定义
    private final byte[] compressedBlueprintData;  // 压缩后的蓝图数据
    private final BlockPos anchorPos;
    private final BlockPos boxPos;
    private final RotationMirror rotationMirror;
    private final BlueprintHandler.BuildState buildState;

    // 新增构造函数 - 用于编解码器
    public BlueprintBuildPacket(byte[] compressedBlueprintData, BlockPos anchorPos, BlockPos boxPos,
                                RotationMirror rotationMirror, BlueprintHandler.BuildState buildState) {
        this.compressedBlueprintData = compressedBlueprintData;
        this.anchorPos = anchorPos;
        this.boxPos = boxPos;
        this.rotationMirror = rotationMirror;
        this.buildState = buildState;
    }

    // 原有构造函数 - 用于创建新包
    public BlueprintBuildPacket(String blueprintData, BlockPos anchorPos, BlockPos boxPos,
                                RotationMirror rotationMirror, BlueprintHandler.BuildState buildState) {
        this(compressString(blueprintData), anchorPos, boxPos, rotationMirror, buildState);

        // 检查压缩后数据大小
        if (compressedBlueprintData.length > MAX_COMPRESSED_LENGTH) {
            throw new IllegalArgumentException("blueprint.overdata");
        }
    }

    public BlueprintBuildPacket(Blueprint blueprint, BlockPos anchorPos, BlockPos boxPos,
                                RotationMirror rotationMirror, BlueprintHandler.BuildState buildState) {
        this(BlueprintSerializer.blueprintToString(blueprint), anchorPos, boxPos, rotationMirror, buildState);
    }

    // Getter 方法
    public byte[] getCompressedBlueprintData() {
        return compressedBlueprintData;
    }

    public BlockPos getAnchorPos() {
        return anchorPos;
    }

    public BlockPos getBoxPos() {
        return boxPos;
    }

    public RotationMirror getRotationMirror() {
        return rotationMirror;
    }

    public BlueprintHandler.BuildState getBuildState() {
        return buildState;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        Level level = player.level();

        // 1. 解压蓝图数据
        String blueprintData = decompressToString(compressedBlueprintData);

        // 2. 反序列化蓝图
        Blueprint blueprint = BlueprintSerializer.blueprintFromString(blueprintData, level);
        if (blueprint == null) {
            player.sendSystemMessage(Component.translatable("blueprint.parse_failed"));
            return;
        }

        // 3. 处理建造逻辑
        BlueprintHandler.BuildState result = BlueprintHandler.buildingBlock(
                this.buildState,
                blueprint,
                level,
                anchorPos,
                boxPos,
                rotationMirror,
                player
        );

        // 4. 返回建造结果
        context.reply(new BuildResultPacket(
                result.isComplete(),
                result.getFailureType(),
                result.getProblemPos(),
                result.getProcessedCount(),
                result.getTotalBlocks(),
                result.getRequiredState()
        ));
    }

    // ========== 内部编解码器类 ==========

    static class RotationMirrorCodec {
        public static final StreamCodec<RegistryFriendlyByteBuf, RotationMirror> STREAM_CODEC =
                StreamCodec.of(
                        FriendlyByteBuf::writeEnum,
                        buf -> buf.readEnum(RotationMirror.class)
                );
    }

    static class BuildStateCodec {
        public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintHandler.BuildState> STREAM_CODEC =
                StreamCodec.of(
                        (buf, state) -> {
                            buf.writeBoolean(state.isComplete());
                            buf.writeEnum(state.getFailureType());
                            buf.writeNullable(state.getProblemPos(), BlockPos.STREAM_CODEC);
                            buf.writeInt(state.getProcessedCount());
                            buf.writeInt(state.getTotalBlocks());
                            buf.writeNullable(state.getRequiredState(), (b, stateValue) -> {
                                Tag tag = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, stateValue)
                                        .getOrThrow(error -> new EncoderException("Failed to encode BlockState: " + error));
                                b.writeNbt(tag);
                            });
                        },
                        buf -> {
                            BlueprintHandler.BuildState state = new BlueprintHandler.BuildState();
                            state.setComplete(buf.readBoolean());
                            state.setFailureType(buf.readEnum(BlueprintHandler.BuildFailureType.class));
                            state.setProblemPos(buf.readNullable(BlockPos.STREAM_CODEC));
                            state.setProcessedCount(buf.readInt());
                            state.setTotalBlocks(buf.readInt());
                            state.setRequiredState(buf.readNullable(b -> {
                                Tag tag = b.readNbt();
                                if (tag == null) return null;
                                return BlockState.CODEC.parse(NbtOps.INSTANCE, tag)
                                        .getOrThrow(error -> new DecoderException("Failed to decode BlockState: " + error));
                            }));
                            return state;
                        }
                );
    }
}