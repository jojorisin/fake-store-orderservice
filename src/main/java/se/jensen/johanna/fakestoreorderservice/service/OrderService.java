package se.jensen.johanna.fakestoreorderservice.service;

import java.util.Currency;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.jensen.johanna.fakestoreorderservice.dto.AddressRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CartItemRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutCartItemDTO;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutCartResponse;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.dto.ReservationRequest;
import se.jensen.johanna.fakestoreorderservice.dto.event.OrderConfirmedPaidEvent;
import se.jensen.johanna.fakestoreorderservice.exception.domain.EmptyCartException;
import se.jensen.johanna.fakestoreorderservice.exception.domain.InvalidOrderStateException;
import se.jensen.johanna.fakestoreorderservice.exception.domain.OrderNotFoundException;
import se.jensen.johanna.fakestoreorderservice.mapper.AddressMapper;
import se.jensen.johanna.fakestoreorderservice.mapper.OrderItemMapper;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;
import se.jensen.johanna.fakestoreorderservice.model.OrderStatus;
import se.jensen.johanna.fakestoreorderservice.model.ShippingAddress;
import se.jensen.johanna.fakestoreorderservice.repository.OrderRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderItemMapper orderItemMapper;
  private final AddressMapper addressMapper;
  private final CartGateway cartGateway;
  private final InventoryGateway inventoryGateway;
  private final PaymentService paymentService;
  private final ApplicationEventPublisher eventPublisher;

  public CheckoutResponse putOrder(Jwt jwt, AddressRequest addressRequest) {
    log.debug("creating order...");
    CheckoutCartResponse cartToCheckout = cartGateway.getCartForCheckout();
    List<CheckoutCartItemDTO> cartItems = cartToCheckout.checkoutCart();
    if (cartItems == null || cartItems.isEmpty()) {
      log.debug("empty cart at checkout.");
      throw new EmptyCartException("No items in cart. Please add products.");
    }
    log.debug("Checkout cart: {}", cartToCheckout);
    List<OrderItem> orderItems = cartItems.stream().map(orderItemMapper::toOrderItem).toList();
    ShippingAddress address = addressMapper.toShippingAddress(addressRequest);

    // create pending order.
    // TODO fetch currency dynamically from cart
    Currency currency = Currency.getInstance("USD");
    Order order = Order.create(UUID.fromString(jwt.getSubject()), orderItems, address, currency);
    UUID orderId = order.getOrderId();

    // create checkout session with payment provider
    CheckoutResponse checkoutResponse = paymentService.createCheckoutSession(order,
        jwt.getClaimAsString("email"));
    // assign payment reference to order
    order.assignPaymentReferences(checkoutResponse.paymentReference(),
        checkoutResponse.paymentProviderType());
    // send reservation to inventory
    List<CartItemRequest> itemsToReserve = cartItems.stream()
        .map(orderItemMapper::toCartItemRequest).toList();
    inventoryGateway.reserveCart(new ReservationRequest(itemsToReserve, orderId));

    orderRepository.save(order);
    log.info("Order created. Order id:{}, User: {}", orderId, order.getBuyerId());
    return checkoutResponse;
  }

  /**
   * Triggered by internal order paid-event. Marks order as paid and publishes an order confirmed
   * paid-event.
   */
  @Transactional
  public void handlePaidOrder(String paymentReference, UUID orderId) {
    log.debug("handling paid order event...");
    Order order = orderRepository.findById(orderId).orElseThrow(() -> {
      log.error("CRITICAL: Order {} not found when confirming payment.", orderId);
      return new OrderNotFoundException("Order not found.");
    });
    String savedPaymentReference = order.getPaymentReference();

    if (savedPaymentReference != null && !savedPaymentReference
        .equals(paymentReference)) {
      if (order.getOrderStatus() == OrderStatus.PAID) {
        log.error(
            "CRITICAL: Double payment. Order {} is already paid with payment reference: {}. Incoming payment ref: {} ",
            orderId, savedPaymentReference, paymentReference);
        throw new InvalidOrderStateException("Order is already paid!");
      } else {
        log.warn(
            "Payment reference mis-match on Order {}. Old reference {}. Updating order with paid reference {} ",
            orderId, savedPaymentReference, paymentReference);
        order.updatePaymentReference(paymentReference);
      }
    }
    if (savedPaymentReference == null) {
      log.warn("Paid order {} is missing payment reference. Assigning order with paid reference {}",
          orderId, paymentReference);
      order.updatePaymentReference(paymentReference);
    }
    order.confirmPaidOrder();
    eventPublisher.publishEvent(new OrderConfirmedPaidEvent(orderId));
  }


}



