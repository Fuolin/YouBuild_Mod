package com.youbuild.network;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.youbuild.blueprinthandler.BlueprintHandler;
import com.youbuild.command.CommandExecutor;
import com.youbuild.util.BlueprintSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.youbuild.network.Compress.compressString;
import static com.youbuild.network.Compress.decompressToString;

public class BlueprintComparisonPacket implements CustomPacketPayload {

    // 配置参数
    private static final int MAX_COMPRESSED_LENGTH = 30000;  // 压缩蓝图数据最大长度
    private static final int MAX_FILENAME_LENGTH = 256;      // 文件名最大长度


    public static final Type<BlueprintComparisonPacket> TYPE =
            new Type<>(ResourceLocation.parse("youbuild:blueprint_comparison"));


    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintComparisonPacket> STREAM_CODEC =
            StreamCodec.composite(
                    // 1. 压缩蓝图数据 (字节数组)
                    StreamCodec.of(
                            (buf, bytes) -> buf.writeByteArray(bytes),
                            buf -> buf.readByteArray(MAX_COMPRESSED_LENGTH)
                    ),
                    BlueprintComparisonPacket::getCompressedBlueprintData,

                    // 2. 锚点位置
                    BlockPos.STREAM_CODEC,
                    BlueprintComparisonPacket::getAnchorPos,

                    // 3. 旋转镜像
                    RotationMirrorCodec.STREAM_CODEC,
                    BlueprintComparisonPacket::getRotationMirror,

                    // 4. 文件名 (字符串)
                    StreamCodec.of(
                            (buf, fileName) -> buf.writeUtf(fileName, MAX_FILENAME_LENGTH),
                            buf -> buf.readUtf(MAX_FILENAME_LENGTH)
                    ),
                    BlueprintComparisonPacket::getFileName,

                    // 构造函数 - 参数顺序必须与上面字段顺序匹配
                    BlueprintComparisonPacket::new
            );

    // 字段定义
    private final byte[] compressedBlueprintData;  // 压缩后的蓝图数据
    private final BlockPos anchorPos;              // 锚点位置
    private final RotationMirror rotationMirror;   // 旋转镜像
    private final String fileName;                 // 文件名

    // 新增的构造函数 - 用于编解码器
    public BlueprintComparisonPacket(byte[] compressedBlueprintData, BlockPos anchorPos,
                                     RotationMirror rotationMirror, String fileName) {
        this.compressedBlueprintData = compressedBlueprintData;
        this.anchorPos = anchorPos;
        this.rotationMirror = rotationMirror;
        this.fileName = fileName;
    }

    // 原有构造函数 - 用于创建新包
    public BlueprintComparisonPacket(String blueprintData, BlockPos anchorPos,
                                     RotationMirror rotationMirror, String fileName) {
        this(compressString(blueprintData), anchorPos, rotationMirror, fileName);

        if (compressedBlueprintData.length > MAX_COMPRESSED_LENGTH) {
            throw new IllegalArgumentException("blueprint.overdata");
        }
    }

    public BlueprintComparisonPacket(Blueprint blueprint, BlockPos anchorPos,
                                     RotationMirror rotationMirror, String fileName) {
        this(BlueprintSerializer.blueprintToString(blueprint), anchorPos, rotationMirror, fileName);
    }

    // Getter 方法
    public byte[] getCompressedBlueprintData() {
        return compressedBlueprintData;
    }

    public BlockPos getAnchorPos() {
        return anchorPos;
    }

    public RotationMirror getRotationMirror() {
        return rotationMirror;
    }

    public String getFileName() {
        return fileName;
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

        // 3. 比较蓝图
        List<BlockPos> mismatches = BlueprintHandler.findMismatchedBlocksServer(
                blueprint,
                level,
                anchorPos,
                rotationMirror
        );

        // 4. 如果匹配则执行命令文件
        if (mismatches.isEmpty()) {
            CommandExecutor.executeCommandsFromFile(fileName);
        }

        // 5. 返回比较结果
        context.reply(new ComparisonResultPacket(mismatches));
    }

static class RotationMirrorCodec {
    public static final StreamCodec<FriendlyByteBuf, RotationMirror> STREAM_CODEC =
            StreamCodec.of(
                    FriendlyByteBuf::writeEnum,
                    buf -> buf.readEnum(RotationMirror.class)
            );
    }
}