package com.stargazing.bibliotech.loanservice.client.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stargazing.bibliotech.loanservice.client.catalog.exception.CatalogBadRequestException;
import com.stargazing.bibliotech.loanservice.client.catalog.exception.CatalogConflictException;
import com.stargazing.bibliotech.loanservice.client.catalog.exception.CatalogNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;

import java.io.InputStream;

@Slf4j
@RequiredArgsConstructor
public class CatalogFeignErrorDecoder implements ErrorDecoder {

  private final ObjectMapper objectMapper;
  private final ErrorDecoder defaultErrorDecoder = new Default();

  @Override
  public Exception decode(String methodKey, Response response) {
    log.error("CatalogClient error - Method: {}, HTTP Status: {}", methodKey, response.status());

    try (InputStream bodyIs = response.body().asInputStream();) {
      // 1. Open the response body stream
      
      // 2. Parse the JSON from catalog's GlobalExceptionHandler into a ProblemDetail object
      ProblemDetail problem = objectMapper.readValue(bodyIs, ProblemDetail.class);
      String detailMessage = problem.getDetail() != null ? problem.getDetail() : "Unknown Catalog Error";

      log.warn("Catalog returned error: Title='{}', Detail='{}'", problem.getTitle(), problem.getDetail());

      // 3. Translate specific Catalog errors into Loan Service domain exceptions
      return switch (response.status()) {
        case 404 -> new CatalogNotFoundException("Catalog rejected: " + detailMessage);
        case 409 -> new CatalogConflictException("Catalog conflict: " + detailMessage);
        case 400 -> new CatalogBadRequestException("Invalid Catalog request: " + detailMessage);
        default -> defaultErrorDecoder.decode(methodKey, response);
      };
    } catch (Exception e) {
      // If the body is empty or not JSON, fallback to default behavior
      log.error("Failed to parse error response from Catalog Service", e);
      return defaultErrorDecoder.decode(methodKey, response);
    }
  }
}
