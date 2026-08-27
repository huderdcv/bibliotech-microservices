package com.stargazing.bibliotech.loanservice.loan.dto;
import com.stargazing.bibliotech.loanservice.loan.enums.LoanStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Details of the successfully created book loan")
public record LoanResponse(

  @Schema(
    description = "Unique identifier of the loan record",
    example = "1"
  )
  Long id,

  @Schema(
    description = "International Standard Book Number reserved in the catalog",
    example = "978-0134685991"
  )
  String bookIsbn,

  @Schema(
    description = "Identifier of the user who borrowed the book",
    example = "user-12345"
  )
  String memberId,

  @Schema(
    description = "The exact date the transaction occurred",
    example = "2026-08-07"
  )
  LocalDate loanDate,

  @Schema(
    description = "The calculated deadline for return (14 days from loan date)",
    example = "2026-08-21"
  )
  LocalDate dueDate,

  @Schema(
    description = "Current lifecycle state of the loan",
    example = "ACTIVE"
  )
  LoanStatus status,

  @Schema(
    description = "UTC timestamp of record creation",
    example = "2026-08-07T21:25:15Z"
  )
  Instant createdAt,

  @Schema(
    description = "UTC timestamp of the last record update",
    example = "2026-08-07T21:25:15Z"
  )
  Instant updatedAt
) {
}