package com.xuecheng.content.feignclient;

import com.xuecheng.base.exception.XueChengPlusException;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Component
public class MediaServiceClientFallback implements FallbackFactory<MediaServiceClient> {

    @Override
    public MediaServiceClient create(Throwable throwable) {
        return new MediaServiceClient() {
            @Override
            public String upload(MultipartFile filedata, String folder, String objectName) throws IOException {
                log.error("MediaServiceClient发生熔断异常,异常原因:",throwable);
                return null;
            }
        };
    }
}
