package se.jensen.johanna.fakestoreorderservice.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;
import se.jensen.johanna.fakestoreorderservice.dto.ReservationRequest;

public interface InventoryClient {

  @PostExchange("/reservations/reserve-cart")
  void reserveCart(@RequestBody ReservationRequest request);

}
