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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import se.jensen.johanna.fakestoreorderservice.dto.CartItemRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.dto.OrderRequest;
import se.jensen.johanna.fakestoreorderservice.dto.ProductBatchResponse;
import se.jensen.johanna.fakestoreorderservice.dto.ProductDTO;
import se.jensen.johanna.fakestoreorderservice.dto.ReservationRequest;
import se.jensen.johanna.fakestoreorderservice.dto.StripeEventDTO;
import se.jensen.johanna.fakestoreorderservice.exception.DomainStateException;
import se.jensen.johanna.fakestoreorderservice.mapper.AddressMapper;
import se.jensen.johanna.fakestoreorderservice.mapper.OrderItemMapper;
import se.jensen.johanna.fakestoreorderservice.messaging.OrderEventPublisher;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;
import se.jensen.johanna.fakestoreorderservice.model.ShippingAddress;
import se.jensen.johanna.fakestoreorderservice.repository.OrderRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  @Value("${product-service-url}")
  private String productServiceUrl;

  @Value("${inventory-service-url}")
  private String inventoryServiceUrl;

  private final OrderRepository orderRepository;
  private final OrderItemMapper orderItemMapper;
  private final AddressMapper addressMapper;
  private final RestTemplate restTemplate;
  private final PaymentService paymentService;
  private final OrderEventPublisher orderEventPublisher;

  /**
   * Creates an order with status PENDING. Retrieves cart items from product-service and reserves
   * the cart in inventory. Creates and returns a checkout session for stripe payment
   *
   * @param jwt     token
   * @param request set containing id of products and quantity
   * @return CheckoutResponse containing stripe url for payment
   */

  public CheckoutResponse putOrder(Jwt jwt, OrderRequest request) {
    log.info("Creating order for user {}. request {}...", jwt.getSubject(), request);

    UUID buyerId = UUID.fromString(jwt.getSubject());
    String email = jwt.getClaimAsString("email");
    Set<UUID> productIds = request.itemRequests().stream().map(CartItemRequest::productId)
        .collect(Collectors.toSet());
    List<ProductDTO> products = fetchCartProducts(productIds);
    List<OrderItem> orderItems = mapOrderItems(products, request.itemRequests());
    ShippingAddress shippingAddress = addressMapper.toShippingAddress(request.addressRequest());
    Order pendingOrder = Order.create(buyerId, orderItems, shippingAddress);
    orderRepository.save(pendingOrder);
    log.info("Pending Order {} created. Reserving order items...", pendingOrder.getOrderId());
    reserveOrderItems(new ReservationRequest(request.itemRequests(), pendingOrder.getOrderId()));
    log.info("Creating checkout session for order {}...", pendingOrder.getOrderId());
    return paymentService.createCheckoutSession(pendingOrder, email);

  }

  /**
   * Sends reservation request to inventory
   *
   * @param reservationRequest Set of product id-quantity and order id to track reservation
   */
  public void reserveOrderItems(ReservationRequest reservationRequest) {
    log.info("Reserving order items {} for order {}...", reservationRequest.cartItemRequests(),
        reservationRequest.orderId());
    HttpEntity<ReservationRequest> entity = new HttpEntity<>(reservationRequest);
    try {
      log.debug("Sending reservation request {} to inventory url {}", entity, inventoryServiceUrl);
      restTemplate.postForEntity(
          inventoryServiceUrl + "/reservations/reserve-cart", entity, Void.class
      );
    } catch (HttpStatusCodeException e) {
      log.error("Unable to reserve order items from {}. status: {}", inventoryServiceUrl,
          e.getStatusCode());
      throw new DomainStateException("Unable to process order.");
    }

  }

  public List<OrderItem> mapOrderItems(List<ProductDTO> products,
      Set<CartItemRequest> itemRequests) {
    log.debug("Mapping order items from cart items {}, matching product ids {}...", itemRequests,
        products);
    List<OrderItem> orderItems = new ArrayList<>();
    for (CartItemRequest item : itemRequests) {
      ProductDTO productDTO = products.stream()
          .filter(p -> p.productId().equals(item.productId())).findFirst().orElse(null);
      if (productDTO == null) {
        log.error("Matching Product not found {}", item.productId());
        throw new DomainStateException("Unable to process order.");
      }
      orderItems.add(orderItemMapper.toOrderItem(productDTO, item.quantity()));
    }
    return orderItems;
  }

  /**
   * Retrieves all products from the cart from productservice
   */
  public List<ProductDTO> fetchCartProducts(Set<UUID> productIds) {
    log.info("Fetching products from product service for productIds: {}...", productIds);
    HttpEntity<Set<UUID>> entity = new HttpEntity<>(productIds);
    ProductBatchResponse response = restTemplate.postForObject(
        productServiceUrl + "/internal/products/batch", entity, ProductBatchResponse.class);
    if (response == null || response.products() == null || response.products().isEmpty()) {
      log.error("Unable to get products from product service");
      throw new DomainStateException("Unable to process order.");
    }
    return response.products();

  }

  /**
   * Triggered by Stripe paid events. Marks order as PAID and publishes event to confirm reservation
   * in inventory.
   */
  @Transactional
  public void handlePaidOrder(StripeEventDTO stripeEvent) {
    String stripeSessionId = stripeEvent.detail().data().stripeObject().sessionId();
    log.info("Handling paid order. stripe session: {}",
        stripeSessionId);
    Order order = orderRepository.findByStripeSessionId(stripeSessionId).orElseThrow(() -> {
      log.error("Order for stripe session id: {} not found",
          stripeSessionId);
      return new DomainStateException("Unable to process order.");
    });
    order.confirmPaidOrder();
    orderRepository.save(order);
    log.info("Order {} confirmed paid", order.getOrderId());

    log.info("Begin to publish confirm reservation event. OrderId: {} ",
        order.getOrderId());
    orderEventPublisher.publishConfirmReservationEvent(order.getOrderId());
  }


}



