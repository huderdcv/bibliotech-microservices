package com.stargazing.bibliotech.loanservice.client.catalog.exception;

public abstract class CatalogClientException extends RuntimeException {
  protected CatalogClientException(String message) {
    super(message);
  }
}
