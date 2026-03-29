package knu.dykf.landom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원 탈퇴")
public record UserWithdrawRequest(
        @Schema(description = "현재 비밀번호", example = "password1234!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {}