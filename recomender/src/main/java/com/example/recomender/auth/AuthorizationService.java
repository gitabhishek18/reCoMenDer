package com.example.recomender.auth;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final AuthenticatedUserService authenticatedUserService;

    public AuthorizationService(AuthenticatedUserService authenticatedUserService) {
        this.authenticatedUserService = authenticatedUserService;
    }

    public void ensureCurrentUserOrAdmin(Long targetUserId) {
        Long currentUserId = authenticatedUserService.getCurrentUserId();
        boolean isAdmin = authenticatedUserService.isAdmin();

        if (!currentUserId.equals(targetUserId) && !isAdmin) {
            throw new AccessDeniedException("You are not allowed to access this resource");
        }
    }
}