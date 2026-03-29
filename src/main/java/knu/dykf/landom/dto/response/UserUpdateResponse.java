package knu.dykf.landom.dto.response;

public record UserUpdateResponse(
        Long id,
        String username,
        String nickname
) {}
