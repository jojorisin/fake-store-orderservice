package se.jensen.johanna.fakestoreorderservice.exception;

import com.stripe.exception.StripeException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.jensen.johanna.fakestoreorderservice.dto.ErrorResponse;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(StripeException.class)
  public ResponseEntity<ErrorResponse> handleStripe(StripeException e) {
    log.error("StripeException - {}", e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse(
        502, e.getClass().getSimpleName(), "Payment processing error", Instant.now()
    ));
  }

}
