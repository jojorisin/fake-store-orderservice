package se.jensen.johanna.fakestoreorderservice.messaging;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.jensen.johanna.fakestoreorderservice.dto.StripeEventDTO;
import se.jensen.johanna.fakestoreorderservice.exception.DomainStateException;
import se.jensen.johanna.fakestoreorderservice.service.OrderService;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripeListener {

  private final OrderService orderService;


  @SqsListener("${app.queues.order-events}")
  public void handleStripeEvent(StripeEventDTO stripeEvent) {
    if (stripeEvent == null) {
      log.error("Stripe event is null");
      throw new DomainStateException("Unable to process order.");
    }
    orderService.handlePaidOrder(stripeEvent);

  }

}
