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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.exception.CheckoutException;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentType;

@Component
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

  @Value("${stripe.api-key}")
  private String stripeApiKey;

  @Value("${stripe.success-url}")
  private String stripeSuccessUrl;

  @Value("${stripe.cancel-url}")
  private String stripeCancelUrl;

  @PostConstruct
  public void init() {
    Stripe.apiKey = stripeApiKey;
    log.info("Stripe API initialized for creating checkout sessions.");
  }

  @Override
  public boolean supports(PaymentType paymentMethod) {
    return paymentMethod == PaymentType.STRIPE;
  }

  @Override
  public PaymentType getPaymentType() {
    return PaymentType.STRIPE;
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
      //order.assignStripeSession(session.getId());

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


}
