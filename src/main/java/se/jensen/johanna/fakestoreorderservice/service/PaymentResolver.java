package se.jensen.johanna.fakestoreorderservice.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.jensen.johanna.fakestoreorderservice.exception.UnsupportedPaymentTypeException;
import se.jensen.johanna.fakestoreorderservice.service.constants.PaymentType;

@RequiredArgsConstructor
@Slf4j
@Component
public class PaymentResolver {

  private final List<PaymentProvider> paymentProviders;

  public PaymentProvider resolve(PaymentType paymentType) {
    return paymentProviders.stream()
        .filter(paymentProvider -> paymentProvider.supports(paymentType))
        .findFirst().orElseThrow(() -> {
          log.error("paymenttype {} not implemented", paymentType);
          return new UnsupportedPaymentTypeException(
              "Payment type not supported: " + paymentType);
        });

  }
}
