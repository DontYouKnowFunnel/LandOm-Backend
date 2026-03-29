package knu.dykf.landom.service;

import knu.dykf.landom.dto.request.UserPasswordUpdateRequest;
import knu.dykf.landom.dto.request.UserUpdateRequest;
import knu.dykf.landom.dto.response.UserInfoResponse;
import knu.dykf.landom.dto.response.UserUpdateResponse;
import knu.dykf.landom.entity.User;
import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import knu.dykf.landom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserInfoResponse getMyInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new UserInfoResponse(user.getId(), user.getUsername(), user.getNickname());
    }

    @Transactional
    public UserUpdateResponse updateMyInfo(String username, UserUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateNickname(request.nickname());

        return new UserUpdateResponse(user.getId(), user.getUsername(), user.getNickname());
    }

    @Transactional
    public void updatePassword(String username, UserPasswordUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. 현재 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 2. 새 비밀번호가 기존 비밀번호와 동일한지 확인
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.SAME_AS_OLD_PASSWORD);
        }

        // 3. 비밀번호 암호화 후 변경
        user.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void withdraw(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        userRepository.delete(user);
    }
}