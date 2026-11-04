package com.slipcheck.backend.adapter.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slipcheck.backend.adapter.web.dto.Dtos.AuthResponse;
import com.slipcheck.backend.adapter.web.dto.Dtos.AuthResult;
import com.slipcheck.backend.adapter.web.dto.Dtos.LoginRequest;
import com.slipcheck.backend.adapter.web.dto.Dtos.RegisterRequest;
import com.slipcheck.backend.adapter.web.dto.Dtos.SellerDto;
import com.slipcheck.backend.adapter.web.service.AuthService;
import com.slipcheck.backend.persistence.entity.SellerEntity;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result = authService.register(request.email(), request.password(),
                request.displayName(), request.language());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request.email(), request.password());
        return ResponseEntity.ok(toResponse(result));
    }

    private static AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(result.token(), toDto(result.seller()));
    }

    static SellerDto toDto(SellerEntity seller) {
        return new SellerDto(seller.getId(), seller.getEmail(), seller.getDisplayName(),
                seller.getLanguage(), seller.getTimeZone());
    }
}