package com.stargazing.bibliotech.loanservice.loan.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "loan")
public record LoanProperties(

  @NotNull(message = "Loan duration days cannot be null")
  @Min(value = 1, message = "Loan duration days must be at least 1")
  Integer durationDays

) {
}
