package com.example.recomender.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.recomender.auth.dto.AuthResponse;
import com.example.recomender.auth.dto.LoginRequest;
import com.example.recomender.auth.dto.RegisterRequest;
import com.example.recomender.user.Role;
import com.example.recomender.user.User;
import com.example.recomender.user.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    public AuthResponse register(RegisterRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("Email already registered");
        }
        User user=new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        User savedUser= userRepository.save(user);
        String token=jwtService.generateToken(savedUser);
        return new AuthResponse(token,
            savedUser.getId(), 
            savedUser.getEmail(), 
            savedUser.getRole().name());
    }
    public AuthResponse login(LoginRequest request){
        User user=userRepository.findByEmail(request.getEmail())
        .orElseThrow(()->new BadCredentialsException("invalid email or email not registered"));
        boolean passwordmatches=passwordEncoder.matches(request.getPassword(),user.getPasswordHash());
        if(!passwordmatches)throw new BadCredentialsException("incorrect password");
        String token=jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole().name());
    }
}
