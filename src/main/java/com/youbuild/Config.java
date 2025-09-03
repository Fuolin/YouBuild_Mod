package com.youbuild;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Config {
    private static final String CONFIG_FILE_NAME = "xblocks.toml";//xblocks文件名


    public static void createConfig(Path configPath) {
        //文件路径
        Path tomlPath = configPath.resolve(CONFIG_FILE_NAME);

        //如果存在则不新建
        if(Files.exists(tomlPath))return;

        //初始化配置文件
        try {
            String tomlContent = """
                    #Add: non-contrasting or constructed squares
                    !end,please add in the above section
                    #The commands to be executed after the comparison is completed are stored in the "commands" folder.
                    #The .txt file names must correspond to the .blueprint file names.
                    #Each line of command in the file that starts with [onetime] is executed only once, while that starting with [forever] can be executed repeatedly.
                    #Hidden chapters will be stored in the "hide" folder.
                    #Use the command /movefolder sourcePath targetPath to display them.
                    #The default directory for sourcePath is hide/, and the default directory for targetPath is blueprints/. Use "." to replace "/" that cannot be input.
                    #It will automatically read the first line of the name.txt file in the folder as the folder name.""";
            Files.writeString(
                    tomlPath,
                    tomlContent,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE
            );
            System.out.println("[YouBuild]Configuration file has been created: " + tomlPath);
        } catch (Exception e) {
            System.out.println("[YouBuild]Failed to create the configuration file: " + tomlPath);
        }
    }

    public static List<BlockState> readConfig(){
        //确保服务器运行存在
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if(server==null)return null;

        //获取ConfigPath
        Path serverRoot = server.getServerDirectory();
        Path configDir = serverRoot.resolve("youbuild");
        Path configPath = configDir.resolve(CONFIG_FILE_NAME);
        if(!Files.exists(configPath))createConfig(configDir);

        //获取方块注册表
        Registry<Block> blockRegistry = BuiltInRegistries.BLOCK;
        List<BlockState> xblockState = new ArrayList<>();

        //默认添加的方块
        xblockState.add(Objects.requireNonNull(blockRegistry.get(ResourceLocation.parse("structurize:blocksubstitution"))).defaultBlockState());
        xblockState.add(Objects.requireNonNull(blockRegistry.get(ResourceLocation.parse("structurize:blocksolidsubstitution"))).defaultBlockState());
        xblockState.add(Objects.requireNonNull(blockRegistry.get(ResourceLocation.parse("structurize:blockfluidsubstitution"))).defaultBlockState());

        //循环加载配置文件
        try {
            for(String line : Files.readAllLines(configPath, StandardCharsets.UTF_8)){
                // 跳过空行和注释
                if (line.isEmpty() || line.startsWith("#")) continue;
                if(line.startsWith("!end")) break;
                xblockState.add(Objects.requireNonNull(blockRegistry.get(ResourceLocation.parse(line))).defaultBlockState());
            }
        }catch(IOException ignored){}

        return xblockState;
    }
}

