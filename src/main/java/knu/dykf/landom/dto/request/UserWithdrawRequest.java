package knu.dykf.landom.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserWithdrawRequest(
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {}