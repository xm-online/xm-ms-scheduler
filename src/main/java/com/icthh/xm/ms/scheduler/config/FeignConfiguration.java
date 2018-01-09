package com.icthh.xm.ms.scheduler.config;

import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.icthh.xm.ms.scheduler")
public class FeignConfiguration {

}
