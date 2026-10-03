package se.jensen.johanna.fakestoreorderservice.messaging.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import se.jensen.johanna.fakestoreorderservice.dto.event.OrderConfirmedPaidEvent;
import se.jensen.johanna.fakestoreorderservice.dto.event.OrderPaidEvent;
import se.jensen.johanna.fakestoreorderservice.messaging.publisher.OrderEventPublisher;
import se.jensen.johanna.fakestoreorderservice.service.OrderService;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

  private final OrderService orderService;
  private final OrderEventPublisher eventPublisher;

  @EventListener
  public void onOrderPaid(OrderPaidEvent event) {
    log.debug("received order paid event...");
    orderService.handlePaidOrder(event.paymentReference(), event.orderId());
  }

  @TransactionalEventListener
  public void onOrderConfirmedPaid(OrderConfirmedPaidEvent event) {
    log.debug("received order confirmed paid event...");
    eventPublisher.publishOrderPaidEvent(event.orderId());
  }

}
