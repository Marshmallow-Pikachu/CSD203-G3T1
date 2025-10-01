// com.ratewise.exceptions.ApiExceptionHandler
package com.ratewise.exceptions;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.*;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ratewise")
public class ApiExceptionHandler {
  private Map<String,Object> body(String error, String hint){
    Map<String,Object> m = new LinkedHashMap<>();
    m.put("ok", false);
    m.put("error", error);        // frontend reads this
    if (hint != null && !hint.isBlank()) m.put("hint", hint);
    return m;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<?> badJson(HttpMessageNotReadableException ex){
    return ResponseEntity.badRequest()
      .body(body("Invalid JSON.", "Request body is malformed or has wrong data types."));

  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> beanValidation(MethodArgumentNotValidException ex){
    var first = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
    String msg = (first != null) ? first.getField()+": "+first.getDefaultMessage() : "Invalid request payload.";
    return ResponseEntity.badRequest().body(body(msg, null));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<?> typeMismatch(MethodArgumentTypeMismatchException ex){
    String msg = "Parameter '" + ex.getName() + "' has invalid value '" + ex.getValue() + "'.";
    return ResponseEntity.badRequest().body(body(msg, null));
  }

  @ExceptionHandler({ NumberFormatException.class, ClassCastException.class })
  public ResponseEntity<?> numberIssues(RuntimeException ex){
    return ResponseEntity.badRequest().body(body("One or more numeric fields are not numbers.", null));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> badInput(IllegalArgumentException ex){
    return ResponseEntity.badRequest().body(body(ex.getMessage(), null)); // e.g., "No inputs detected."
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<?> unprocessable(IllegalStateException ex){
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body(ex.getMessage(), null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> generic(Exception ex){
    return ResponseEntity.status(500).body(body("Unexpected server error.", null));
  }

  @ExceptionHandler(ApiValidationException.class)
  public ResponseEntity<?> validation(ApiValidationException ex) {
    Map<String,Object> m = new LinkedHashMap<>();
    m.put("ok", false);
    m.put("error", ex.getMessage());   // "Invalid input."
    m.put("errors", ex.getErrors());   // list of concrete messages
    return ResponseEntity.badRequest().body(m);
  }
}
