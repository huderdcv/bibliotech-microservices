package com.stargazing.bibliotech.loanservice.client.catalog;

import com.stargazing.bibliotech.loanservice.client.catalog.dto.BookResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
  name = "catalog-service",
  configuration = CatalogFeignConfig.class
)
public interface CatalogClient {

  @PostMapping("/api/v1/catalog/books/{isbn}/reserve")
  BookResponse reserveOne(@PathVariable("isbn") String isbn);

  @PostMapping("/api/v1/catalog/books/{isbn}/return")
  BookResponse returnOne(@PathVariable("isbn") String isbn);

}