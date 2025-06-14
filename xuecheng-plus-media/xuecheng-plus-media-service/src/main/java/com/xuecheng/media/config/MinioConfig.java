package com.xuecheng.media.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mr.M
 * @version 1.0
 * @description minio配置
 * @date 2022/9/12 19:32
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    private String endpoint;
    private String accessKey;
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        // 从配置文件中获取Minio的连接信息
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint(endpoint)
                        .credentials(accessKey, secretKey)
                        .build();
        return minioClient;
    }
}