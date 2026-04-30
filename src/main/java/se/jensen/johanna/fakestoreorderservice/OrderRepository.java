package se.jensen.johanna.fakestoreorderservice;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import se.jensen.johanna.fakestoreorderservice.model.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {

}
