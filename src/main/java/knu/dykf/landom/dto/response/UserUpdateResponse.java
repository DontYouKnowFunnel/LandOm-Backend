package knu.dykf.landom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 정보 수정 응답")
public record UserUpdateResponse(
        @Schema(description = "회원 PK", example = "INTEGER")
        Long id,

        @Schema(description = "아이디", example = "user123")
        String username,

        @Schema(description = "닉네임", example = "nickname123")
        String nickname
) {}
