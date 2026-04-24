package se.jensen.johanna.fakestoreorderservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.jensen.johanna.fakestoreorderservice.dto.OrderRequest;
import se.jensen.johanna.fakestoreorderservice.dto.OrderResponse;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  @PostMapping
  public ResponseEntity<OrderResponse> order(@AuthenticationPrincipal Jwt jwt,
      @RequestBody @Valid OrderRequest orderRequest) {
    
    return ResponseEntity.ok(new OrderResponse());

  }

}
