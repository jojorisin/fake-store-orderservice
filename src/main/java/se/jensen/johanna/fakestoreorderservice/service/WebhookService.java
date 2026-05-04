package se.jensen.johanna.fakestoreorderservice.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.jensen.johanna.fakestoreorderservice.exception.PaymentException;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

  private final OrderService orderService;
  @Value("${stripe.webhook-secret}")
  private String webhookSecret;


  public void handleStripeWebhook(String payload, String signature) {
    try {
      Event event = Webhook.constructEvent(payload, signature, webhookSecret);
      EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
      Session session;
      switch (event.getType()) {
        case "checkout.session.completed":
          if (dataObjectDeserializer.getObject().isPresent()) {
            session = (Session) dataObjectDeserializer.getObject().get();
            if (session.getPaymentStatus().equals("paid")) {
              String sessionId = session.getId();
              orderService.confirmPaidOrder(sessionId);
            } else {
              log.warn("session is not paid");
            }

          }
          break;

        default:
          log.info("Ignoring event type: {}", event.getType());
      }
    } catch (StripeException e) {
      log.error("Error handling stripe webhook", e);
      throw new PaymentException("Unable to process payment.");
    }
  }


}
