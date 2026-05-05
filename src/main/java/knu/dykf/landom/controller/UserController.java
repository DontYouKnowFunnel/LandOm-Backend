package knu.dykf.landom.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import knu.dykf.landom.dto.request.UserPasswordUpdateRequest;
import knu.dykf.landom.dto.request.UserUpdateRequest;
import knu.dykf.landom.dto.request.UserWithdrawRequest;
import knu.dykf.landom.dto.response.UserInfoResponse;
import knu.dykf.landom.dto.response.UserUpdateResponse;
import knu.dykf.landom.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "사용자 관련 API (내 정보 관리)")
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "정보 조회 성공")
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getMyInfo(userDetails.getUsername()));
    }

    @Operation(summary = "내 정보 수정", description = "닉네임 등 내 정보를 수정합니다.")
    @ApiResponse(responseCode = "200", description = "정보 수정 성공")
    @PatchMapping("/me")
    public ResponseEntity<UserUpdateResponse> updateMyInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateMyInfo(userDetails.getUsername(), request));
    }

    @Operation(summary = "회원 탈퇴", description = "비밀번호를 재입력받아 회원 탈퇴를 처리합니다.")
    @ApiResponse(responseCode = "204", description = "회원 탈퇴 성공")
    @PostMapping("/me/withdraw")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserWithdrawRequest request) {
        userService.withdraw(userDetails.getUsername(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 변경", description = "기존 비밀번호 확인 후 새로운 비밀번호로 변경합니다.")
    @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공")
    @PatchMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserPasswordUpdateRequest request) {
        userService.updatePassword(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }
}
