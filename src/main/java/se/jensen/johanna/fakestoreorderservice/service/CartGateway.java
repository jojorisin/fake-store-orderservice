package se.jensen.johanna.fakestoreorderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import se.jensen.johanna.fakestoreorderservice.client.CartClient;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutCartResponse;
import se.jensen.johanna.fakestoreorderservice.exception.domain.EmptyCartException;
import se.jensen.johanna.fakestoreorderservice.exception.infra.InternalClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartGateway {

  private final CartClient cartClient;

  public CheckoutCartResponse getCartForCheckout() {
    log.debug("fetching cart for checkout...");
    CheckoutCartResponse response;
    try {
      response = cartClient.getCartForCheckout();
    } catch (RestClientException e) {
      throw new InternalClientException("Unable to fetch cart.", e);
    }
    if (response == null || response.checkoutCart() == null) {
      throw new InternalClientException("Cart client returned null when fetching cart.");
    }
    if (response.checkoutCart().isEmpty()) {
      throw new EmptyCartException("Cart is empty.");
    }
    return response;
  }

}
