package se.jensen.johanna.fakestoreorderservice.client;

import org.springframework.web.service.annotation.GetExchange;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutCartResponse;

public interface CartClient {

  @GetExchange("/internal/cart/checkout-cart")
  CheckoutCartResponse getCartForCheckout();


}
