package com.luistudio.reservas.service.auth;

import com.luistudio.reservas.dto.auth.ResetConfirmInput;
import com.luistudio.reservas.dto.auth.ResetRequestInput;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.PasswordResetEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.PasswordResetRepository;
import com.luistudio.reservas.repository.UserRepository;
import com.luistudio.reservas.security.SecretHashService;
import com.luistudio.reservas.service.EmailOutboxService;
import com.luistudio.reservas.service.email.EmailTemplateService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretHashService secretHashService;
    private final SecurityCodeService securityCodeService;
    private final EmailOutboxService emailOutboxService;
    private final EmailTemplateService emailTemplateService;
    private final String resetPasswordUrl;

    public PasswordResetService(
        UserRepository userRepository,
        PasswordResetRepository passwordResetRepository,
        PasswordEncoder passwordEncoder,
        SecretHashService secretHashService,
        SecurityCodeService securityCodeService,
        EmailOutboxService emailOutboxService,
        EmailTemplateService emailTemplateService,
        @Value("${app.frontend.reset-password-url}") String resetPasswordUrl
    ) {
        this.userRepository = userRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.passwordEncoder = passwordEncoder;
        this.secretHashService = secretHashService;
        this.securityCodeService = securityCodeService;
        this.emailOutboxService = emailOutboxService;
        this.emailTemplateService = emailTemplateService;
        this.resetPasswordUrl = resetPasswordUrl;
    }

    @Transactional
    public void requestReset(ResetRequestInput request) {
        userRepository.findByCorreoIgnoreCase(request.email()).ifPresent(user -> {
            SecurityCodeService.GeneratedPasswordReset generatedReset = securityCodeService.createPasswordReset(user, 30);
            PasswordResetEntity reset = generatedReset.entity();
            passwordResetRepository.save(reset);
            String encodedToken = URLEncoder.encode(generatedReset.rawToken(), StandardCharsets.UTF_8);
            String separator = resetPasswordUrl.contains("?") ? "&" : "?";
            String resetLink = resetPasswordUrl + separator + "token=" + encodedToken;

            emailOutboxService.enqueue(
                user,
                "Recuperacion de contrasena",
                emailTemplateService.callToAction(
                    "Recuperacion de contrasena",
                    "Recibimos una solicitud para restablecer tu contrasena.",
                    resetLink
                ),
                null
            );
        });
    }

    @Transactional
    public void confirmReset(ResetConfirmInput request) {
        passwordResetRepository.deleteByExpiraEnBefore(OffsetDateTime.now());

        String tokenHash = secretHashService.hash(request.token());
        PasswordResetEntity reset = passwordResetRepository.findByTokenAndUsadoFalse(tokenHash)
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Token invalido o expirado"));

        if (reset.getExpiraEn().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Token expirado");
        }

        UserEntity user = reset.getUsuario();
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La nueva contrasena no puede ser igual a la anterior");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setActualizadoEn(OffsetDateTime.now());
        userRepository.save(user);

        reset.setUsado(true);
        passwordResetRepository.save(reset);
    }
}
