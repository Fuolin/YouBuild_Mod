package com.youbuild.network;

import com.youbuild.blueprinthandler.BlueprintHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import io.netty.handler.codec.EncoderException;

import java.util.Objects;

public record BuildResultPacket(boolean complete, BlueprintHandler.BuildFailureType failureType, BlockPos problemPos,
                                int processedCount, int totalBlocks,
                                BlockState requiredState) implements CustomPacketPayload {

    public static final Type<BuildResultPacket> TYPE =
            new Type<>(ResourceLocation.parse("youbuild:build_result"));

    //枚举编解码器
    private static final StreamCodec<FriendlyByteBuf, BlueprintHandler.BuildFailureType> FAILURE_TYPE_STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public BlueprintHandler.@NotNull BuildFailureType decode(FriendlyByteBuf buf) {
                    return BlueprintHandler.BuildFailureType.values()[buf.readVarInt()];
                }

                @Override
                public void encode(FriendlyByteBuf buf, BlueprintHandler.BuildFailureType value) {
                    buf.writeVarInt(value.ordinal());
                }
            };

    // BlockState的流编解码器

    static {
        ByteBufCodecs.fromCodecTrusted(BlockState.CODEC);
    }

    // STREAM_CODEC
    public static final StreamCodec<RegistryFriendlyByteBuf, BuildResultPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public @NotNull BuildResultPacket decode(RegistryFriendlyByteBuf buf) {
                    boolean complete = buf.readBoolean();
                    BlueprintHandler.BuildFailureType failureType = FAILURE_TYPE_STREAM_CODEC.decode(buf);
                    BlockPos problemPos = buf.readNullable(BlockPos.STREAM_CODEC);
                    int processedCount = buf.readInt();
                    int totalBlocks = buf.readInt();
                    BlockState requiredState = buf.readNullable(b ->
                            Objects.requireNonNull(BlockState.CODEC.parse(NbtOps.INSTANCE, b.readNbt())
                                    .resultOrPartial(error -> {
                                        throw new EncoderException(error);
                                    })
                                    .orElse(null)));

                    return new BuildResultPacket(
                            complete,
                            failureType,
                            problemPos,
                            processedCount,
                            totalBlocks,
                            requiredState
                    );
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, BuildResultPacket packet) {
                    buf.writeBoolean(packet.complete());
                    FAILURE_TYPE_STREAM_CODEC.encode(buf, packet.failureType());
                    buf.writeNullable(packet.problemPos(), BlockPos.STREAM_CODEC);
                    buf.writeInt(packet.processedCount());
                    buf.writeInt(packet.totalBlocks());
                    buf.writeNullable(packet.requiredState(), (b, state) ->
                            BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state)
                                    .resultOrPartial(error -> {
                                        throw new EncoderException(error);
                                    })
                                    .ifPresent(b::writeNbt)
                    );
                }
            };

    public BuildResultPacket(
            boolean complete,
            BlueprintHandler.BuildFailureType failureType,
            BlockPos problemPos,
            int processedCount,
            int totalBlocks,
            BlockState requiredState
    ) {
        this.complete = complete;
        this.failureType = failureType != null ? failureType : BlueprintHandler.BuildFailureType.NONE;
        this.problemPos = problemPos;
        this.processedCount = processedCount;
        this.totalBlocks = totalBlocks;
        this.requiredState = requiredState;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> com.youbuild.client.ClientHandler.handleBuildResult(
                complete,
                failureType,
                problemPos,
                processedCount,
                totalBlocks,
                requiredState
        ));
    }
}