package com.stargazing.bibliotech.loanservice.client.catalog.exception;

public class ServiceUnavailableException extends RuntimeException {
  public ServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
