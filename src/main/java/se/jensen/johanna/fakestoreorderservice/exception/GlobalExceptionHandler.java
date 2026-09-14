package se.jensen.johanna.fakestoreorderservice.exception;

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
import se.jensen.johanna.fakestoreorderservice.exception.domain.DomainException;
import se.jensen.johanna.fakestoreorderservice.exception.infra.InfrastructureException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponse> handleDomainException(DomainException e,
      HttpServletRequest request) {
    HttpStatus status = getHttpStatus(e.getErrorCode());
    if (e.getErrorCode() == ErrorCode.INVALID_ORDER_STATE) {
      log.error("Invalid order state. path: {}", request.getRequestURI(), e);
    }
    if (e.getErrorCode() == ErrorCode.PRODUCT_NOT_FOUND) {
      log.warn("Product not found: {}, path: {}", e.getMessage(), request.getRequestURI());
    }
    return ResponseEntity.status(status).body(new ErrorResponse(
        Instant.now(), status.value(), e.getErrorCode(), e.getMessage(), null
    ));

  }

  @ExceptionHandler(InfrastructureException.class)
  public ResponseEntity<ErrorResponse> handleInfrastructureException(InfrastructureException e,
      HttpServletRequest request) {
    log.error("InfrastructureException. path: {}", request.getRequestURI(), e);
    HttpStatus status = getHttpStatus(e.getErrorCode());
    return ResponseEntity.status(status).body(
        new ErrorResponse(Instant.now(), status.value(),
            e.getErrorCode(), "Unable to process request. Please try again later.", null)
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
    log.debug("Validation failed - fields: {}, path: {}", errors, request.getRequestURI());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(),
            ErrorCode.INVALID_INPUT, "Validation failed",
            errors));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
    log.error("Unexpected exception. path: {}", request.getRequestURI(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ErrorCode.INTERNAL_SERVER_ERROR, "Unable to process request. Please try again later.",
            null));
  }


  private HttpStatus getHttpStatus(ErrorCode errorCode) {
    return switch (errorCode) {
      case INVALID_WEBHOOK, INVALID_ORDER_STATE, INVALID_INPUT -> HttpStatus.BAD_REQUEST;
      case PRODUCT_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case SERVICE_ERROR, INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
      case PAYMENT_PROVIDER_ERROR -> HttpStatus.BAD_GATEWAY;
    };
  }

}
