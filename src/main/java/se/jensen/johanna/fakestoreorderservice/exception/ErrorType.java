package se.jensen.johanna.fakestoreorderservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
  ILLEGAL_STATE(HttpStatus.INTERNAL_SERVER_ERROR),
  PAYMENT_ERROR(HttpStatus.PAYMENT_REQUIRED);

  private final HttpStatus status;

  ErrorType(HttpStatus status) {
    this.status = status;
  }
}
