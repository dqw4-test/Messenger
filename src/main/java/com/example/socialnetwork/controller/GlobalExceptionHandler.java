package com.example.socialnetwork.controller;

import com.example.socialnetwork.domain.dto.ApiErrorResponseDto;
import com.example.socialnetwork.domain.dto.ApiValidationErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                            HttpServletRequest request) {
        List<ApiValidationErrorDto> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toValidationError)
                .toList();
        ApiErrorResponseDto response = buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                validationErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponseDto> handleConstraintViolation(ConstraintViolationException ex,
                                                                         HttpServletRequest request) {
        List<ApiValidationErrorDto> validationErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ApiValidationErrorDto(
                        violation.getPropertyPath().toString(),
                        violation.getMessage(),
                        violation.getInvalidValue()
                ))
                .toList();
        ApiErrorResponseDto response = buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                validationErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponseDto> handleResponseStatusException(ResponseStatusException ex,
                                                                             HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        ApiErrorResponseDto response = buildResponse(
                status,
                ex.getReason(),
                request.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponseDto> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                            HttpServletRequest request) {
        ApiErrorResponseDto response = buildResponse(
                HttpStatus.CONFLICT,
                "Resource conflict: data already exists or violates integrity constraints",
                request.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiErrorResponseDto> handleBadRequest(Exception ex, HttpServletRequest request) {
        ApiErrorResponseDto response = buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request data",
                request.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({TaskRejectedException.class, RejectedExecutionException.class})
    public ResponseEntity<ApiErrorResponseDto> handleTaskRejected(Exception ex, HttpServletRequest request) {
        ApiErrorResponseDto response = buildResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests: task queue is overloaded, please retry later",
                request.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex,
                                                                           HttpServletRequest request) {
        ApiErrorResponseDto response = buildResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Uploaded file is too large",
                request.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDto> handleUnhandled(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on path {}", request.getRequestURI(), ex);
        ApiErrorResponseDto response = buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected internal error",
                request.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ApiValidationErrorDto toValidationError(FieldError fieldError) {
        return new ApiValidationErrorDto(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()
        );
    }

    private ApiErrorResponseDto buildResponse(HttpStatus status,
                                              String message,
                                              String path,
                                              List<ApiValidationErrorDto> validationErrors) {
        return ApiErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .validationErrors(validationErrors)
                .build();
    }
}
