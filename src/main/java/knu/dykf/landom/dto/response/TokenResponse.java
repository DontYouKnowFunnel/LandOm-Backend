package knu.dykf.landom.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}