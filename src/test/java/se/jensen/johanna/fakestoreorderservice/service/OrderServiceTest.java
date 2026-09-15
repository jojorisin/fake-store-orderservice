package se.jensen.johanna.fakestoreorderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.HttpServerErrorException;
import se.jensen.johanna.fakestoreorderservice.client.InventoryClient;
import se.jensen.johanna.fakestoreorderservice.client.ProductClient;
import se.jensen.johanna.fakestoreorderservice.dto.AddressRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CartItemRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.dto.OrderRequest;
import se.jensen.johanna.fakestoreorderservice.dto.ProductBatchResponse;
import se.jensen.johanna.fakestoreorderservice.dto.ProductDTO;
import se.jensen.johanna.fakestoreorderservice.dto.ReservationRequest;
import se.jensen.johanna.fakestoreorderservice.exception.infra.InternalServiceException;
import se.jensen.johanna.fakestoreorderservice.mapper.AddressMapper;
import se.jensen.johanna.fakestoreorderservice.mapper.OrderItemMapper;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;
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
  private PaymentProvider paymentProvider;
  @Mock
  private ProductClient productClient;
  @Mock
  private InventoryClient inventoryClient;
  @Mock
  private OrderItemMapper orderItemMapper;
  @Mock
  private AddressMapper addressMapper;

  private Jwt jwt;


  @BeforeEach
  void setUp() {
    jwt = mock(Jwt.class);


  }


  @Test
  void putOrder_ShouldSuccessfullyPutOrderAndSave() {
    UUID sharedProductId = UUID.randomUUID();

    when(jwt.getSubject()).thenReturn(UUID.randomUUID().toString());
    when(jwt.getClaimAsString("email")).thenReturn("test@test.com");

    CartItemRequest cartItem = createCartItemRequest(sharedProductId, 2);
    Set<CartItemRequest> itemRequests = Set.of(cartItem);

    OrderRequest request = new OrderRequest(PaymentProviderType.STRIPE, itemRequests,
        defaultAddressRequest());

    ProductDTO product = createProductDtoWithId(sharedProductId);
    ProductBatchResponse fakeResponse = new ProductBatchResponse(List.of(product));

    OrderItem realItem = defaultOrderItem();

    when(addressMapper.toShippingAddress(any())).thenReturn(mock(ShippingAddress.class));
    when(productClient.getProductBatch(anySet())).thenReturn(fakeResponse);
    when(orderItemMapper.toOrderItem(any(ProductDTO.class), anyInt())).thenReturn(realItem);

    when(paymentProvider.createCheckoutSession(any(Order.class), anyString()))
        .thenReturn(new CheckoutResponse("http://url", "123"));

    CheckoutResponse result = orderService.putOrder(jwt, request);
    assertThat(result.checkoutUrl()).isEqualTo("http://url");
    verify(orderRepository, times(1)).save(any(Order.class));
    verify(inventoryClient).reserveCart(any(ReservationRequest.class));
  }


  @Test
  void reserveOrderItems_ShouldThrowInternalServiceExceptionWhenUnableToReserve() {
    ReservationRequest request = mock(ReservationRequest.class);
    doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(inventoryClient)
        .reserveCart(request);

    assertThrows(InternalServiceException.class,
        () -> orderService.reserveCart(request));

  }

  private CartItemRequest createCartItemRequest(UUID productId, Integer quantity) {
    return new CartItemRequest(productId, quantity);
  }


  private AddressRequest defaultAddressRequest() {
    return new AddressRequest("firstname", "lastname", "co", "streetname1",
        "streetName2", "54345", "city", "country");
  }


  private ProductDTO createProductDtoWithId(UUID productId) {
    return new ProductDTO(productId, "Title", 100, "description", "image");
  }


  private OrderItem defaultOrderItem() {
    return OrderItem.builder()
        .pricePerItem(new BigDecimal("100.00"))
        .quantity(1)
        .title("Title")
        .build();
  }

}