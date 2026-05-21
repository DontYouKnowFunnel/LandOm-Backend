package knu.dykf.landom.exception;

import knu.dykf.landom.dto.response.common.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriverException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("handleMethodArgumentNotValidException", e);

        String errorMessage = e.getBindingResult()
                .getAllErrors()
                .getFirst()
                .getDefaultMessage();

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(new ErrorResponse(
                        ErrorCode.INVALID_INPUT_VALUE.name(),
                        errorMessage
                ));
    }

    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.error("handleBindException", e);
        String errorMessage = e.getBindingResult()
                .getAllErrors()
                .getFirst()
                .getDefaultMessage();

        return ErrorResponse.toResponseEntity(ErrorCode.INVALID_INPUT_VALUE, errorMessage);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("handleConstraintViolationException", e);
        String errorMessage = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());

        return ErrorResponse.toResponseEntity(ErrorCode.INVALID_INPUT_VALUE, errorMessage);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("handleHttpMessageNotReadableException", e);
        return ErrorResponse.toResponseEntity(ErrorCode.INVALID_REQUEST_BODY);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        log.error("handleMissingServletRequestParameterException", e);
        return ErrorResponse.toResponseEntity(
                ErrorCode.MISSING_REQUEST_PARAMETER,
                e.getParameterName() + " 파라미터는 필수입니다."
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.error("handleMethodArgumentTypeMismatchException", e);
        return ErrorResponse.toResponseEntity(
                ErrorCode.INVALID_INPUT_VALUE,
                e.getName() + " 값의 타입이 올바르지 않습니다."
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.error("handleHttpRequestMethodNotSupportedException", e);
        return ErrorResponse.toResponseEntity(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.error("handleAccessDeniedException", e);
        return ErrorResponse.toResponseEntity(ErrorCode.HANDLE_ACCESS_DENIED);
    }

    @ExceptionHandler(JwtException.class)
    protected ResponseEntity<ErrorResponse> handleJwtException(JwtException e) {
        log.error("handleJwtException", e);
        return ErrorResponse.toResponseEntity(ErrorCode.INVALID_TOKEN);
    }

    @ExceptionHandler(DataAccessException.class)
    protected ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException e) {
        log.error("handleDataAccessException", e);
        return ErrorResponse.toResponseEntity(ErrorCode.DATABASE_ERROR);
    }

    @ExceptionHandler(RestClientException.class)
    protected ResponseEntity<ErrorResponse> handleRestClientException(RestClientException e) {
        log.error("handleRestClientException", e);
        return ErrorResponse.toResponseEntity(ErrorCode.LLM_SERVER_ERROR);
    }

    @ExceptionHandler(WebDriverException.class)
    protected ResponseEntity<ErrorResponse> handleWebDriverException(WebDriverException e) {
        log.error("handleWebDriverException", e);
        return ErrorResponse.toResponseEntity(ErrorCode.CRAWLING_FAILED);
    }

    @ExceptionHandler(JacksonException.class)
    protected ResponseEntity<ErrorResponse> handleJacksonException(JacksonException e) {
        log.error("handleJacksonException", e);
        return ErrorResponse.toResponseEntity(ErrorCode.EVENT_PAYLOAD_INVALID);
    }

    @ExceptionHandler(IOException.class)
    protected ResponseEntity<ErrorResponse> handleIOException(IOException e) {
        log.error("handleIOException", e);
        return ErrorResponse.toResponseEntity(ErrorCode.EVENT_PAYLOAD_INVALID);
    }

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("handleCustomException: {}", e.getErrorCode());
        return ErrorResponse.toResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("handleException", e);
        return ErrorResponse.toResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
