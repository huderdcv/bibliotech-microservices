package com.stargazing.bibliotech.loanservice.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private final Clock clock;

  // ===================================================================================
  // 1. CUSTOM BUSINESS EXCEPTIONS
  // ===================================================================================
  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleNotFoundResource(ResourceNotFoundException exception, HttpServletRequest request) {
    log.warn("Resource not found on path {}: {}", request.getRequestURI(), exception.getMessage());
    return buildProblemDetail(
      HttpStatus.NOT_FOUND,
      "Resource not found",
      exception.getMessage()
    );
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ProblemDetail handleDuplicateResource(DuplicateResourceException exception, HttpServletRequest request) {
    log.warn("Duplicate resource conflict on path {}: {}", request.getRequestURI(), exception.getMessage());
    return buildProblemDetail(
      HttpStatus.CONFLICT,
      "Duplicate resource",
      exception.getMessage()
    );
  }

  @ExceptionHandler(ServiceUnavailableException.class)
  public ProblemDetail handleServiceUnavailable(ServiceUnavailableException exception, HttpServletRequest request) {
    log.error("Downstream service failure on path {}: {}", request.getRequestURI(), exception.getMessage());

    return buildProblemDetail(
      HttpStatus.SERVICE_UNAVAILABLE,
      "Service unavailable",
      exception.getMessage()
    );
  }

  @ExceptionHandler(BookUnavailableException.class)
  public ProblemDetail handleBookUnavailable(BookUnavailableException exception, HttpServletRequest request) {
    log.warn("Book unavailable on path {}: {}", request.getRequestURI(), exception.getMessage());

    return buildProblemDetail(
      HttpStatus.CONFLICT,
      "Book Unavailable",
      exception.getMessage()
    );
  }

  // ===================================================================================
  // 2. SPRING MVC: OVERRIDES (Payload & Validation Errors)
  // ===================================================================================

  // Handles malformed JSON requests or enum mismatches in the request body.
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
    HttpMessageNotReadableException ex,
    HttpHeaders headers,
    HttpStatusCode status,
    WebRequest request
  ) {
    log.warn("JSON parsing error: {}", ex.getMessage());

    ProblemDetail problem = buildProblemDetail(
      HttpStatus.BAD_REQUEST,
      "Malformed JSON request",
      "The format of the submitted data is incorrect or contains invalid values."
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  // Handles Jakarta Validation failures (@NotBlank, @NotNull, etc.) and extracts field errors.
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
    MethodArgumentNotValidException ex,
    HttpHeaders headers,
    HttpStatusCode status,
    WebRequest request
  ) {
    // create the ProblemDetail
    ProblemDetail problem = buildProblemDetail(
      HttpStatus.BAD_REQUEST,
      "Input validation errors",
      "Data validation failed."
    );

    // extract the errors dynamically
    Map<String, String> errors = ex.getBindingResult()
      .getFieldErrors()
      .stream()
      .collect(Collectors.toMap(
        FieldError::getField,
        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
        (existing, replacement) -> existing
      ));

    // add the errors to ProblemDetail
    problem.setProperty("errors", errors);

    // answer
    log.warn("Payload validation failed. Errors: {}", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);

  }

  // ===================================================================================
  // 3. SPRING WEB & DATA: URL PARAMETER ERRORS
  // ===================================================================================

  // Handles Spring Data errors when sorting by a column that doesn't exist (e.g., ?sort=fakeColumn).
//  @ExceptionHandler(PropertyReferenceException.class)
//  public ProblemDetail handlePropertyReferenceError(PropertyReferenceException ex) {
//    log.warn("Invalid sorting parameter provided: {}", ex.getMessage());
//    return buildProblemDetail(
//      HttpStatus.BAD_REQUEST,
//      "Invalid sort parameter",
//      "The provided sort parameter is invalid."
//    );
//  }

  // Handles Spring Web errors when URL parameters fail type conversion (e.g., text instead of number, '?page=abc').
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ProblemDetail handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
    log.warn("Invalid request parameter type provided: {}", ex.getMessage());

    return buildProblemDetail(
      HttpStatus.BAD_REQUEST,
      "Invalid parameter format",
      "The parameter '" + ex.getName() + "' has an invalid format."
    );
  }

  // ===================================================================================
  // 4. DATABASE EXCEPTIONS
  // ===================================================================================

  // Handles database constraint violations (e.g., duplicate unique keys).
//  @ExceptionHandler(DataIntegrityViolationException.class)
//  public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
//    log.warn("Database constraint violation: {}", ex.getMessage());
//    return buildProblemDetail(
//      HttpStatus.CONFLICT,
//      "Data integrity violation",
//      "The operation cannot be completed due to a data conflict in the system."
//    );
//  }

  // ===================================================================================
  // 4. JAVA CORE
  // ===================================================================================

  // Handles standard Java exceptions manually thrown by defensive checks in the service layer.
  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  public ProblemDetail handleBadRequest(RuntimeException ex, HttpServletRequest request) {
    log.warn("Business rule violation on path {}: {}", request.getRequestURI(), ex.getMessage());
    return buildProblemDetail(
      HttpStatus.BAD_REQUEST,
      "Invalid request",
      ex.getMessage());
  }

  // ===================================================================================
  // 5. GLOBAL FALLBACK (500 Internal Server Error)
  // ===================================================================================

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGeneralError(Exception ex) {
    log.error("Unexpected internal server error: ", ex);
    return buildProblemDetail(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "Internal server error",
      "An internal server error occurred. Please contact support."
    );
  }

  // ===================================================================================
  // 6. UTILITY METHODS
  // ===================================================================================
  private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setProperty("timestamp", Instant.now(clock));
    return problem;
  }
}
