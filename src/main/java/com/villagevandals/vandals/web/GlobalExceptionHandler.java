package com.villagevandals.vandals.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain failures into HTTP status codes so that an authorization failure is reported as
 * {@code 403} rather than as a {@code 500}.
 *
 * <p>Responses reuse the {@link Message} record, matching the body shape the controllers already
 * return on success.
 *
 * <p>Note for tests: {@code MockMvcBuilders.standaloneSetup} does not discover
 * {@code @RestControllerAdvice} beans. A controller test that asserts on these status codes must
 * register the handler explicitly with {@code .setControllerAdvice(new GlobalExceptionHandler())}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Message> handleAccessDenied(AccessDeniedException e) {
    LOG.warn("Access denied: {}", e.getMessage());
    return respond(HttpStatus.FORBIDDEN, e.getMessage(), "Access denied");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Message> handleIllegalArgument(IllegalArgumentException e) {
    LOG.warn("Bad request: {}", e.getMessage());
    return respond(HttpStatus.BAD_REQUEST, e.getMessage(), "Invalid request");
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Message> handleIllegalState(IllegalStateException e) {
    LOG.warn("Conflicting state: {}", e.getMessage());
    return respond(HttpStatus.CONFLICT, e.getMessage(), "Request conflicts with current state");
  }

  /** {@link Message} is backed by {@code Map.of}, which rejects nulls, so fall back to a default. */
  private ResponseEntity<Message> respond(HttpStatus status, String message, String fallback) {
    return ResponseEntity.status(status).body(Message.of(message == null ? fallback : message));
  }
}
