package knu.dykf.landom.dto.response;

public record UserInfoResponse(
        Long id,
        String username,
        String nickname
) {}