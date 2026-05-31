package com.luistudio.reservas.service;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.security.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AccessGuard {

    private final CurrentUserProvider currentUserProvider;

    public AccessGuard(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    public AuthPrincipal requireUser() {
        currentUserProvider.requireNotProvisionalToken();
        return currentUserProvider.requireCurrentUser();
    }

    public AuthPrincipal requireAdmin() {
        AuthPrincipal principal = requireUser();
        if (!currentUserProvider.isAdmin(principal)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Acceso solo para administradores");
        }
        return principal;
    }
}
