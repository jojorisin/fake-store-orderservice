package se.jensen.johanna.fakestoreorderservice.controller;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.jensen.johanna.fakestoreorderservice.service.OrderService;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentProviderType;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

  private final OrderService orderService;


  @PostMapping("/stripe-webhook")
  public ResponseEntity<Void> handleWebhook(@RequestBody byte[] payload,
      @RequestHeader("Stripe-Signature") String signature) {
    String payloadString = new String(payload, StandardCharsets.UTF_8);
    orderService.handlePaymentWebhook(PaymentProviderType.STRIPE, payloadString, signature);
    return ResponseEntity.ok().build();
  }


}
