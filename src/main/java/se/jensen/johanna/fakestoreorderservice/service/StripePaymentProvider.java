package se.jensen.johanna.fakestoreorderservice.service;

import com.stripe.Stripe;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.dto.PaymentWebhookEvent;
import se.jensen.johanna.fakestoreorderservice.exception.infra.InvalidPaymentWebhookException;
import se.jensen.johanna.fakestoreorderservice.exception.infra.PaymentProviderException;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentEventType;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentProviderType;

@Component
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

  @Value("${stripe.api-key}")
  private String stripeApiKey;

  @Value("${stripe.success-url}")
  private String stripeSuccessUrl;

  @Value("${stripe.cancel-url}")
  private String stripeCancelUrl;

  @Value("${stripe.webhook-secret}")
  private String stripeWebhookSecret;

  @PostConstruct
  public void init() {
    Stripe.apiKey = stripeApiKey;
    log.info("Stripe API initialized for creating checkout sessions.");
  }

  @Override
  public boolean supports(PaymentProviderType paymentMethod) {
    return paymentMethod == PaymentProviderType.STRIPE;
  }

  @Override
  public PaymentProviderType getPaymentType() {
    return PaymentProviderType.STRIPE;
  }

  @Override
  @Transactional
  public CheckoutResponse createCheckoutSession(Order order, String email) {
    log.debug("Creating stripe checkout session...");
    log.debug("Creating line items for checkout. order id: {}", order.getOrderId());
    List<SessionCreateParams.LineItem> lineItems = createLineItems(order.getOrderItems());
    try {
      SessionCreateParams params = SessionCreateParams.builder()
          .setMode(SessionCreateParams.Mode.PAYMENT).setCustomerEmail(email)
          .setSuccessUrl(stripeSuccessUrl + "{CHECKOUT_SESSION_ID}")
          .setCancelUrl(stripeCancelUrl)
          .addAllLineItem(lineItems)
          .putMetadata("orderId", order.getOrderId().toString()).build();
      Session session = Session.create(params);
      return new CheckoutResponse(session.getUrl(), session.getId());
    } catch (StripeException e) {
      throw new PaymentProviderException("Unable to process payment.", e);
    }

  }

  public List<SessionCreateParams.LineItem> createLineItems(List<OrderItem> orderItems) {
    log.debug("Creating line items from order items: {}", orderItems);
    List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
    for (OrderItem item : orderItems) {
      SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
          .setQuantity(item.getQuantity().longValue()).setPriceData(
              SessionCreateParams.LineItem.PriceData.builder().setCurrency("usd").setUnitAmount(
                      item.getPricePerItem().multiply(BigDecimal.valueOf(100)).longValue())
                  .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                      .setName(item.getTitle()).build()).build()).build();
      lineItems.add(lineItem);

    }
    return lineItems;
  }

  @Override
  public PaymentWebhookEvent parseWebhookEvent(String payload, String signature) {
    log.debug("Parsing stripe webhook event...");
    if (signature == null) {
      throw new InvalidPaymentWebhookException(
          "Unable to process payment. No stripe signature found in headers.");
    }
    Event event;
    try {
      event = Webhook.constructEvent(payload, signature, stripeWebhookSecret);
    } catch (SignatureVerificationException e) {
      throw new InvalidPaymentWebhookException("Unable to construct webhook event", e);
    }
    Session session;
    String orderId;
    PaymentEventType eventType;
    switch (event.getType()) {
      case "checkout.session.completed":
        session = (Session) event.getDataObjectDeserializer().getObject()
            .orElseGet(() -> {
              log.warn("API mismatch, using unsafe deserialization");
              try {
                return event.getDataObjectDeserializer().deserializeUnsafe();
              } catch (EventDataObjectDeserializationException e) {
                log.debug("Error parsing stripe webhook event.");
                throw new InvalidPaymentWebhookException("Unable to deserialize stripe event.", e);
              }
            });
        if (session.getMetadata() == null) {
          throw new InvalidPaymentWebhookException("Stripe Session metadata is null.");
        }
        orderId = session.getMetadata().get("orderId");
        if (orderId == null || orderId.isBlank()) {
          throw new InvalidPaymentWebhookException("Order id is missing from metadata.");
        }
        eventType = "paid".equals(session.getPaymentStatus()) ? PaymentEventType.PAID
            : PaymentEventType.CANCELLED;
        return new PaymentWebhookEvent(eventType, orderId, session.getId());
      default:
        log.debug("Unsupported event type. Returning null for event type {}", event.getType());
        return null;
    }

  }


}
