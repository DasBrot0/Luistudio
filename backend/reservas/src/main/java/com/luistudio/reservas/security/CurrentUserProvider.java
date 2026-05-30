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
        if (authentication != null && Boolean.TRUE.equals(authentication.getDetails())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Se requiere validar 2FA");
        }
    }

    public boolean isAdmin(AuthPrincipal principal) {
        return "ADMIN".equalsIgnoreCase(principal.role());
    }
}
