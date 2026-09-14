package se.jensen.johanna.fakestoreorderservice.exception.infra;

import se.jensen.johanna.fakestoreorderservice.exception.ErrorCode;

public class InvalidPaymentWebhookException extends InfrastructureException {

  public InvalidPaymentWebhookException(String message) {
    super(message, ErrorCode.INVALID_WEBHOOK);
  }

  public InvalidPaymentWebhookException(String message, Throwable cause) {
    super(message, ErrorCode.INVALID_WEBHOOK, cause);
  }
}
