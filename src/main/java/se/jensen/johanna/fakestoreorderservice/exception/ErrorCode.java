package se.jensen.johanna.fakestoreorderservice.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
  INVALID_WEBHOOK,
  SERVICE_ERROR,
  PAYMENT_PROVIDER_ERROR,
  INVALID_ORDER_STATE,
  INVALID_INPUT,
  INTERNAL_CLIENT_ERROR,
  EMPTY_CART,
  ORDER_NOT_FOUND


}
