package com.luistudio.reservas.service;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.model.UserStatus;
import com.luistudio.reservas.repository.UserRepository;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.security.CurrentUserProvider;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AccessGuard {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;

    public AccessGuard(CurrentUserProvider currentUserProvider, UserRepository userRepository) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
    }

    public AuthPrincipal requireUser() {
        currentUserProvider.requireNotProvisionalToken();
        AuthPrincipal principal = currentUserProvider.requireCurrentUser();
        UserEntity user = userRepository.findById(principal.userId())
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Tu sesión ya no es válida. Vuelve a iniciar sesión."));
        if (user.getEstado() != UserStatus.HABILITADO) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Tu cuenta esta deshabilitada. Contacta al administrador.");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Tu cuenta esta bloqueada temporalmente. Intenta mas tarde.");
        }
        return principal;
    }

    public AuthPrincipal requireAdmin() {
        AuthPrincipal principal = requireUser();
        if (!currentUserProvider.isAdmin(principal)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Acceso solo para administradores");
        }
        return principal;
    }
}
