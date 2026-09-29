package se.jensen.johanna.fakestoreorderservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.security.oauth2.jwt.Jwt;
import se.jensen.johanna.fakestoreorderservice.client.CartClient;
import se.jensen.johanna.fakestoreorderservice.client.InventoryClient;
import se.jensen.johanna.fakestoreorderservice.dto.AddressRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutCartItemDTO;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutCartResponse;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.mapper.AddressMapper;
import se.jensen.johanna.fakestoreorderservice.mapper.OrderItemMapper;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.repository.OrderRepository;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentProviderType;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @InjectMocks
  private OrderService orderService;
  @Mock
  private OrderRepository orderRepository;
  @Mock
  private PaymentProvider paymentProvider;
  @Mock
  private InventoryClient inventoryClient;
  @Spy
  private OrderItemMapper orderItemMapper = Mappers.getMapper(OrderItemMapper.class);
  @Spy
  private final AddressMapper addressMapper = Mappers.getMapper(AddressMapper.class);
  @Mock
  private CartClient cartClient;

  private Jwt jwt;


  @BeforeEach
  void setUp() {
    jwt = mock(Jwt.class);


  }

  @Test
  void shouldSuccessfullyPutOrderAndSave() {
    UUID buyerId = UUID.randomUUID();
    // products to order
    UUID productId1 = UUID.randomUUID();
    int quantity1 = 1;
    BigDecimal price1 = new BigDecimal("100.00");
    UUID productId2 = UUID.randomUUID();
    int quantity2 = 2;
    BigDecimal price2 = new BigDecimal("200.00");
    BigDecimal expectedOrderSum = price1.multiply(BigDecimal.valueOf(quantity1))
        .add(price2.multiply(BigDecimal.valueOf(quantity2)));
    AddressRequest addressRequest = getDefaultAddress();
    List<CheckoutCartItemDTO> cartItems = new ArrayList<>();
    cartItems.add(getCartItemDTO(productId1, quantity1, price1));
    cartItems.add(getCartItemDTO(productId2, quantity2, price2));
    CheckoutCartResponse checkoutCartResponse = new CheckoutCartResponse(cartItems);
    CheckoutResponse checkoutResponse = new CheckoutResponse("checkout url", "paymentreference");

    when(cartClient.getCartForCheckout()).thenReturn(checkoutCartResponse);
    when(jwt.getSubject()).thenReturn(String.valueOf(buyerId));
    when(jwt.getClaimAsString("email")).thenReturn("test@test.com");
    when(paymentProvider.getPaymentType()).thenReturn(PaymentProviderType.STRIPE);
    when(paymentProvider.createCheckoutSession(any(Order.class), anyString())).thenReturn(
        checkoutResponse);

    orderService.putOrder(jwt, addressRequest);

    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderRepository, times(1)).save(orderCaptor.capture());
    Order savedOrder = orderCaptor.getValue();
    assertEquals(buyerId, savedOrder.getBuyerId());
    assertEquals(cartItems.size(), savedOrder.getOrderItems().size());
    assertEquals(expectedOrderSum, savedOrder.getOrderSum());
  }

  private AddressRequest getDefaultAddress() {
    return new AddressRequest("firstName", "lastName", "co", "streetName",
        "streetName2", "12345", "city", "country");
  }

  private CheckoutCartItemDTO getCartItemDTO(UUID productId, int quantity,
      BigDecimal pricePerItem) {
    return new CheckoutCartItemDTO(productId, quantity, pricePerItem, "title");
  }


}