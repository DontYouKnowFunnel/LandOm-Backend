package knu.dykf.landom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "사용자 아이디", example = "testuser1")
        @NotBlank
        String username,

        @Schema(description = "비밀번호", example = "mysecretpassword!")
        @NotBlank
        String password
) {}