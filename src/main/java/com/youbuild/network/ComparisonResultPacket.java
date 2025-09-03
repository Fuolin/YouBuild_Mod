package com.youbuild.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record ComparisonResultPacket(List<BlockPos> mismatches) implements CustomPacketPayload {

    public static final Type<ComparisonResultPacket> TYPE =
            new Type<>(ResourceLocation.parse("youbuild:comparison_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ComparisonResultPacket> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodec.of(
                            // 编码器：明确调用实例方法writeBlockPos
                            (buf, list) -> buf.writeCollection(list, (buffer, pos) -> buffer.writeBlockPos(pos)),
                            // 解码器：明确调用实例方法readBlockPos
                            buf -> buf.readCollection(ArrayList::new, buffer -> buffer.readBlockPos())
                    ),
                    ComparisonResultPacket::mismatches,
                    ComparisonResultPacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        com.youbuild.client.ClientHandler.handleComparisonResult(mismatches);
    }
}
