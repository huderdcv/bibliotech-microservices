package com.stargazing.bibliotech.loanservice.common.exception;

public class BookUnavailableException extends RuntimeException {

  public BookUnavailableException(String message) {
    super(message);
  }

  public BookUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
