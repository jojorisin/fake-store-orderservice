package se.jensen.johanna.fakestoreorderservice.messaging;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.jensen.johanna.fakestoreorderservice.dto.StripeEventDTO;
import se.jensen.johanna.fakestoreorderservice.repository.OrderRepository;
import se.jensen.johanna.fakestoreorderservice.service.OrderService;

@Slf4j
@Profile("!local")
@Component
@RequiredArgsConstructor
public class StripeListener {

  private final OrderService orderService;
  private final OrderRepository orderRepository;


  /**
   * Listens to successfully paid stripe-events.
   */
  @SqsListener("${app.queues.order-paid-events}")
  public void handlePaymentStatusPaid(StripeEventDTO stripeEvent) {
    log.info("Received ORDER PAID stripe event: {}", stripeEvent);
    String sessionId = stripeEvent.detail().data().stripeObject().sessionId();
    if (sessionId == null) {
      log.warn("Stripe session id is null. Abort processing.");
      return;
    }
    if (!orderRepository.existsByStripeSessionId(sessionId)) {
      log.warn("Order not found for stripe session id: {}. Abort processing.",
          sessionId);
      return;
    }
    orderService.handlePaidOrder(stripeEvent);

  }

}
