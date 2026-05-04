package se.jensen.johanna.fakestoreorderservice.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import se.jensen.johanna.fakestoreorderservice.model.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {


  @EntityGraph(attributePaths = "orderItems")
  Optional<Order> findByStripeSessionId(String stripeSessionId);

}
