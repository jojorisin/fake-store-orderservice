package se.jensen.johanna.fakestoreorderservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import se.jensen.johanna.fakestoreorderservice.dto.AddressRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutCartItemDTO;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutCartResponse;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.dto.event.OrderConfirmedPaidEvent;
import se.jensen.johanna.fakestoreorderservice.exception.domain.InvalidOrderStateException;
import se.jensen.johanna.fakestoreorderservice.mapper.AddressMapper;
import se.jensen.johanna.fakestoreorderservice.mapper.OrderItemMapper;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;
import se.jensen.johanna.fakestoreorderservice.model.OrderStatus;
import se.jensen.johanna.fakestoreorderservice.model.ShippingAddress;
import se.jensen.johanna.fakestoreorderservice.repository.OrderRepository;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentProviderType;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @InjectMocks
  private OrderService orderService;
  @Mock
  private OrderRepository orderRepository;

  @Mock
  private InventoryGateway inventoryGateway;
  @Mock
  private CartGateway cartGateway;
  @Spy
  private OrderItemMapper orderItemMapper = Mappers.getMapper(OrderItemMapper.class);
  @Spy
  private final AddressMapper addressMapper = Mappers.getMapper(AddressMapper.class);
  @Mock
  private PaymentService paymentService;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  private Jwt jwt;


  @BeforeEach
  void setUp() {
    jwt = mock(Jwt.class);
  }

  @Test
  void shouldSuccessfullyPutOrderAndSave() {
    UUID buyerId = UUID.randomUUID();
    AddressRequest addressRequest = getDefaultAddress();
    List<CheckoutCartItemDTO> cartItems = getDefaultCheckoutCartItems();
    BigDecimal cartSum = calculateCartSum(cartItems);
    CheckoutCartResponse checkoutCartResponse = new CheckoutCartResponse(cartItems);
    CheckoutResponse checkoutResponse = new CheckoutResponse("checkout url", "paymentreference",
        PaymentProviderType.STRIPE);

    when(cartGateway.getCartForCheckout()).thenReturn(checkoutCartResponse);
    when(jwt.getSubject()).thenReturn(String.valueOf(buyerId));
    when(jwt.getClaimAsString("email")).thenReturn("test@test.com");
    when(paymentService.createCheckoutSession(any(Order.class), anyString())).thenReturn(
        checkoutResponse);

    orderService.putOrder(jwt, addressRequest);

    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderRepository, times(1)).save(orderCaptor.capture());
    Order savedOrder = orderCaptor.getValue();
    assertEquals(buyerId, savedOrder.getBuyerId());
    assertEquals(cartItems.size(), savedOrder.getOrderItems().size());
    assertEquals(cartSum, savedOrder.getOrderSum());
  }

  @Test
  void handlePaidOrder_shouldSuccessfullyMarkOrderAsPaidAndPublishEvent() {
    String paymentReference = "1234";
    Order order = getDefaultOrder();
    order.assignPaymentReferences(paymentReference, PaymentProviderType.STRIPE);
    UUID orderId = order.getOrderId();

    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

    orderService.handlePaidOrder(paymentReference, orderId);

    assertEquals(OrderStatus.PAID, order.getOrderStatus());
    ArgumentCaptor<OrderConfirmedPaidEvent> captor = ArgumentCaptor.forClass(
        OrderConfirmedPaidEvent.class);
    verify(eventPublisher, times(1)).publishEvent(captor.capture());
    OrderConfirmedPaidEvent orderConfirmedPaidEvent = captor.getValue();
    assertEquals(order.getOrderId(), orderConfirmedPaidEvent.orderId());
  }

  @Test
  void handlePaidOrder_shouldThrowWhenOrderIsAlreadyMarkedAsPaid() {
    String savedPaymentReference = "1234";
    Order order = getDefaultOrder();
    UUID orderId = order.getOrderId();
    order.assignPaymentReferences(savedPaymentReference, PaymentProviderType.STRIPE);
    order.confirmPaidOrder();
    // incoming payment reference from webhook
    String webhookReference = "5678";

    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

    assertThrows(InvalidOrderStateException.class,
        () -> orderService.handlePaidOrder(webhookReference, orderId));
  }

  @Test
  void handlePaidOrder_shouldUpdatePaymentReferenceWhenMisMatch() {
    String savedPaymentReference = "1234";
    Order order = getDefaultOrder();
    UUID orderId = order.getOrderId();
    order.assignPaymentReferences(savedPaymentReference, PaymentProviderType.STRIPE);
    String webhookReference = "5678";

    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

    orderService.handlePaidOrder(webhookReference, orderId);

    assertEquals(webhookReference, order.getPaymentReference());
    assertEquals(OrderStatus.PAID, order.getOrderStatus());
  }

  @Test
  void handlePaidOrder_shouldUpdatePaymentReferenceWhenPaymentReferenceIsNull() {
    Order order = getDefaultOrder();
    UUID orderId = order.getOrderId();
    String webhookReference = "5678";

    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

    orderService.handlePaidOrder(webhookReference, orderId);

    assertEquals(webhookReference, order.getPaymentReference());
    assertEquals(OrderStatus.PAID, order.getOrderStatus());
  }

  private AddressRequest getDefaultAddress() {
    return new AddressRequest("firstName", "lastName", "co", "streetName",
        "streetName2", "12345", "city", "country");
  }

  private ShippingAddress getDefaultShippingAddress() {
    return ShippingAddress.create("firstName", "lastName", "co", "streetName",
        "streetName2", "12345", "city", "country");
  }

  private List<CheckoutCartItemDTO> getDefaultCheckoutCartItems() {
    return List.of(
        new CheckoutCartItemDTO(UUID.randomUUID(), 1, BigDecimal.valueOf(100.00), "title"),
        new CheckoutCartItemDTO(UUID.randomUUID(), 1, BigDecimal.valueOf(200.00), "title2"));
  }

  private List<OrderItem> getDefaultOrderItems() {
    return List.of(OrderItem.create(UUID.randomUUID(), "title", BigDecimal.valueOf(100.00), 1),
        OrderItem.create(UUID.randomUUID(), "title2", BigDecimal.valueOf(200.00), 3));
  }

  private BigDecimal calculateCartSum(List<CheckoutCartItemDTO> items) {
    return items.stream()
        .map(item -> item.pricePerItem().multiply(BigDecimal.valueOf(item.quantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private Order getDefaultOrder() {
    return Order.create(UUID.randomUUID(), getDefaultOrderItems(), getDefaultShippingAddress(),
        Currency.getInstance("USD"));
  }


}