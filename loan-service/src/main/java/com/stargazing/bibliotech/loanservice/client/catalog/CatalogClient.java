package com.stargazing.bibliotech.loanservice.client.catalog;

import com.stargazing.bibliotech.loanservice.client.catalog.dto.BookResponse;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient("catalog-service")
public interface CatalogClient {

  @PostMapping("/api/v1/catalog/books/{isbn}/reserve")
  BookResponse reserveOne(@PathVariable("isbn") String isbn);

  @PostMapping("/api/v1/catalog/books/{isbn}/return")
  BookResponse returnOne(@PathVariable("isbn") String isbn);
}