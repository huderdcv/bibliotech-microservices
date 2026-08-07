package com.stargazing.bibliotech.catalogservice.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.ISBN;

@Schema(
  name = "CreateBookRequest",
  description = "Payload required to create a new book entry in the catalog"
)
public record CreateBookRequest(

  @Schema(
    description = "International Standard Book Number (10 or 13 digits)",
    example = "978-0134685991",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank(message = "ISBN must not be blank")
  @ISBN(message = "Must be a valid ISBN-10 or ISBN-13 format")
  String isbn,

  @Schema(
    description = "Full title of the book",
    example = "Effective Java",
    maxLength = 255,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank(message = "Title must not be blank")
  @Size(max = 255, message = "Title cannot exceed 255 characters")
  String title,

  @Schema(
    description = "Author name(s)",
    example = "Joshua Bloch",
    maxLength = 255,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank(message = "Author must not be blank")
  @Size(max = 255, message = "Author name cannot exceed 255 characters")
  String author,

  @Schema(
    description = "Total physical copies owned by the library",
    example = "10",
    minimum = "0",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull(message = "Total copies count is required")
  @Min(value = 0, message = "Total copies cannot be negative")
  Integer totalCopies,

  @Schema(
    description = "Copies currently available for checkout",
    example = "10",
    minimum = "0",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull(message = "Available copies count is required")
  @Min(value = 0, message = "Available copies cannot be negative")
  Integer availableCopies
) {
}