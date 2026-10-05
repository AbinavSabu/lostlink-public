package com.yourteam.lostfound.controller;

import com.yourteam.lostfound.dto.AuthResponseDTO;
import com.yourteam.lostfound.dto.UserLoginDTO;
import com.yourteam.lostfound.dto.UserRegisterDTO;
import com.yourteam.lostfound.model.User;
import com.yourteam.lostfound.service.UserService;
import com.yourteam.lostfound.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@RequestBody UserRegisterDTO registerDTO) {
        User registeredUser = userService.registerUser(registerDTO);
        String role = registeredUser.getRole() != null ? registeredUser.getRole() : "ROLE_USER";

        String token = JwtUtil.generateToken(registeredUser.getEmail(), role);

        AuthResponseDTO response = new AuthResponseDTO(
                token,
                registeredUser.getId(),
                registeredUser.getName(),
                registeredUser.getEmail(),
                role
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody UserLoginDTO loginDTO) {
        User user = userService.authenticateUser(loginDTO);
        String role = user.getRole() != null ? user.getRole() : "ROLE_USER";

        String token = JwtUtil.generateToken(user.getEmail(), role);

        AuthResponseDTO response = new AuthResponseDTO(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                role
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        User user = userService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "",
                "role", user.getRole() != null ? user.getRole() : "ROLE_USER"
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        User user = userService.getCurrentAuthenticatedUser();
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        userService.changePassword(user.getId(), oldPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> request) {
        User user = userService.getCurrentAuthenticatedUser();
        String name = request.get("name");
        String phoneNumber = request.get("phoneNumber");
        User updated = userService.updateProfile(user.getId(), name, phoneNumber);
        return ResponseEntity.ok(Map.of(
                "id", updated.getId(),
                "name", updated.getName(),
                "email", updated.getEmail(),
                "phoneNumber", updated.getPhoneNumber() != null ? updated.getPhoneNumber() : "",
                "role", updated.getRole() != null ? updated.getRole() : "ROLE_USER"
        ));
    }
}