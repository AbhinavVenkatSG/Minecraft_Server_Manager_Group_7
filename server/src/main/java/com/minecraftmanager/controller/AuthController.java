package com.minecraftmanager.controller;

import com.minecraftmanager.dto.LoginRequest;
import com.minecraftmanager.dto.LoginResponse;
import com.minecraftmanager.dto.RegisterRequest;
import com.minecraftmanager.model.User;
import com.minecraftmanager.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return authService.login(request.getUsername(), request.getPassword())
                .map(user -> ResponseEntity.ok(new LoginResponse(
                        user.getApiToken(),
                        user.getId(),
                        user.getUsername()
                )))
                .orElse(ResponseEntity.status(401).build());
    }
    
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest request) {
        if (authService.register(request.getUsername(), request.getPassword()) != null) {
            return authService.login(request.getUsername(), request.getPassword())
                    .map(user -> ResponseEntity.ok(new LoginResponse(
                            user.getApiToken(),
                            user.getId(),
                            user.getUsername()
                    )))
                    .orElse(ResponseEntity.status(500).build());
        }
        return ResponseEntity.badRequest().build();
    }
}
