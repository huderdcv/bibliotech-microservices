package com.stargazing.bibliotech.loanservice.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.ISBN;

public record BorrowBookRequest(

  @Schema(
    description = "International Standard Book Number (10 or 13 digits)",
    example = "978-0134685991",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank(message = "ISBN must not be blank")
  @ISBN(message = "Must be a valid ISBN-10 or ISBN-13 format")
  String bookIsbn,

  @Schema(
    description = "Identifier of the user",
    example = "user-12345",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank(message = "Member ID must not be blank")
  @Pattern(
    regexp = "^user-\\d+$",
    message = "Member ID must start with 'user-' followed by numbers (e.g., user-12345)"
  )
  String memberId
) {
}
