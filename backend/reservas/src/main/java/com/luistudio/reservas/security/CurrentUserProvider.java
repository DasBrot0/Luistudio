package com.luistudio.reservas.security;

import com.luistudio.reservas.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public AuthPrincipal requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return principal;
    }

    public void requireNotProvisionalToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && isProvisional(authentication)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Se requiere validar 2FA");
        }
    }

    public void requireProvisionalToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !isProvisional(authentication)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Se requiere un token provisional válido");
        }
    }

    public String currentJti() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof JwtService.ParsedToken parsed) {
            return parsed.jti();
        }
        return null;
    }

    private boolean isProvisional(Authentication authentication) {
        if (authentication.getDetails() instanceof JwtService.ParsedToken parsed) {
            return parsed.provisional();
        }
        // Legacy fallback
        return Boolean.TRUE.equals(authentication.getDetails());
    }

    public boolean isAdmin(AuthPrincipal principal) {
        return "ADMIN".equalsIgnoreCase(principal.role());
    }
}

