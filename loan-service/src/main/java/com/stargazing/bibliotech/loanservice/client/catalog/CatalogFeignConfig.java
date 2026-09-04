package com.stargazing.bibliotech.loanservice.client.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class CatalogFeignConfig {

  @Bean
  public ErrorDecoder catalogErrorDecoder(ObjectMapper objectMapper) {
    return new CatalogFeignErrorDecoder(objectMapper);
  }
}
