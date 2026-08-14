package com.stargazing.bibliotech.loanservice.common.exception;

public class DuplicateResourceException extends RuntimeException {
  public DuplicateResourceException(String message) {
    super(message);
  }
}
