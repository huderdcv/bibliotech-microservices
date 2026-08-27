package com.stargazing.bibliotech.loanservice.loan.enums;

public enum LoanStatus {
  PENDING,  // Waiting for Catalog service confirmation
  ACTIVE,   // Successfully reserved and loaned
  FAILED,   // Catalog reservation failed (rollback state)
  RETURNED, // Book has been returned
  OVERDUE   // Past due date
}
