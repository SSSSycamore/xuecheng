package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SearchServiceClientFallback implements FallbackFactory<SearchServiceClient> {

    @Override
    public SearchServiceClient create(Throwable throwable) {
        return new SearchServiceClient() {
            @Override
            public Boolean add(CourseIndex courseIndex) {
                log.error("远程调用搜索服务add方法发生熔断,CourseIndex:{},异常信息:{}",courseIndex,throwable.toString(),throwable);
                return false;
            }
        };
    }
}
