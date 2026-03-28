package knu.dykf.landom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @Schema(description = "발급받았던 Refresh Token", example = "eyJhbG...")
        @NotBlank
        String refreshToken
) {}