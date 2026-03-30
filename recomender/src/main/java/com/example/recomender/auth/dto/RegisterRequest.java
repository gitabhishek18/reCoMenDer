package com.example.recomender.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "email is required")
    @Email(message = "email format is invalid")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min=6,message = "Password must be at least 6 characters")
    private String password;
}