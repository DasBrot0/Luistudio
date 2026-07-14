package com.luistudio.reservas.service;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.SensitiveChangeTokenEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.SensitiveChangeTokenRepository;
import com.luistudio.reservas.repository.UserRepository;
import com.luistudio.reservas.security.SecretHashService;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SensitiveChangeService {

    private final SensitiveChangeTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final SecretHashService secretHashService;
    private final AuditService auditService;

    public SensitiveChangeService(
        SensitiveChangeTokenRepository tokenRepository,
        UserRepository userRepository,
        SecretHashService secretHashService,
        AuditService auditService
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.secretHashService = secretHashService;
        this.auditService = auditService;
    }

    @Transactional
    public void confirmChange(String rawToken) {
        String hashedToken = secretHashService.hash(rawToken);
        SensitiveChangeTokenEntity entity = tokenRepository.findByTokenAndUsedFalse(hashedToken)
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Token inválido o ya usado"));

        if (entity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Token expirado");
        }

        UserEntity user = entity.getUsuario();
        entity.setUsed(true);
        tokenRepository.save(entity);

        dispatchAction(user, entity.getActionType(), entity.getPayload());
        auditService.record(user, "SENSITIVE_CHANGE_CONFIRMED", "sensitive_change_token",
            String.valueOf(entity.getId()), "actionType=" + entity.getActionType());
    }

    private void dispatchAction(UserEntity user, String actionType, String payload) {
        switch (actionType) {
            case "DISABLE_2FA" -> {
                user.setHas2fa(false);
                userRepository.save(user);
            }
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "Tipo de acción desconocido: " + actionType);
        }
    }

}
