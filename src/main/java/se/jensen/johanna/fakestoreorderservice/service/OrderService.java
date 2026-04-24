package se.jensen.johanna.fakestoreorderservice.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import se.jensen.johanna.fakestoreorderservice.OrderRepository;
import se.jensen.johanna.fakestoreorderservice.dto.OrderRequest;
import se.jensen.johanna.fakestoreorderservice.dto.OrderResponse;
import se.jensen.johanna.fakestoreorderservice.mapper.AddressMapper;
import se.jensen.johanna.fakestoreorderservice.mapper.OrderItemMapper;
import se.jensen.johanna.fakestoreorderservice.model.Order;
import se.jensen.johanna.fakestoreorderservice.model.OrderItem;
import se.jensen.johanna.fakestoreorderservice.model.ShippingAddress;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderItemMapper orderItemMapper;
  private final AddressMapper addressMapper;


  public OrderResponse createOrder(Jwt jwt, OrderRequest request) {
    UUID buyerId = UUID.fromString(jwt.getSubject());
    List<OrderItem> orderItems = request.itemRequests().stream().map(orderItemMapper::toOrderItem)
        .toList();
    ShippingAddress shippingAddress = addressMapper.toShippingAddress(request.addressRequest());
    Order pendingOrder = Order.create(buyerId, orderItems, shippingAddress);
    return new OrderResponse();
  }


}
