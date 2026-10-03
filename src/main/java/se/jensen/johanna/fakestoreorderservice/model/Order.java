package se.jensen.johanna.fakestoreorderservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import se.jensen.johanna.fakestoreorderservice.exception.domain.InvalidOrderStateException;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentProviderType;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "orders")
@Builder
@Getter
public class Order {

  @Id
  private UUID orderId;

  @NotNull
  private UUID buyerId;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @NotNull
  private List<OrderItem> orderItems;

  @NotNull
  @Embedded
  private ShippingAddress shippingAddress;

  @NotNull
  private BigDecimal orderSum;

  @NotNull
  @Column(nullable = false, length = 3)
  private Currency currency;

  @NotNull
  @Enumerated(EnumType.STRING)
  private OrderStatus orderStatus;

  private String paymentReference;

  @Enumerated(EnumType.STRING)
  private PaymentProviderType paymentType;

  private Instant createdAt;

  private Instant updatedAt;

  @PrePersist
  private void onCreate() {
    this.createdAt = Instant.now();
  }

  @PreUpdate
  private void onUpdate() {
    this.updatedAt = Instant.now();
  }

  public static Order create(UUID buyerId, List<OrderItem> orderItems, ShippingAddress address,
      Currency currency) {
    if (orderItems == null || orderItems.isEmpty()) {
      throw new InvalidOrderStateException("Order items cant be null or empty.");
    }
    Order order = Order.builder()
        .orderId(UUID.randomUUID())
        .buyerId(buyerId)
        .orderItems(orderItems)
        .shippingAddress(address)
        .currency(currency)
        .orderSum(calculateOrderSum(orderItems))
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .orderStatus(OrderStatus.PENDING).build();

    order.orderItems.forEach(item -> item.giveParent(order));

    return order;
  }

  private static BigDecimal calculateOrderSum(List<OrderItem> orderItems) {
    return orderItems.stream().map(OrderItem::calculateTotalPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public void assignPaymentReferences(String paymentReference, PaymentProviderType paymentType) {
    this.paymentReference = paymentReference;
    this.paymentType = paymentType;
  }

  public void updatePaymentReference(String paymentReference) {
    if (orderStatus != OrderStatus.PENDING) {
      throw new InvalidOrderStateException(
          "Payment reference can't be updated if order is not PENDING.");
    }
    this.paymentReference = paymentReference;
  }

  public void confirmPaidOrder() {
    if (!orderStatus.equals(OrderStatus.PENDING)) {
      throw new InvalidOrderStateException("Order is already paid");
    }
    orderStatus = OrderStatus.PAID;
  }


}
