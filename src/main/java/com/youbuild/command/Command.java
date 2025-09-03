package com.youbuild.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Command {
    // 注册
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("movefolder")
                .requires(source -> source.hasPermission(2)) // 需要管理员权限(权限等级2)
                .then(Commands.argument("sourcePath", StringArgumentType.string())
                        .then(Commands.argument("targetPath", StringArgumentType.string())
                                .executes(Command::execute))));
    }

    // 执行命令
    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        //尝试获取玩家信息
        ServerPlayer player = null;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            // 允许非玩家来源（如服务端控制台）执行
        }

        // 获取服务端实例
        MinecraftServer server = context.getSource().getServer();

        // 获取服务端根目录
        Path serverRoot = server.getServerDirectory();

        // 获取指令参数
        String sourceSub = StringArgumentType.getString(context, "sourcePath").replace(".","/");
        String targetSub = StringArgumentType.getString(context, "targetPath").replace(".","/");

        // 添加根目录前缀，确保操作限制在MC根目录内
        Path sourceRoot = serverRoot.resolve("youbuild/hide/");
        Path targetRoot = serverRoot.resolve("blueprints/");

        // 构建source完整路径
        Path fullSourcePath = sourceRoot.resolve(sourceSub).normalize();

        //获取name
        String name = sourceSub;//默认值
        Path sourceNamePath = sourceRoot.resolve(sourceSub).resolve("name.txt");
        if(Files.exists(sourceNamePath)) {
            try {
                name=Files.readAllLines(sourceNamePath).getFirst().trim();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        //构建targe完整路径
        Path fullTargetPath = targetRoot.resolve(targetSub).resolve(name).normalize();

        // 路径安全验证
        if (!fullSourcePath.startsWith(sourceRoot) ||
                !fullTargetPath.startsWith(targetRoot)) {
            context.getSource().sendFailure(Component.translatable("foldermover.path_traversal"));
            return 0;
        }

        // 执行文件夹移动
        boolean success = FolderMover.moveFolder(fullSourcePath, fullTargetPath);

        // 向玩家发送结果消息
        if (success) {
            Component successMsg = Component.translatable("foldermover.success",name);
            if (player != null) {
                player.sendSystemMessage(successMsg); // 玩家执行时发送到聊天框
            } else {
                source.sendSuccess(() -> successMsg, true); // 控制台执行时发送到控制台
            }
        } else {
            Component failMsg = Component.translatable("foldermover.fail");
            if (player != null) {
                player.sendSystemMessage(failMsg);
            } else {
                source.sendFailure(failMsg);
            }
        }
        return 1; // 命令执行成功的返回值
    }

}
