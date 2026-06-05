package knu.dykf.landom.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    DUPLICATE_USERNAME(HttpStatus.BAD_REQUEST, "이미 사용 중인 아이디입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "등록되지 않은 사용자입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "기존과 동일한 비밀번호는 사용할 수 없습니다."),

    // Project
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "섹션을 찾을 수 없습니다."),
    LANDING_PAGE_SNAPSHOT_NOT_FOUND(HttpStatus.BAD_REQUEST, "저장된 랜딩 페이지 HTML이 없습니다."),
    SECTION_HTML_NOT_FOUND(HttpStatus.BAD_REQUEST, "섹션에 해당하는 HTML을 찾을 수 없습니다."),
    OPTIMIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, "개선안을 찾을 수 없습니다."),
    OPTIMIZATION_TARGET_MISMATCH(HttpStatus.BAD_REQUEST, "개선안 저장 대상이 요청 경로와 일치하지 않습니다."),

    // Token
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않거나 만료되었습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "잘못된 리프레시 토큰입니다."),

    // Common (Validation & Access)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문이 올바르지 않습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // Event & Analytics
    EVENT_PAYLOAD_INVALID(HttpStatus.BAD_REQUEST, "이벤트 payload 형식이 올바르지 않습니다."),
    EVENT_STORE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이벤트 저장 중 오류가 발생했습니다."),
    EVENT_QUERY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이벤트 조회 중 오류가 발생했습니다."),

    // Crawling
    CRAWLING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "랜딩 페이지 크롤링 중 오류가 발생했습니다."),

    // Server
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부에 에러가 발생했습니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 처리 중 오류가 발생했습니다."),
    CLICKHOUSE_SCHEMA_FILE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "ClickHouse 스키마 파일을 찾을 수 없습니다."),
    LLM_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "LLM 서버 내부에 에러가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
