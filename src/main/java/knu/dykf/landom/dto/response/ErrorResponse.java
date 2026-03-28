package knu.dykf.landom.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import knu.dykf.landom.exception.ErrorCode;

@Getter
@Builder
public class ErrorResponse {
    private final String code;
    private final String message;

    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .code(errorCode.name())
                        .message(errorCode.getMessage())
                        .build());
    }
}