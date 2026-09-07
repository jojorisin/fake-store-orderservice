package se.jensen.johanna.fakestoreorderservice.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import se.jensen.johanna.fakestoreorderservice.dto.CartItemRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.dto.OrderRequest;
import se.jensen.johanna.fakestoreorderservice.dto.PaymentWebhookEvent;
import se.jensen.johanna.fakestoreorderservice.dto.ProductBatchResponse;
import se.jensen.johanna.fakestoreorderservice.dto.ProductDTO;
import se.jensen.johanna.fakestoreorderservice.dto.ReservationRequest;
import se.jensen.johanna.fakestoreorderservice.exception.DomainStateException;
import se.jensen.johanna.fakestoreorderservice.mapper.AddressMapper;
import se.jensen.johanna.fakestoreorderservice.mapper.OrderItemMapper;
import se.jensen.johanna.fakestoreorderservice.messaging.OrderEventPublisher;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;
import se.jensen.johanna.fakestoreorderservice.repository.OrderRepository;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentEventType;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentProviderType;

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
  private final OrderEventPublisher orderEventPublisher;
  private final PaymentResolver paymentResolver;

  /**
   * Creates an order with status PENDING. Retrieves cart items from product service then validates
   * and reserves the cart in inventory. Returns a checkout response from the chosen payment
   * provider
   *
   * @param jwt     token
   * @param request set containing id of products and quantity
   * @return CheckoutResponse containing checkout url and payment reference
   */

  public CheckoutResponse putOrder(Jwt jwt, OrderRequest request) {
    log.info("Creating order for user {}. request {}...", jwt.getSubject(), request);
    PaymentProvider paymentProvider = paymentResolver.resolve(request.paymentType());
    Set<UUID> productIds = request.itemRequests().stream().map(CartItemRequest::productId)
        .collect(Collectors.toSet());
    List<ProductDTO> products = fetchCartProducts(productIds);
    List<OrderItem> orderItems = validateAndMapOrderItems(products, request.itemRequests());

    Order pendingOrder = Order.create(UUID.fromString(jwt.getSubject()), orderItems,
        addressMapper.toShippingAddress(request.addressRequest()));
    orderRepository.save(pendingOrder);
    log.info("Pending Order {} created. Reserving order items...", pendingOrder.getOrderId());
    reserveOrderItems(new ReservationRequest(request.itemRequests(), pendingOrder.getOrderId()));

    log.info("Creating checkout session for order {}...", pendingOrder.getOrderId());
    CheckoutResponse response = paymentProvider.createCheckoutSession(pendingOrder,
        jwt.getClaimAsString("email"));
    pendingOrder.assignPaymentReferences(response.paymentReference(),
        paymentProvider.getPaymentType());
    orderRepository.save(pendingOrder);

    return response;

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
    } catch (RestClientException e) {
      log.error("Unable to reserve order items from {}. status: {}", inventoryServiceUrl,
          e.getMessage());
      throw new DomainStateException("Unable to process order.");
    }

  }


  /**
   * Validates that the product ids in the request from the client match the response from
   * product-service then maps them to order items
   */
  public List<OrderItem> validateAndMapOrderItems(List<ProductDTO> products,
      Set<CartItemRequest> itemRequests) {
    Map<UUID, ProductDTO> productMap = products.stream()
        .collect(Collectors.toMap(ProductDTO::productId, p -> p));
    return itemRequests.stream().map(item -> {
      ProductDTO productDTO = productMap.get(item.productId());
      if (productDTO == null) {
        log.warn("Unable to validate cart item. Client sent invalid product id {}",
            item.productId());
        throw new IllegalArgumentException("Unable to process order.");
      }
      return orderItemMapper.toOrderItem(productDTO, item.quantity());
    }).collect(Collectors.toList());
  }


  /**
   * Retrieves all products from the cart from productservice
   */
  public List<ProductDTO> fetchCartProducts(Set<UUID> productIds) {
    log.debug("Fetching products from product service for productIds: {}...", productIds);
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
   * Receives webhook from the payment provider, marks order as paid and publishes an order-paid
   * event
   */
  public void handlePaymentWebhook(PaymentProviderType paymentType, String payload,
      String signature) {
    log.debug("Handling payment webhook...");
    PaymentProvider paymentProvider = paymentResolver.resolve(paymentType);
    PaymentWebhookEvent event = paymentProvider.parseWebhookEvent(payload, signature);
    if (event == null) {
      log.debug("Payment provider returned null event. Skipping webhook.");
      return;
    }
    Order order = orderRepository.findByPaymentReference(event.paymentReference())
        .orElseThrow(() -> {
          log.error("Order for stripe session id: {} not found",
              event.paymentReference());
          return new DomainStateException("Unable to process order.");
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



