package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.auth.AuthUserResponse;
import com.luistudio.reservas.dto.auth.LoginRequest;
import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.dto.auth.ResetConfirmInput;
import com.luistudio.reservas.dto.auth.ResetRequestInput;
import com.luistudio.reservas.dto.auth.TwoFactorCodeInput;
import com.luistudio.reservas.dto.auth.TwoFactorVerifyInput;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.model.LoginAttemptEntity;
import com.luistudio.reservas.model.PasswordResetEntity;
import com.luistudio.reservas.model.TwoFactorCodeEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.model.UserStatus;
import com.luistudio.reservas.repository.LoginAttemptRepository;
import com.luistudio.reservas.repository.PasswordResetRepository;
import com.luistudio.reservas.repository.TwoFactorCodeRepository;
import com.luistudio.reservas.repository.UserRepository;
import com.luistudio.reservas.security.JwtService;
import com.luistudio.reservas.security.SecretHashService;
import com.luistudio.reservas.service.auth.strategy.LoginStrategy;
import com.luistudio.reservas.service.factory.SecurityEntityFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final TwoFactorCodeRepository twoFactorCodeRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final DtoMapper dtoMapper;
    private final EmailOutboxService emailOutboxService;
    private final SecurityEntityFactory securityEntityFactory;
    private final SecretHashService secretHashService;
    private final List<LoginStrategy> loginStrategies;
    private final String resetPasswordUrl;

    public AuthService(
        UserRepository userRepository,
        LoginAttemptRepository loginAttemptRepository,
        PasswordResetRepository passwordResetRepository,
        TwoFactorCodeRepository twoFactorCodeRepository,
        JwtService jwtService,
        PasswordEncoder passwordEncoder,
        DtoMapper dtoMapper,
        EmailOutboxService emailOutboxService,
        SecurityEntityFactory securityEntityFactory,
        SecretHashService secretHashService,
        List<LoginStrategy> loginStrategies,
        @Value("${app.frontend.reset-password-url}") String resetPasswordUrl
    ) {
        this.userRepository = userRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.twoFactorCodeRepository = twoFactorCodeRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.dtoMapper = dtoMapper;
        this.emailOutboxService = emailOutboxService;
        this.securityEntityFactory = securityEntityFactory;
        this.secretHashService = secretHashService;
        this.loginStrategies = loginStrategies;
        this.resetPasswordUrl = resetPasswordUrl;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse login(LoginRequest request, String ipAddress) {
        UserEntity user = userRepository.findByCorreoIgnoreCase(request.email())
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));

        if (user.getEstado() == UserStatus.DESHABILITADO) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Cuenta deshabilitada");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Cuenta bloqueada temporalmente");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerAttempt(user, false, ipAddress);
            long failedLast15m = loginAttemptRepository.countByUsuarioAndExitoFalseAndFechaIntentoAfter(
                user,
                OffsetDateTime.now().minusMinutes(15)
            );

            if (failedLast15m >= 5) {
                user.setLockedUntil(OffsetDateTime.now().plusMinutes(15));
                userRepository.save(user);
                emailOutboxService.enqueue(
                    user,
                    "Intento no autorizado detectado",
                    "Alguien intento ingresar a tu cuenta. Si no fuiste tu, reportalo.",
                    null
                );
                throw new BusinessException(HttpStatus.FORBIDDEN, "Cuenta bloqueada temporalmente");
            }

            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        registerAttempt(user, true, ipAddress);
        user.setLockedUntil(null);
        userRepository.save(user);

        return resolveLoginStrategy(user).buildResponse(user);
    }

    @Transactional
    public LoginResponse verify2fa(Long userId, String code) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        TwoFactorCodeEntity latest = twoFactorCodeRepository.findTopByUsuarioAndUsadoFalseOrderByIdDesc(user)
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "No existe c\u00f3digo 2FA activo"));

        if (latest.getExpiraAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "C\u00f3digo 2FA expirado");
        }

        if (!secretHashService.matches(code, latest.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "C\u00f3digo 2FA inv\u00e1lido");
        }

        latest.setUsado(true);
        twoFactorCodeRepository.save(latest);

        String token = jwtService.generateToken(user.getId(), user.getRol().getNombre());
        return new LoginResponse(token, null, false, dtoMapper.toAuthUser(user), "2FA validado");
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        return dtoMapper.toAuthUser(user);
    }

    @Transactional
    public void requestReset(ResetRequestInput request) {
        userRepository.findByCorreoIgnoreCase(request.email()).ifPresent(user -> {
            SecurityEntityFactory.GeneratedPasswordReset generatedReset = securityEntityFactory.newPasswordReset(user, 30);
            PasswordResetEntity reset = generatedReset.entity();
            passwordResetRepository.save(reset);
            String encodedToken = URLEncoder.encode(generatedReset.rawToken(), StandardCharsets.UTF_8);
            String separator = resetPasswordUrl.contains("?") ? "&" : "?";
            String resetLink = resetPasswordUrl + separator + "token=" + encodedToken;

            emailOutboxService.enqueue(
                user,
                "Recuperaci\u00f3n de contrase\u00f1a",
                "Haz clic para restablecer tu contrase\u00f1a: " + resetLink,
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
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La nueva contrase\u00f1a no puede ser igual a la anterior");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setActualizadoEn(OffsetDateTime.now());
        userRepository.save(user);

        reset.setUsado(true);
        passwordResetRepository.save(reset);
    }

    @Transactional
    public void enroll2fa(Long userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        if (Boolean.TRUE.equals(user.getHas2fa())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "2FA ya esta activado");
        }
        SecurityEntityFactory.GeneratedTwoFactorCode generatedTwoFactor = securityEntityFactory.newTwoFactorCode(user, 10);
        TwoFactorCodeEntity twoFactor = generatedTwoFactor.entity();
        twoFactorCodeRepository.save(twoFactor);
        emailOutboxService.enqueue(
            user,
            "Confirmación de activación de 2FA",
            "Recibimos una solicitud para activar la autenticación en dos pasos.\nCódigo de confirmación: " + generatedTwoFactor.rawCode(),
            null
        );
    }

    @Transactional
    public void verify2faEnrollment(Long userId, TwoFactorCodeInput request) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        TwoFactorCodeEntity latest = twoFactorCodeRepository.findTopByUsuarioAndUsadoFalseOrderByIdDesc(user)
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "No existe c\u00f3digo 2FA"));

        if (latest.getExpiraAt().isBefore(OffsetDateTime.now()) || !secretHashService.matches(request.code(), latest.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "C\u00f3digo inv\u00e1lido o expirado");
        }

        latest.setUsado(true);
        user.setHas2fa(true);
        userRepository.save(user);
        twoFactorCodeRepository.save(latest);
    }

    @Transactional
    public void disable2fa(Long userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(user.getHas2fa())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "2FA ya est\u00e1 desactivado");
        }
        SecurityEntityFactory.GeneratedTwoFactorCode generatedTwoFactor = securityEntityFactory.newTwoFactorCode(user, 10);
        TwoFactorCodeEntity twoFactor = generatedTwoFactor.entity();
        twoFactorCodeRepository.save(twoFactor);
        emailOutboxService.enqueue(
            user,
            "Confirmación de desactivación de 2FA",
            "Recibimos una solicitud para desactivar la autenticación en dos pasos.\nCódigo de confirmación: " + generatedTwoFactor.rawCode(),
            null
        );
    }

    @Transactional
    public void confirmDisable2fa(Long userId, TwoFactorCodeInput request) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(user.getHas2fa())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "2FA ya est\u00e1 desactivado");
        }
        TwoFactorCodeEntity latest = twoFactorCodeRepository.findTopByUsuarioAndUsadoFalseOrderByIdDesc(user)
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "No existe c\u00f3digo 2FA"));

        if (latest.getExpiraAt().isBefore(OffsetDateTime.now()) || !secretHashService.matches(request.code(), latest.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "C\u00f3digo inv\u00e1lido o expirado");
        }

        latest.setUsado(true);
        user.setHas2fa(false);
        userRepository.save(user);
        twoFactorCodeRepository.save(latest);
    }

    private void registerAttempt(UserEntity user, boolean success, String ipAddress) {
        LoginAttemptEntity attempt = securityEntityFactory.newLoginAttempt(user, success, ipAddress);
        loginAttemptRepository.saveAndFlush(attempt);
    }

    private LoginStrategy resolveLoginStrategy(UserEntity user) {
        return loginStrategies.stream()
            .filter(strategy -> strategy.supports(user))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No existe estrategia de login para el usuario"));
    }
}


