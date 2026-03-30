package com.example.recomender.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeProfileResponse {
    private Long userId;
    private String name;
    private String email;
    private String role;
}
