package com.aalago.aalago.backend.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class ApiErrorHandler {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<Map<String, String>> api(ApiException ex) {
    return ResponseEntity.status(ex.status()).body(Map.of("message", ex.getMessage()));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<Map<String, String>> uploadTooLarge(MaxUploadSizeExceededException ex) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(Map.of("message", "Image file is too large. Maximum allowed size is 5 MB."));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, String>> any(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", ex.getMessage()));
  }
}
