package se.jensen.johanna.fakestoreorderservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "orders")
@Builder
public class Order {

  @Id
  @GeneratedValue
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
  @Enumerated(EnumType.STRING)
  private OrderStatus orderStatus;

  @NotNull
  private Instant createdAt;
  @NotNull
  private Instant updatedAt;

  @PreUpdate
  private void onUpdate() {
    this.updatedAt = Instant.now();
  }

  public static Order create(UUID buyerId, List<OrderItem> orderItems, ShippingAddress address) {
    return Order.builder()
        .buyerId(buyerId)
        .orderItems(orderItems)
        .shippingAddress(address)
        .orderSum(calculateOrderSum(orderItems))
        .orderStatus(OrderStatus.PENDING).build();

  }

  private static BigDecimal calculateOrderSum(List<OrderItem> orderItems) {
    return orderItems.stream().map(OrderItem::calculateTotalPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

}
