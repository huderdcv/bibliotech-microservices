package com.stargazing.bibliotech.loanservice.common.exception;

public class ServiceUnavailableException extends RuntimeException {
  public ServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
