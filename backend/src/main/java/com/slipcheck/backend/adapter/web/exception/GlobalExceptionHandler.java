package com.slipcheck.backend.adapter.web.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import com.slipcheck.backend.adapter.web.dto.Dtos.ErrorBody;

import java.util.stream.Collectors;

/** Consistent error body for the whole API: {"error": "CODE", "message": "..."}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorBody("VALIDATION", "Invalid or missing fields: " + detail));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBody> unreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErrorBody("BAD_REQUEST", "Malformed request body"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorBody> tooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(new ErrorBody("IMAGE_TOO_LARGE", "The image exceeds the 4 MB limit"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorBody> conflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody("CONFLICT", "Conflicts with existing data"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorBody> status(ResponseStatusException e) {
        String reason = e.getReason() == null ? "Request failed" : e.getReason();
        return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorBody(String.valueOf(e.getStatusCode().value()), reason));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> generic(Exception e) {
        log.error("unhandled error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody("INTERNAL", "Something went wrong"));
    }

    @ExceptionHandler(EmailTakenException.class)
    public ResponseEntity<ErrorBody> emailTaken(EmailTakenException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody("EMAIL_TAKEN", e.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorBody> invalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorBody("INVALID_CREDENTIALS", e.getMessage()));
    }

    @ExceptionHandler(SellerNotFoundException.class)
    public ResponseEntity<ErrorBody> sellerNotFound(SellerNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody("SELLER_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(ChannelNotFoundException.class)
    public ResponseEntity<ErrorBody> channelNotFound(ChannelNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody("CHANNEL_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(InvalidTimeZoneException.class)
    public ResponseEntity<ErrorBody> invalidTimeZone(InvalidTimeZoneException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorBody("INVALID_TIME_ZONE", e.getMessage()));
    }

    @ExceptionHandler(UnsupportedChannelException.class)
    public ResponseEntity<ErrorBody> unsupportedChannel(UnsupportedChannelException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorBody("UNSUPPORTED_CHANNEL", e.getMessage()));
    }

    @ExceptionHandler(CannotUnlinkWebChannelException.class)
    public ResponseEntity<ErrorBody> cannotUnlinkWeb(CannotUnlinkWebChannelException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorBody("CANNOT_UNLINK_WEB", e.getMessage()));
    }
}
