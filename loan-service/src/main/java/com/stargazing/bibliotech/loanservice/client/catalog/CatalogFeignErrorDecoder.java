package com.stargazing.bibliotech.loanservice.client.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stargazing.bibliotech.loanservice.common.exception.BookUnavailableException;
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

      log.warn("Catalog returned error: Title='{}', Detail='{}'", problem.getTitle(), problem.getDetail());

      // 3. Translate specific Catalog errors into Loan Service domain exceptions
      return switch (response.status()) {
        case 404 -> new BookUnavailableException("Catalog rejected: " + problem.getDetail());
        case 409 -> new BookUnavailableException("Catalog conflict: " + problem.getDetail());
        case 400 -> new BookUnavailableException("Invalid Catalog request: " + problem.getDetail());
        default -> defaultErrorDecoder.decode(methodKey, response);
      };
    } catch (Exception e) {
      // If the body is empty or not JSON, fallback to default behavior
      log.error("Failed to parse error response from Catalog Service", e);
      return defaultErrorDecoder.decode(methodKey, response);
    }
  }
}
