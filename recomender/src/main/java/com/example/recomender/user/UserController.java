package com.example.recomender.user;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    public record CreateUserRequest(@NotBlank String name) {}
    @PostMapping
    public User createUser(@Valid @RequestBody CreateUserRequest req) {
        User u=new User();
        u.setName(req.name().trim());
        userRepository.save(u);
        return u;
    }
    
}
