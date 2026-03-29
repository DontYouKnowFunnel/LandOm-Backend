package knu.dykf.landom.controller;


import jakarta.validation.Valid;
import knu.dykf.landom.dto.request.UserPasswordUpdateRequest;
import knu.dykf.landom.dto.request.UserUpdateRequest;
import knu.dykf.landom.dto.request.UserWithdrawRequest;
import knu.dykf.landom.dto.response.UserInfoResponse;
import knu.dykf.landom.dto.response.UserUpdateResponse;
import knu.dykf.landom.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getMyInfo(userDetails.getUsername()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserUpdateResponse> updateMyInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateMyInfo(userDetails.getUsername(), request));
    }

    @PostMapping("/me/withdraw")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserWithdrawRequest request) {
        userService.withdraw(userDetails.getUsername(), request.password());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserPasswordUpdateRequest request) {
        userService.updatePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }
}
