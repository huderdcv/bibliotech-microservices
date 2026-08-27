package com.stargazing.bibliotech.loanservice.loan;

import com.stargazing.bibliotech.loanservice.loan.dto.BorrowBookRequest;
import com.stargazing.bibliotech.loanservice.loan.dto.LoanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loan", description = "Endpoints for managing loans records in the catalog")
public class LoanController {

  //- DEPENDENCY INJECTIONS
  private final LoanService loanService;

  //- METHODS

  //-- BORROW A BOOK
  @Operation(
    summary = "Borrow a specific book",
    description = "Creates a new loan record for a patron. Automatically reserves the physical book inventory in the catalog and calculates the return deadline (14 days)."
  )
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "201",
      description = "Loan created successfully",
      headers = @Header(name = "Location", description = "URI of the newly created loan resource", schema = @Schema(type = "string")),
      content = @Content(schema = @Schema(implementation = LoanResponse.class))
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad Request - Invalid payload, validation error, or the requested book is out of stock",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Not Found - The requested Book ISBN does not exist in the catalog",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
  })
  @PostMapping("/borrow")
  public ResponseEntity<LoanResponse> borrowBook(
    @Valid
    @RequestBody
    BorrowBookRequest request
  ) {
    LoanResponse loanResponse = loanService.borrowBook(request);

    // Build Location URI: /api/v1/catalog/books/{id}
    URI location = ServletUriComponentsBuilder
      .fromCurrentContextPath()
      .path("/api/v1/loans/{id}")
      .buildAndExpand(loanResponse.id())
      .toUri();

    return ResponseEntity.created(location).body(loanResponse);
  }
}
