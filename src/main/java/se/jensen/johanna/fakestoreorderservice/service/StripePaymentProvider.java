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
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.dto.PaymentWebhookEvent;
import se.jensen.johanna.fakestoreorderservice.exception.CheckoutException;
import se.jensen.johanna.fakestoreorderservice.exception.DomainStateException;
import se.jensen.johanna.fakestoreorderservice.exception.InvalidWebhookSignatureException;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentEventType;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentProviderType;

@Component
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

  private final SessionFactory sessionFactory;
  @Value("${stripe.api-key}")
  private String stripeApiKey;

  @Value("${stripe.success-url}")
  private String stripeSuccessUrl;

  @Value("${stripe.cancel-url}")
  private String stripeCancelUrl;

  @Value("${stripe.webhook-secret}")
  private String stripeWebhookSecret;

  public StripePaymentProvider(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

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
      log.error("Error creating checkout session for order {} {}, {}", order.getOrderId(), e,
          e.getMessage());
      throw new CheckoutException("Unable to process payment.");
    }

  }

  public List<SessionCreateParams.LineItem> createLineItems(List<OrderItem> orderItems) {
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
    if (lineItems.isEmpty()) {
      log.error("Error creating checkout session. No line items found.");
      throw new CheckoutException("Unable to process payment.");
    }
    return lineItems;
  }

  @Override
  public PaymentWebhookEvent parseWebhookEvent(String payload, String signature) {
    log.debug("Parsing webhook event {}", payload);
    if (signature == null) {
      log.warn("No stripe signature found in headers");
      throw new InvalidWebhookSignatureException(
          "Unable to process payment. No stripe signature found in headers.");
    }
    Event event;
    try {
      log.debug("Parsing webhook event {}", payload);
      event = Webhook.constructEvent(payload, signature, stripeWebhookSecret);
    } catch (SignatureVerificationException e) {
      log.error("Error parsing webhook event {}", e.getMessage());
      throw new CheckoutException("Unable to process payment.");
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
                return (Session) event.getDataObjectDeserializer().deserializeUnsafe();

              } catch (EventDataObjectDeserializationException e) {
                log.error("Error parsing webhook event {}", e.getMessage());
                throw new CheckoutException("Unable to process payment.");
              }
            });
        orderId = session.getMetadata().get("orderId");
        if (orderId == null || orderId.isBlank()) {
          log.error("Order id was not found in metadata. Stripe session id: {}", session.getId());
          throw new DomainStateException("Unable to process payment.");
        }
        eventType = session.getPaymentStatus().equals("paid") ? PaymentEventType.PAID
            : PaymentEventType.CANCELLED;
        return new PaymentWebhookEvent(eventType, orderId, session.getId());

      default:
        log.debug("Unsupported event type. Returning null for event type {}", event.getType());
        return null;

    }


  }


}
