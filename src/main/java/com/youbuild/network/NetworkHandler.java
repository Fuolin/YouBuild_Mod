package com.youbuild.network;

import com.youbuild.YouBuild;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {
    // 接收模组事件总线并注册网络包
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NetworkHandler::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(YouBuild.ModId);

        // 注册蓝图比较包（客户端 -> 服务器）
        registrar.playToServer(
                BlueprintComparisonPacket.TYPE,
                BlueprintComparisonPacket.STREAM_CODEC,
                BlueprintComparisonPacket::handle
        );

        // 注册比较结果包（服务器 -> 客户端）
        registrar.playToClient(
                ComparisonResultPacket.TYPE,
                ComparisonResultPacket.STREAM_CODEC,
                (comparisonResultPacket, context) -> comparisonResultPacket.handle()
        );

        // 注册蓝图建造请求包（客户端 -> 服务器）
        registrar.playToServer(
                BlueprintBuildPacket.TYPE,
                BlueprintBuildPacket.STREAM_CODEC,
                BlueprintBuildPacket::handle
        );

        // 注册建造结果返回包（服务器 -> 客户端）
        registrar.playToClient(
                BuildResultPacket.TYPE,
                BuildResultPacket.STREAM_CODEC,
                BuildResultPacket::handle
        );
    }
}