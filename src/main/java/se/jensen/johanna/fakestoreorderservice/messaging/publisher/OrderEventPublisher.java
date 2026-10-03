package se.jensen.johanna.fakestoreorderservice.messaging.publisher;

import java.util.UUID;

public interface OrderEventPublisher {

  void publishOrderPaidEvent(UUID orderId);

}
