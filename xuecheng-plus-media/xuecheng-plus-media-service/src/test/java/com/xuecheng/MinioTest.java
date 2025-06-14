package com.xuecheng;

import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MinioTest {
    MinioClient minioClient = MinioClient.builder()
            .endpoint("http://192.168.100.128:9000")
            .credentials("minioadmin", "minioadmin")
            .build();

    @Test
    public void test_upload() throws Exception {
        UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                .bucket("mediafiles")
                .object("test.mp4")
                .filename("D:\\Google chroem\\275498_medium.mp4")
                .build();
        minioClient.uploadObject(uploadObjectArgs);
    }

    @Test
    public void test_delete() throws Exception {
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket("mediafiles")
                .object("test.mp4")
                .build();
        minioClient.removeObject(removeObjectArgs);
    }

    @Test
    public void test_download() throws Exception {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket("mediafiles")
                .object("test.mp4")
                .build();
        GetObjectResponse object = minioClient.getObject(getObjectArgs);
        // 这里可以将获取到的对象写入到文件中
        // 例如使用OutputStream将其写入到本地文件
        OutputStream outputStream = new FileOutputStream(new File("D:\\downloaded_test.mp4"));
        IOUtils.copy(object, outputStream);
    }

    @Test
    public void test_chunkUpload(){
        // 定义分块文件夹
        String chunkFileFolderPath = "chunkFiles";
        String localChunkFilePath = "E:\\chunkFiles\\";
        for(int i = 0;i < 73; i++){
            String chunkFileName = localChunkFilePath + i;
            try {
                UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                        .bucket("mediafiles")
                        .object("chunkFiles/" + i)
                        .filename(chunkFileName)
                        .build();
                minioClient.uploadObject(uploadObjectArgs);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void test_mergeMinio() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        List<ComposeSource> sourceList = new ArrayList<>();
        for(int i = 0; i < 73; i++){
            ComposeSource composeSource = ComposeSource.builder()
                    .bucket("mediafiles")
                    .object("chunkFiles/" + i)
                    .build();
            sourceList.add(composeSource);
        }
        ComposeObjectArgs mediafiles = ComposeObjectArgs.builder()
                .bucket("mediafiles")
                .object("merged_video.mp4")
                .sources(sourceList)
                .build();
        minioClient.composeObject(mediafiles);

        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket("mediafiles")
                .object("merged_video.mp4")
                .build();
        GetObjectResponse object = minioClient.getObject(getObjectArgs);
        // 将合并后的文件写入到本地
        String md5_minio = DigestUtils.md5Hex(object);
        String md5_local = DigestUtils.md5Hex(Files.newInputStream(new File("E:\\Videos\\NARAKA  BLADEPOINT\\NARAKA  BLADEPOINT 2024.06.25 - 01.34.03.06.DVR.mp4").toPath()));
        if (md5_minio.equals(md5_local)) {
            System.out.println("MD5校验通过，文件合并成功！");
        } else {
            System.out.println("MD5校验失败，文件合并可能存在问题！");
        }

    }
}
