package com.youbuild.network;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Compress {
    private static final int BUFFER_SIZE = 1024; // 压缩/解压缓冲区大小

    // ========== 压缩/解压缩方法 ==========
    static byte[] compressString(String data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] input = data.getBytes(StandardCharsets.UTF_8);
            Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
            deflater.setInput(input);
            deflater.finish();

            byte[] buffer = new byte[BUFFER_SIZE];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                baos.write(buffer, 0, count);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalArgumentException("blueprint.compress_error");
        }
    }

    /**
     * 解压字节数组为字符串
     */
    static String decompressToString(byte[] compressedData) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Inflater inflater = new Inflater();
            inflater.setInput(compressedData);

            byte[] buffer = new byte[BUFFER_SIZE];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                baos.write(buffer, 0, count);
            }
            return baos.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("blueprint.decompress_error");
        }
    }
}
