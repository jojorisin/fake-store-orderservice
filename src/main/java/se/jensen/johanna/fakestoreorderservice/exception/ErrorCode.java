package se.jensen.johanna.fakestoreorderservice.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
  INVALID_WEBHOOK,
  PRODUCT_NOT_FOUND,
  SERVICE_ERROR,
  PAYMENT_PROVIDER_ERROR,
  INVALID_ORDER_STATE,
  INVALID_INPUT,
  INTERNAL_SERVER_ERROR


}
