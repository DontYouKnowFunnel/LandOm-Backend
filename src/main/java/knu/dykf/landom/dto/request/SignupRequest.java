package knu.dykf.landom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @Schema(description = "사용자 아이디 (고유값)", example = "testuser1")
        @NotBlank(message = "아이디(username)는 필수입니다.")
        String username,

        @Schema(description = "비밀번호", example = "mysecretpassword!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @Schema(description = "사용자 닉네임", example = "테스터")
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname
) {}