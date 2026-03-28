package knu.dykf.landom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshTokenRequest {
    @Schema(description = "발급받았던 Refresh Token", example = "eyJhbG...")
    @NotBlank
    private String refreshToken;
}