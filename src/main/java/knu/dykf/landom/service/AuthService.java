package knu.dykf.landom.service;

import knu.dykf.landom.dto.request.LoginRequest;
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

        // Tip: 실제 운영 시에는 refreshToken을 DB나 Redis에 저장하여 검증 프로세스를 추가해야 합니다.

        return new TokenResponse(accessToken, refreshToken);
    }
}