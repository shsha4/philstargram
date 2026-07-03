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

    /**
     * 도메인 불변식 위반(예: 게시글 길이 초과, 자기 자신 팔로우). 도메인이 규칙을 스스로
     * 지키므로 web 계층에 중복 검증을 두지 않고, 여기서 400 으로 변환한다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(new ErrorResponse("INVALID_REQUEST", e.getMessage())));
    }
}
