package se.jensen.johanna.fakestoreorderservice.dto;

import java.time.Instant;
import java.util.Map;
import se.jensen.johanna.fakestoreorderservice.exception.ErrorCode;

public record ErrorResponse(
    Instant timestamp,
    int status,
    ErrorCode errorCode,
    String message,
    Map<String, String> fieldErrors
) {

}
