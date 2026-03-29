package knu.dykf.landom.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname
) {}