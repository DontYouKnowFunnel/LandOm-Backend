package knu.dykf.landom.service;

import knu.dykf.landom.dto.request.LoginRequest;
import knu.dykf.landom.dto.request.RefreshTokenRequest;
import knu.dykf.landom.dto.request.SignupRequest;
import knu.dykf.landom.dto.response.TokenResponse;
import knu.dykf.landom.entity.User;
import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import knu.dykf.landom.jwt.JwtUtil;
import knu.dykf.landom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User newUser = User.builder()
                .username(request.username())
                .password(encodedPassword)
                .nickname(request.nickname())
                .build();

        userRepository.save(newUser);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtUtil.createAccessToken(user.getUsername());
        String refreshToken = jwtUtil.createRefreshToken(user.getUsername());

        user.updateRefreshToken(refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        if (!jwtUtil.validateToken(request.refreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String username = jwtUtil.getUsernameFromToken(request.refreshToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.getRefreshToken().equals(request.refreshToken())) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        String newAccess = jwtUtil.createAccessToken(username);
        String newRefresh = jwtUtil.createRefreshToken(username);
        user.updateRefreshToken(newRefresh);

        return new TokenResponse(newAccess, newRefresh);
    }
}