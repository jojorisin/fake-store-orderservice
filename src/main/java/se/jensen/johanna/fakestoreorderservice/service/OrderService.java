package se.jensen.johanna.fakestoreorderservice.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import se.jensen.johanna.fakestoreorderservice.client.InventoryClient;
import se.jensen.johanna.fakestoreorderservice.client.ProductClient;
import se.jensen.johanna.fakestoreorderservice.dto.CartItemRequest;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.dto.OrderRequest;
import se.jensen.johanna.fakestoreorderservice.dto.PaymentWebhookEvent;
import se.jensen.johanna.fakestoreorderservice.dto.ProductBatchResponse;
import se.jensen.johanna.fakestoreorderservice.dto.ProductDTO;
import se.jensen.johanna.fakestoreorderservice.dto.ReservationRequest;
import se.jensen.johanna.fakestoreorderservice.exception.domain.ProductNotFound;
import se.jensen.johanna.fakestoreorderservice.exception.infra.InternalServiceException;
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

  private final InventoryClient inventoryClient;
  private final ProductClient productClient;
  private final OrderRepository orderRepository;
  private final OrderItemMapper orderItemMapper;
  private final AddressMapper addressMapper;
  private final PaymentProvider paymentProvider;
  private final OrderEventPublisher orderEventPublisher;

  /**
   * Creates an order with status PENDING. Retrieves cart items from product service then validates
   * and reserves the cart in inventory. Returns a checkout response from the chosen payment
   * provider
   *
   * @param request set containing id of products and quantity
   * @return CheckoutResponse containing checkout url and payment reference
   */

  public CheckoutResponse putOrder(Jwt jwt, OrderRequest request) {
    log.info("Creating order for user {}. request {}...", jwt.getSubject(), request);

    Set<UUID> productIds = request.itemRequests().stream().map(CartItemRequest::productId)
        .collect(Collectors.toSet());
    List<ProductDTO> products = fetchCartProducts(productIds);
    List<OrderItem> orderItems = validateAndMapOrderItems(products, request.itemRequests());

    Order pendingOrder = Order.create(UUID.fromString(jwt.getSubject()), orderItems,
        addressMapper.toShippingAddress(request.addressRequest()));

    log.debug("Pending Order {} created. Reserving order items...", pendingOrder.getOrderId());
    reserveCart(new ReservationRequest(request.itemRequests(), pendingOrder.getOrderId()));

    log.debug("Creating checkout session for order {}...", pendingOrder.getOrderId());
    CheckoutResponse response = paymentProvider.createCheckoutSession(pendingOrder,
        jwt.getClaimAsString("email"));
    pendingOrder.assignPaymentReferences(response.paymentReference(),
        paymentProvider.getPaymentType());

    orderRepository.save(pendingOrder);
    log.info("Order created. Order id: {}, User: {}", pendingOrder.getOrderId(),
        pendingOrder.getBuyerId());

    return response;
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
      throw new InternalServiceException("Unable to reserve order items", e);
    }
  }


  /**
   * Validates that the product ids in the request from the client match the response from
   * product-service then maps them to order items
   */
  private List<OrderItem> validateAndMapOrderItems(List<ProductDTO> products,
      Set<CartItemRequest> itemRequests) {
    Map<UUID, ProductDTO> productMap = products.stream()
        .collect(Collectors.toMap(ProductDTO::productId, p -> p));
    return itemRequests.stream().map(item -> {
      ProductDTO productDTO = productMap.get(item.productId());
      if (productDTO == null) {
        log.warn("Product id: {} was not found in product service response.",
            item.productId());
        throw new ProductNotFound("Product not found.");
      }

      return orderItemMapper.toOrderItem(productDTO, item.quantity());
    }).collect(Collectors.toList());
  }


  /**
   * Retrieves all products from the cart from productservice
   */
  private List<ProductDTO> fetchCartProducts(Set<UUID> productIds) {
    log.debug("Fetching products from product service for productIds: {}...", productIds);

    ProductBatchResponse batchResponse;
    try {
      batchResponse = productClient.getProductBatch(productIds);
    } catch (RestClientException e) {
      log.error("Failed to fetch products from product-service. Product ids: {}", productIds, e);
      throw new InternalServiceException("Unable to fetch products from product service", e);
    }
    if (batchResponse == null || batchResponse.products() == null) {
      log.error(
          "Product service returned null when fetching products. Response: {}, Product ids: {} ",
          batchResponse,
          productIds);
      throw new InternalServiceException("Invalid response from product-service");
    }
    if (batchResponse.products().isEmpty()) {
      log.warn("Product service returned empty list when fetching cart items. Product ids: {}",
          productIds);
      throw new ProductNotFound("Products not found.");
    }

    return batchResponse.products();
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
          return new InternalServiceException("Order not found");
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



