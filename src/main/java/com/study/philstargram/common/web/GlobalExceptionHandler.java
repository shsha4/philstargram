package com.study.philstargram.common.web;

import com.study.philstargram.common.exception.DuplicateException;
import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.common.response.ApiResponse;
import com.study.philstargram.common.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(new ErrorResponse("NOT_FOUND", e.getMessage())));
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(new ErrorResponse("DUPLICATE", e.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("invalid request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(new ErrorResponse("INVALID_REQUEST", message)));
    }
}
