package se.jensen.johanna.fakestoreorderservice.exception;

import org.springframework.http.HttpStatus;
import se.jensen.johanna.fakestoreorderservice.exception.infra.InfrastructureException;

public class InvalidWebhookSignatureException extends InfrastructureException {

  public InvalidWebhookSignatureException(String message, HttpStatus status) {
    super(message, ErrorCode.INVALID_WEBHOOK_SIGNATURE, status);
  }

  public InvalidWebhookSignatureException(String message, HttpStatus status, Throwable cause) {
    super(message, ErrorCode.INVALID_WEBHOOK_SIGNATURE, status, cause);
  }
}
