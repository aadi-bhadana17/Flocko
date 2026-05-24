package com.kilgore.fooddeliveryapp.identity.controller;

import com.kilgore.fooddeliveryapp.identity.dto.request.SignupRequest;
import com.kilgore.fooddeliveryapp.identity.dto.request.LoginRequest;
import com.kilgore.fooddeliveryapp.identity.dto.response.LoginAuthResponse;
import com.kilgore.fooddeliveryapp.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    public LoginAuthResponse signupUser(@Valid @RequestBody SignupRequest request) {
        return authService.registerUser(request);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginAuthResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        LoginAuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public String logoutUser() {
        return authService.logoutUser();
    }
}
