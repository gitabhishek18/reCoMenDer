package com.example.recomender.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private String role;
}
