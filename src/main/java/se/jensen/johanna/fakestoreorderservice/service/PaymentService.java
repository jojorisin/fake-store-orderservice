package se.jensen.johanna.fakestoreorderservice.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.jensen.johanna.fakestoreorderservice.controller.PaymentException;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;

@Service
@Slf4j
public class PaymentService {

  @Value("${stripe.api-key}")
  private String stripeApiKey;

  @Value("${stripe.success-url}")
  private String stripeSuccessUrl;

  @Value("${stripe.cancel-url}")
  private String stripeCancelUrl;

  @Value("${stripe.webhook-secret}")
  private String webhookSecret;

  @PostConstruct
  public void init() {
    Stripe.apiKey = stripeApiKey;
    log.info("Loaded webhook secret: {}", webhookSecret);
  }

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
      order.assignStripeSession(session.getId());

      return new CheckoutResponse(session.getUrl(), session.getId());

    } catch (StripeException e) {
      log.error("Error creating checkout session {}, {}", e, e.getMessage());
      throw new PaymentException("Error creating checkout session");
    }

  }

  public List<SessionCreateParams.LineItem> createLineItems(List<OrderItem> orderItems) {
    List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
    for (OrderItem item : orderItems) {
      SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
          .setQuantity(item.getQuantity().longValue()).setPriceData(
              SessionCreateParams.LineItem.PriceData.builder().setCurrency("sek").setUnitAmount(
                      item.getPricePerItem().multiply(BigDecimal.valueOf(100)).longValue())
                  .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                      .setName(item.getTitle()).build()).build()).build();
      lineItems.add(lineItem);

    }
    return lineItems;
  }


}
