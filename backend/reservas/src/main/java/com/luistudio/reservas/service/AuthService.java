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
import com.luistudio.reservas.service.auth.strategy.LoginStrategy;
import com.luistudio.reservas.service.factory.SecurityEntityFactory;
import java.time.OffsetDateTime;
import java.util.List;
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
    private final List<LoginStrategy> loginStrategies;

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
        List<LoginStrategy> loginStrategies
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
        this.loginStrategies = loginStrategies;
    }

    @Transactional
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
    public LoginResponse verify2fa(TwoFactorVerifyInput request) {
        JwtService.ParsedToken parsed = jwtService.validate(request.provisionalToken());
        if (!parsed.provisional()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Token no es provisional");
        }

        UserEntity user = userRepository.findById(parsed.userId())
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        TwoFactorCodeEntity latest = twoFactorCodeRepository.findTopByUsuarioAndUsadoFalseOrderByIdDesc(user)
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "No existe codigo 2FA activo"));

        if (latest.getExpiraAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Codigo 2FA expirado");
        }

        if (!latest.getCode().equals(request.code())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Codigo 2FA invalido");
        }

        latest.setUsado(true);
        twoFactorCodeRepository.save(latest);

        String token = jwtService.generateToken(user.getId(), user.getCorreo(), user.getRol().getNombre());
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
            PasswordResetEntity reset = securityEntityFactory.newPasswordReset(user, 30);
            passwordResetRepository.save(reset);

            emailOutboxService.enqueue(
                user,
                "Recuperacion de contrasena",
                "Tu token de recuperacion es: " + reset.getToken(),
                null
            );
        });
    }

    @Transactional
    public void confirmReset(ResetConfirmInput request) {
        passwordResetRepository.deleteByExpiraEnBefore(OffsetDateTime.now());

        PasswordResetEntity reset = passwordResetRepository.findByTokenAndUsadoFalse(request.token())
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Token invalido o expirado"));

        if (reset.getExpiraEn().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Token expirado");
        }

        UserEntity user = reset.getUsuario();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setActualizadoEn(OffsetDateTime.now());
        userRepository.save(user);

        reset.setUsado(true);
        passwordResetRepository.save(reset);
    }

    @Transactional
    public void enroll2fa(Long userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        TwoFactorCodeEntity twoFactor = securityEntityFactory.newTwoFactorCode(user, 10);
        twoFactorCodeRepository.save(twoFactor);

        emailOutboxService.enqueue(user, "Activacion de 2FA", "Codigo para activar 2FA: " + twoFactor.getCode(), null);
    }

    @Transactional
    public void verify2faEnrollment(Long userId, TwoFactorCodeInput request) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        TwoFactorCodeEntity latest = twoFactorCodeRepository.findTopByUsuarioAndUsadoFalseOrderByIdDesc(user)
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "No existe codigo 2FA"));

        if (latest.getExpiraAt().isBefore(OffsetDateTime.now()) || !latest.getCode().equals(request.code())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Codigo invalido o expirado");
        }

        latest.setUsado(true);
        user.setHas2fa(true);
        userRepository.save(user);
        twoFactorCodeRepository.save(latest);
    }

    @Transactional
    public void disable2fa(Long userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        user.setHas2fa(false);
        userRepository.save(user);
    }

    private void registerAttempt(UserEntity user, boolean success, String ipAddress) {
        LoginAttemptEntity attempt = securityEntityFactory.newLoginAttempt(user, success, ipAddress);
        loginAttemptRepository.save(attempt);
    }

    private LoginStrategy resolveLoginStrategy(UserEntity user) {
        return loginStrategies.stream()
            .filter(strategy -> strategy.supports(user))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No existe estrategia de login para el usuario"));
    }
}
