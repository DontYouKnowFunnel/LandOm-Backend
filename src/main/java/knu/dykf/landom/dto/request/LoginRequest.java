package knu.dykf.landom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {
    @Schema(description = "사용자 아이디", example = "testuser1")
    @NotBlank
    private String username;

    @Schema(description = "비밀번호", example = "mysecretpassword!")
    @NotBlank
    private String password;
}