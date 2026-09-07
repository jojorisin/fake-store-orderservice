package se.jensen.johanna.fakestoreorderservice.exception;

public class InvalidWebhookSignatureException extends RuntimeException {

  public InvalidWebhookSignatureException(String message) {
    super(message);
  }
}
