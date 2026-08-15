package com.stargazing.bibliotech.loanservice.common.config.openfeign;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.stargazing.bibliotech.loanservice.client")
public class FeignClientConfig {
}
