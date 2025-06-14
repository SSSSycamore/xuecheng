package com.xuecheng;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class BigFileTest {
    @Test
    public void test_chunk() throws IOException {
        File file = new File("E:\\Videos\\NARAKA  BLADEPOINT\\NARAKA  BLADEPOINT 2024.06.25 - 01.34.03.06.DVR.mp4");
        String chunkPath = "E:\\chunkFiles\\";
        int chunkSize = 1024 * 1024 * 5; // 5MB
        int chunkCount = (int) Math.ceil(file.length()*1.0 / chunkSize);
        RandomAccessFile raf_r = new RandomAccessFile(file, "r");
        for (int i = 0; i < chunkCount; i++) {
            String chunkFileName = chunkPath + i;
            RandomAccessFile raf_w = new RandomAccessFile(chunkFileName, "rw");
            byte[] bytes = new byte[1024];
            int bytesRead = -1;
            while ((bytesRead = raf_r.read(bytes)) != -1) {
                raf_w.write(bytes, 0, bytesRead);
                if (raf_w.length() >= chunkSize) {
                    break; // 如果当前分片大小已达到或超过指定大小，则停止读取
                }
            }
            raf_w.close();
        }
        raf_r.close();
    }

    @Test
    public void test_merge() throws IOException {
        File file_source = new File("E:\\Videos\\NARAKA  BLADEPOINT\\NARAKA  BLADEPOINT 2024.06.25 - 01.34.03.06.DVR.mp4");
        String chunkPath = "E:\\chunkFiles\\";
        File file_merge = new File("E:\\file_merge.mp4");
        RandomAccessFile raf_w = new RandomAccessFile("E:\\file_merge.mp4", "rw");
        File[] files = new File(chunkPath).listFiles();
        if (files == null || files.length == 0) {
            System.out.println("No chunk files found to merge.");
            return;
        }
        Arrays.sort(files, (f1, f2) -> f1.getName().length() - f2.getName().length());
        for (File file : files) {
            if (file.isFile()) {
                try (RandomAccessFile raf_r = new RandomAccessFile(file, "r")) {
                    byte[] bytes = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = raf_r.read(bytes)) != -1) {
                        raf_w.write(bytes, 0, bytesRead);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        raf_w.close();
        String md5_source = DigestUtils.md5Hex(Files.newInputStream(file_source.toPath())); // 计算源文件的MD5
        String md5_merge = DigestUtils.md5Hex(Files.newInputStream(file_merge.toPath())); // 计算合并后的文件的MD5
        System.out.println("Source file MD5: " + md5_source);
        System.out.println("Merged file MD5: " + md5_merge);
    }


}
