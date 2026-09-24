package com.stargazing.bibliotech.gateway_server.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class GatewayRoutingConfig {

  @Bean
  public RouteLocator bibliotechRouteConfig(RouteLocatorBuilder builder) {
    return builder.routes()
      // Route for loan
      .route(r -> r
        .path("/bibliotech/loans/**")
        .filters(f -> f.rewritePath("/bibliotech/loans/(?<segment>.*)", "/${segment}")
          .addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
        )
        .uri("lb://LOAN-SERVICE")
      )
      // Route for catalog
      .route(r -> r
        .path("/bibliotech/catalogs/**")
        .filters(f -> f.rewritePath("/bibliotech/catalogs/(?<segment>.*)", "/${segment}")
          .addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
        )
        .uri("lb://CATALOG-SERVICE")
      )
      .build();
  }
}
