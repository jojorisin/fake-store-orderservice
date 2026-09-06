package se.jensen.johanna.fakestoreorderservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {
  ILLEGAL_STATE(HttpStatus.INTERNAL_SERVER_ERROR),
  CHECKOUT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
  UNSUPPORTED_PAYMENT_TYPE(HttpStatus.INTERNAL_SERVER_ERROR),
  ;

  private final HttpStatus status;

  ErrorType(HttpStatus status) {
    this.status = status;
  }
}
