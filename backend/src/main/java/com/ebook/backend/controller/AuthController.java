package com.ebook.backend.controller;

import com.ebook.backend.dto.ApiResponse;
import com.ebook.backend.dto.AuthRequest;
import com.ebook.backend.dto.AuthResponse;
import com.ebook.backend.dto.RegisterRequest;
import com.ebook.backend.model.User;
import com.ebook.backend.repository.UserRepository;
import com.ebook.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        var user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        userRepository.save(user);
        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(new java.util.ArrayList<>())
                .build();
        var jwtToken = jwtService.generateToken(userDetails);
        AuthResponse response = AuthResponse.builder().token(jwtToken).email(user.getEmail()).build();
        return ResponseEntity.ok(ApiResponse.success(response, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(new java.util.ArrayList<>())
                .build();
        var jwtToken = jwtService.generateToken(userDetails);
        AuthResponse response = AuthResponse.builder().token(jwtToken).email(user.getEmail()).build();
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> getProfile(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(user, "Profile fetched successfully"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(@RequestBody User userDetails, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setFullName(userDetails.getFullName());
        // Add more fields if needed
        User saved = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(saved, "Profile updated successfully"));
    }
}
