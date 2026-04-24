package se.jensen.johanna.fakestoreorderservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

  @Value("${stripe.api-key}")
  private String stripeApiKey;

  @Value("${stripe.success-url}")
  private String stripeSuccessUrl;

  @Value("${stripe.cancel-url}")
  private String stripeCancelUrl;

  @Value("${stripe.webhook-secret}")
  private String webhookSecret;


}
