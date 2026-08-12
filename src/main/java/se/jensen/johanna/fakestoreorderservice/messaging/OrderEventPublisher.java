package se.jensen.johanna.fakestoreorderservice.messaging;

import java.util.UUID;

public interface OrderEventPublisher {

  /*
   Confirms a cart-reservation has been ordered and paid.
   */
  void publishConfirmReservationEvent(UUID orderId);

}
