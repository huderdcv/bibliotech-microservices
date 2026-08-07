package com.stargazing.bibliotech.catalogservice.common.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Value("${spring.application.name}")
  private String serviceName;

  @Value("${info.app.version}")
  private String infoVersion;

  @Bean
  public OpenAPI catalogServiceOpenAPI() {
    return new OpenAPI()
      .info(new Info()
        .title("Catalog Service API")
        .description("Core catalog microservice (" + serviceName + ") for the Bibliotech system")
        .version(infoVersion)
        .contact(new Contact()
          .name("Stargazing Dev Team")
          .email("stargazing@gmail.com")
          .url("https://github.com/stargazing/bibliotech")
        )
        .license(new License()
          .name("Apache 2.0")
          .url("http://springdoc.org")
        )
      );
  }
}
