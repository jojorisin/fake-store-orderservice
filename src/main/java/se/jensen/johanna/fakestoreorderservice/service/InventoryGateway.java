package se.jensen.johanna.fakestoreorderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import se.jensen.johanna.fakestoreorderservice.client.InventoryClient;
import se.jensen.johanna.fakestoreorderservice.dto.ReservationRequest;
import se.jensen.johanna.fakestoreorderservice.exception.infra.InternalClientException;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryGateway {

  private final InventoryClient inventoryClient;

  public void reserveCart(ReservationRequest request) {
    log.debug("Reserving cart {} for order {}...", request.cartItemRequests(),
        request.orderId());
    try {
      inventoryClient.reserveCart(request);
    } catch (RestClientException e) {
      throw new InternalClientException("Unable to reserve cart for order.", e);
    }
  }

}
