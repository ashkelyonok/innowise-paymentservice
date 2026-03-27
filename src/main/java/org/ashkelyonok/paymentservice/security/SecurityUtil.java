package org.ashkelyonok.paymentservice.security;

import lombok.RequiredArgsConstructor;
import org.ashkelyonok.paymentservice.exception.InvalidPaymentOperationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final JwtUtil jwtUtil;

    public Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidPaymentOperationException("Unauthenticated access attempt");
        }

        Object credentials = authentication.getCredentials();

        if (!(credentials instanceof String token)) {
            throw new InvalidPaymentOperationException("Invalid security context: missing token");
        }

        return jwtUtil.extractUserId(token);
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

    public void checkOwnership(Long resourceOwnerId) {
        if (isAdmin()) {
            return;
        }

        Long currentUserId = getAuthenticatedUserId();

        if (!Objects.equals(currentUserId, resourceOwnerId)) {
            throw new AccessDeniedException("Access Denied: You do not own this resource.");
        }
    }
}