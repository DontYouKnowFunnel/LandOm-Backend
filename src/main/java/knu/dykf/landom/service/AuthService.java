package knu.dykf.landom.service;

import knu.dykf.landom.dto.request.LoginRequest;
import knu.dykf.landom.dto.request.RefreshTokenRequest;
import knu.dykf.landom.dto.request.SignupRequest;
import knu.dykf.landom.dto.response.TokenResponse;
import knu.dykf.landom.entity.User;
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
    public Long signup(SignupRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = User.builder()
                .username(request.getUsername())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .build();

        return userRepository.save(newUser).getId();
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 아이디입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtUtil.createAccessToken(user.getUsername());
        String refreshToken = jwtUtil.createRefreshToken(user.getUsername());

        user.updateRefreshToken(refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {

        if (!jwtUtil.validateToken(request.getRefreshToken())) {
            throw new IllegalArgumentException("토큰이 만료되었습니다.");
        }

        String username = jwtUtil.getUsernameFromToken(request.getRefreshToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!user.getRefreshToken().equals(request.getRefreshToken())) {
            throw new IllegalArgumentException("잘못된 리프레시 토큰입니다.");
        }

        String newAccess = jwtUtil.createAccessToken(username);
        String newRefresh = jwtUtil.createRefreshToken(username);
        user.updateRefreshToken(newRefresh);

        return new TokenResponse(newAccess, newRefresh);
    }
}