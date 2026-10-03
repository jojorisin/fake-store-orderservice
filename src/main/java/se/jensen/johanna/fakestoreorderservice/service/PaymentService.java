package se.jensen.johanna.fakestoreorderservice.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.dto.event.OrderPaidEvent;
import se.jensen.johanna.fakestoreorderservice.dto.event.PaymentWebhookEvent;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentProviderType;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

  private final PaymentProvider paymentProvider;
  private final ApplicationEventPublisher eventPublisher;

  public CheckoutResponse createCheckoutSession(Order order, String email) {
    return paymentProvider.createCheckoutSession(order, email);
  }

  /**
   * Receives webhook from payment provider and publishes an order paid event
   */
  // TODO handle other events ex cancelled
  public void handlePaymentWebhook(PaymentProviderType paymentType, String payload,
      String signature) {
    log.debug("Handling payment webhook for payment provider: {}...", paymentType);

    PaymentWebhookEvent event = paymentProvider.parseWebhookEvent(payload, signature);
    if (event == null) {
      log.debug("Payment provider returned null event. Skipping webhook.");
      return;
    }
    if (event.paymentStatus() == PaymentStatus.PAID) {
      log.debug("handling event type PAID...");
      eventPublisher.publishEvent(
          new OrderPaidEvent(event.paymentReference(), UUID.fromString(event.orderId())));
    } else {
      log.debug("ignoring other event types...");
    }
  }

}
