package se.jensen.johanna.fakestoreorderservice.messaging;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("local")
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMqEventPublisher implements OrderEventPublisher {

  private final RabbitTemplate rabbitTemplate;
  @Value("${app.exchange.order-exchange}")
  private String orderExchange;

  private static final String ORDER_PAID_ROUTING_KEY = "order.paid";


  @Override
  public void publishConfirmReservationEvent(UUID orderId) {
    log.info("Publishing order paid event for order: {}...", orderId);

    rabbitTemplate.convertAndSend(orderExchange, ORDER_PAID_ROUTING_KEY, orderId);
    log.info("Order paid event published for order: {}", orderId);
  }


}
