package knu.dykf.landom.dto.response;

import knu.dykf.landom.exception.ErrorCode;
import org.springframework.http.ResponseEntity;

public record ErrorResponse(
        String code,
        String message
) {
    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ErrorResponse(errorCode.name(), errorCode.getMessage()));
    }
}