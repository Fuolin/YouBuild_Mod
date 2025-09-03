package com.youbuild.command;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class CommandExecutor {
    // 确保目录存在的辅助方法
    public static void ensureDirectoryExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    // 主执行方法
    public static void executeCommandsFromFile(String fileName) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            System.out.println("Server is not online");
            return;
        }

        try {
            // 获取服务端根目录
            Path serverRoot = server.getServerDirectory();

            // 确保目录存在
            Path dirPath = serverRoot.resolve("youbuild/commands/");
            ensureDirectoryExists(dirPath);

            // 获取文件路径
            Path filePath = dirPath.resolve(fileName + ".txt");

            if (!Files.exists(filePath)) {
                server.getPlayerList().broadcastSystemMessage(
                        Component.translatable("command.file_not_found",fileName),false);
                //创建文件
                Files.createFile(filePath);
                return;
            }

            // 读取文件内容
            List<String> lines = Files.readAllLines(filePath);

            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("command.structure_complete"), false);

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();

                // 跳过空行和注释
                if (line.isEmpty() || line.startsWith("#")) continue;

                // 检查指令是否未执行
                if (line.startsWith("[onetime]")||line.startsWith("[forever]")) {
                    String command = line.substring(9).trim();

                    try {
                        // 执行指令
                        server.getCommands().performPrefixedCommand(
                                server.createCommandSourceStack(),
                                command
                        );

                        if(line.startsWith("[onetime]")){
                            // 更新标记
                            lines.set(i, "#[onetime]" + command);
                            Files.write(filePath, lines, StandardOpenOption.TRUNCATE_EXISTING);
                        }

                    } catch (Exception e) {
                        server.getPlayerList().broadcastSystemMessage(
                                Component.translatable("command.execution_failed" , command , e.getMessage()), false);
                    }
                }
            }
        } catch (IOException e) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("command.file_io_error",e.getMessage()), false);
        }
    }
}