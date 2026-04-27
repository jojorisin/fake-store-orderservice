package se.jensen.johanna.fakestoreorderservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import se.jensen.johanna.fakestoreorderservice.OrderRepository;
import se.jensen.johanna.fakestoreorderservice.dto.CartItemRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.dto.OrderItemRequest;
import se.jensen.johanna.fakestoreorderservice.dto.OrderRequest;
import se.jensen.johanna.fakestoreorderservice.dto.ProductDTO;
import se.jensen.johanna.fakestoreorderservice.dto.ReservationRequest;
import se.jensen.johanna.fakestoreorderservice.dto.ReservationResponse;
import se.jensen.johanna.fakestoreorderservice.mapper.AddressMapper;
import se.jensen.johanna.fakestoreorderservice.mapper.OrderItemMapper;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;
import se.jensen.johanna.fakestoreorderservice.model.ShippingAddress;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  @Value("${PRODUCT_SERVICE_URL}")
  private String productServiceUrl;

  private final OrderRepository orderRepository;
  private final OrderItemMapper orderItemMapper;
  private final AddressMapper addressMapper;
  private final RestTemplate restTemplate;
  private final PaymentService paymentService;


  @Transactional
  public CheckoutResponse putOrder(Jwt jwt, OrderRequest request) {
    log.info("Creating order for user {}", jwt.getSubject());
    Set<CartItemRequest> cartItemRequests = request.orderItemRequests().stream()
        .map(orderItemMapper::toCartItemRequest).collect(
            Collectors.toSet());
    UUID reservationId = reserveOrderItems(jwt, cartItemRequests);
    UUID buyerId = UUID.fromString(jwt.getSubject());
    String email = jwt.getClaimAsString("email");
    List<OrderItem> orderItems = getOrderItems(request.orderItemRequests());
    ShippingAddress shippingAddress = addressMapper.toShippingAddress(request.addressRequest());
    Order pendingOrder = Order.create(buyerId, orderItems, shippingAddress, reservationId);
    orderRepository.save(pendingOrder);

    log.info("Creating checkout session for order {}", pendingOrder.getOrderId());
    return paymentService.createCheckoutSession(pendingOrder, email);


  }

  public UUID reserveOrderItems(Jwt jwt, Set<CartItemRequest> itemRequests) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwt.getTokenValue());
    HttpEntity<ReservationRequest> entity = new HttpEntity<>(new ReservationRequest(itemRequests),
        headers);
    ReservationResponse reservationResponse = restTemplate.postForObject(
        productServiceUrl + "/reservations/reserve-cart", entity, ReservationResponse.class
    );
    return reservationResponse.reservationID();
  }

  //
  public List<OrderItem> getOrderItems(Set<OrderItemRequest> itemRequests) {
    List<OrderItem> orderItems = new ArrayList<>();
    try {
      for (OrderItemRequest item : itemRequests) {
        ProductDTO productDTO = restTemplate.getForObject(
            productServiceUrl + "/products/" + item.productId(), ProductDTO.class);
        orderItems.add(orderItemMapper.toOrderItem(productDTO, item.quantity()));
      }
    } catch (Exception e) {
      log.error("Error fetching products", e);
    }

    return orderItems;
  }


}
