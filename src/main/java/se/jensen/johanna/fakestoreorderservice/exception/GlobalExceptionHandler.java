package se.jensen.johanna.fakestoreorderservice.exception;

import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.jensen.johanna.fakestoreorderservice.dto.ErrorResponse;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponse> handleDomainException(DomainException e,
      HttpServletRequest request) {
    if (e.getErrorCode() == ErrorType.ILLEGAL_STATE) {
      log.error("Unexpected error: {}", e.getMessage(), e);
    } else {
      log.warn("Domain Error: [{}] {} | Path: {}",
          e.getErrorCode(), e.getMessage(), request.getRequestURI());
    }
    return ResponseEntity.status(e.getStatus()).body(new ErrorResponse(
        Instant.now(),
        e.getStatus().value(),
        e.getStatus().getReasonPhrase(),
        e.getErrorCode().name(),
        e.getMessage(),
        request.getRequestURI(),
        null
    ));
  }

  @ExceptionHandler(StripeException.class)
  public ResponseEntity<ErrorResponse> handleStripe(StripeException e, HttpServletRequest request) {
    log.error("StripeException - {}", e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(buildErrorResponse(e, HttpStatus.BAD_GATEWAY, "BAD_GATEWAY", request)
        );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationError(
      MethodArgumentNotValidException e, HttpServletRequest request) {
    Map<String, String> errors = new HashMap<>();
    for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
      String fieldName = fieldError.getField();
      errors.put(fieldName, fieldError.getDefaultMessage());
    }
    log.warn("Validation failed - fields: {}", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(), "VALIDATION_ERROR", "Validation failed",
            request.getRequestURI(),
            errors));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
    log.error("Exception - {}", e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(buildErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
            request));
  }

  private ErrorResponse buildErrorResponse(Exception e, HttpStatus status, String errorCode,
      HttpServletRequest request) {
    return new ErrorResponse(
        Instant.now(),
        status.value(),
        status.getReasonPhrase(),
        errorCode,
        e.getMessage(),
        request.getRequestURI(),
        null
    );
  }

}
