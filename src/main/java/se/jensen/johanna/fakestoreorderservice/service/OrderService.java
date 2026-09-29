package se.jensen.johanna.fakestoreorderservice.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import se.jensen.johanna.fakestoreorderservice.client.CartClient;
import se.jensen.johanna.fakestoreorderservice.client.InventoryClient;
import se.jensen.johanna.fakestoreorderservice.dto.AddressRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CartItemRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutCartItemDTO;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutCartResponse;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.dto.PaymentWebhookEvent;
import se.jensen.johanna.fakestoreorderservice.dto.ReservationRequest;
import se.jensen.johanna.fakestoreorderservice.exception.domain.EmptyCartException;
import se.jensen.johanna.fakestoreorderservice.exception.infra.InternalClientException;
import se.jensen.johanna.fakestoreorderservice.mapper.AddressMapper;
import se.jensen.johanna.fakestoreorderservice.mapper.OrderItemMapper;
import se.jensen.johanna.fakestoreorderservice.messaging.OrderEventPublisher;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;
import se.jensen.johanna.fakestoreorderservice.model.ShippingAddress;
import se.jensen.johanna.fakestoreorderservice.repository.OrderRepository;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentEventType;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentProviderType;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  private final InventoryClient inventoryClient;
  private final OrderRepository orderRepository;
  private final OrderItemMapper orderItemMapper;
  private final AddressMapper addressMapper;
  private final PaymentProvider paymentProvider;
  private final OrderEventPublisher orderEventPublisher;
  private final CartClient cartClient;

  public CheckoutResponse putOrder(Jwt jwt, AddressRequest addressRequest) {
    log.debug("fetching cart for order...");
    CheckoutCartResponse cartToCheckout = cartClient.getCartForCheckout();
    List<CheckoutCartItemDTO> cartItems = cartToCheckout.checkoutCart();
    if (cartItems == null || cartItems.isEmpty()) {
      log.debug("empty cart at checkout.");
      throw new EmptyCartException("No items in cart. Please add products.");
    }
    log.debug("Checkout cart: {}", cartToCheckout);
    List<OrderItem> orderItems = cartItems.stream().map(orderItemMapper::toOrderItem).toList();
    ShippingAddress address = addressMapper.toShippingAddress(addressRequest);

    // create pending order
    Order order = Order.create(UUID.fromString(jwt.getSubject()), orderItems, address);
    UUID orderId = order.getOrderId();

    // create checkout session with payment provider
    log.debug("Creating checkout session for order...");
    CheckoutResponse checkoutResponse = paymentProvider.createCheckoutSession(order,
        jwt.getClaimAsString("email"));
    // assign payment reference to order
    order.assignPaymentReferences(checkoutResponse.paymentReference(),
        paymentProvider.getPaymentType());
    // send reservation to inventory
    List<CartItemRequest> itemsToReserve = cartItems.stream()
        .map(orderItemMapper::toCartItemRequest).toList();
    log.debug("send reservation request to inventory. items: {}", itemsToReserve);
    reserveCart(new ReservationRequest(itemsToReserve, orderId));

    orderRepository.save(order);
    log.info("Order created. Order id:{}, User: {}", orderId, order.getBuyerId());
    return checkoutResponse;
  }


  /**
   * Sends reservation request to inventory
   *
   * @param reservationRequest Set of product id-quantity and order id to track reservation
   */
  public void reserveCart(ReservationRequest reservationRequest) {
    log.debug("Reserving cart {} for order {}...", reservationRequest.cartItemRequests(),
        reservationRequest.orderId());
    try {
      inventoryClient.reserveCart(reservationRequest);
    } catch (RestClientException e) {
      log.error(
          "Unable to reserve cart for order {}. Cart items: {}",
          reservationRequest.orderId(),
          reservationRequest.cartItemRequests(),
          e
      );
      throw new InternalClientException("Unable to reserve order items", e);
    }
  }


  /**
   * Receives webhook from the payment provider, marks order as paid and publishes an order-paid
   * event. Note: Currently only one implemented payment provider
   */
  public void handlePaymentWebhook(PaymentProviderType paymentType, String payload,
      String signature) {
    log.debug("Handling payment webhook for payment provider: {}...", paymentType);

    PaymentWebhookEvent event = paymentProvider.parseWebhookEvent(payload, signature);
    if (event == null) {
      log.debug("Payment provider returned null event. Skipping webhook.");
      return;
    }
    Order order = orderRepository.findByPaymentReference(event.paymentReference())
        .orElseThrow(() -> {
          log.error("Order for stripe session id: {} not found",
              event.paymentReference());
          return new InternalClientException("Order not found");
        });
    if (event.eventType().equals(PaymentEventType.PAID)) {
      log.debug("Order {} is paid. Confirming paid order...", order.getOrderId());
      order.confirmPaidOrder();
      orderRepository.save(order);
      log.info("Order {} confirmed paid", order.getOrderId());
      log.debug("Begin to publish confirm reservation event. OrderId...");

      orderEventPublisher.publishConfirmReservationEvent(order.getOrderId());
    }
  }

}



