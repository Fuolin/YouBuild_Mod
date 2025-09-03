package com.youbuild.command;

import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static com.mojang.text2speech.Narrator.LOGGER;

public class FolderMover {
    static {LogUtils.getLogger();}
    /**
     * 移动文件夹
     * @param sourcePath 源文件夹路径
     * @param targetPath 目标文件夹路径
     * @return 移动成功返回true，否则返回false
     */
    public static boolean moveFolder(Path sourcePath, Path targetPath) {
        // 检查源文件夹是否存在
        if (!Files.exists(sourcePath) || !Files.isDirectory(sourcePath)) {
            LOGGER.error("源文件夹不存在或不是一个目录: {}", sourcePath);
            return false;
        }

        try {
            // 创建目标文件夹的父目录（如果不存在）
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }

            // 移动文件夹
            Files.move(sourcePath, targetPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            LOGGER.info("成功将文件夹从 {} 移动到 {}", sourcePath, targetPath);
            return true;
        } catch (IOException e) {
            LOGGER.error("移动文件夹时发生错误", e);
            return false;
        }
    }
}
