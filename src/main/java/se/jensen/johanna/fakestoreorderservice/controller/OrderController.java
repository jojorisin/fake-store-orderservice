package se.jensen.johanna.fakestoreorderservice.controller;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.jensen.johanna.fakestoreorderservice.dto.CheckoutResponse;
import se.jensen.johanna.fakestoreorderservice.dto.OrderRequest;
import se.jensen.johanna.fakestoreorderservice.service.OrderService;
import se.jensen.johanna.fakestoreorderservice.service.WebhookService;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;
  private final WebhookService webhookService;

  @PostMapping
  public ResponseEntity<CheckoutResponse> order(@AuthenticationPrincipal Jwt jwt,
      @RequestBody @Valid OrderRequest orderRequest) {

    return ResponseEntity.status(HttpStatus.CREATED).body(orderService.putOrder(jwt, orderRequest));

  }

  @PostMapping("/webhook")
  public ResponseEntity<Void> handleWebhook(@RequestBody byte[] payload,
      @RequestHeader("stripe-signature") String signature) {
    webhookService.handleStripeWebhook(new String(payload, StandardCharsets.UTF_8), signature);
    return ResponseEntity.ok().build();
  }

}
