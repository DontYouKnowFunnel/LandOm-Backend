package knu.dykf.landom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "내 정보 수정 요청")
public record UserUpdateRequest(
        @Schema(description = "변경할 닉네임", example = "new nickname")
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname
) {}