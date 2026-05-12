package knu.dykf.landom.dto.response.auth;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}