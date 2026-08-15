package com.stargazing.bibliotech.loanservice.client.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(
  name = "BookResponse",
  description = "Response object containing complete details of a cataloged book"
)
public record BookResponse(

  @Schema(description = "Unique database ID", example = "42")
  Long id,

  @Schema(description = "ISBN number", example = "978-0134685991")
  String isbn,

  @Schema(description = "Book title", example = "Effective Java")
  String title,

  @Schema(description = "Book author", example = "Joshua Bloch")
  String author,

  @Schema(description = "Total registered inventory", example = "10")
  Integer totalCopies,

  @Schema(description = "Copies available for checkout", example = "10")
  Integer availableCopies,

  @Schema(description = "UTC timestamp when the record was created")
  Instant createdAt,

  @Schema(description = "UTC timestamp when the record was last updated")
  Instant updatedAt
) {
}