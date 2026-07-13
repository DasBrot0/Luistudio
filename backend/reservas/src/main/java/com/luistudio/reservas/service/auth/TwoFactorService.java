package com.luistudio.reservas.service.auth;

import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.dto.auth.TwoFactorCodeInput;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.model.TwoFactorCodeEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.TwoFactorCodeRepository;
import com.luistudio.reservas.repository.UserRepository;
import com.luistudio.reservas.security.JwtService;
import com.luistudio.reservas.security.SecretHashService;
import com.luistudio.reservas.service.DtoMapper;
import com.luistudio.reservas.service.EmailOutboxService;
import com.luistudio.reservas.service.SessionService;
import com.luistudio.reservas.service.email.EmailTemplateService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TwoFactorService {

    private final UserRepository userRepository;
    private final TwoFactorCodeRepository twoFactorCodeRepository;
    private final JwtService jwtService;
    private final DtoMapper dtoMapper;
    private final EmailOutboxService emailOutboxService;
    private final EmailTemplateService emailTemplateService;
    private final SecurityCodeService securityCodeService;
    private final SecretHashService secretHashService;
    private final SessionService sessionService;

    public TwoFactorService(
        UserRepository userRepository,
        TwoFactorCodeRepository twoFactorCodeRepository,
        JwtService jwtService,
        DtoMapper dtoMapper,
        EmailOutboxService emailOutboxService,
        EmailTemplateService emailTemplateService,
        SecurityCodeService securityCodeService,
        SecretHashService secretHashService,
        SessionService sessionService
    ) {
        this.userRepository = userRepository;
        this.twoFactorCodeRepository = twoFactorCodeRepository;
        this.jwtService = jwtService;
        this.dtoMapper = dtoMapper;
        this.emailOutboxService = emailOutboxService;
        this.emailTemplateService = emailTemplateService;
        this.securityCodeService = securityCodeService;
        this.secretHashService = secretHashService;
        this.sessionService = sessionService;
    }

    @Transactional
    public LoginResponse verifyLoginCode(Long userId, String code, String ip, String userAgent) {
        UserEntity user = findUser(userId);
        TwoFactorCodeEntity latest = twoFactorCodeRepository.findTopByUsuarioAndUsadoFalseOrderByIdDesc(user)
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "No existe código 2FA activo"));

        if (latest.getExpiraAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Código 2FA expirado");
        }

        if (!secretHashService.matches(code, latest.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Código 2FA inválido");
        }

        latest.setUsado(true);
        twoFactorCodeRepository.save(latest);

        String jti = UUID.randomUUID().toString();
        String token = jwtService.generateToken(user.getId(), user.getRol().getNombre(), jti);
        sessionService.createSession(user, jti, ip, userAgent);
        return new LoginResponse(token, null, false, dtoMapper.toAuthUser(user), "2FA validado");
    }

    @Transactional
    public void sendLoginCode(UserEntity user) {
        SecurityCodeService.GeneratedTwoFactorCode generatedTwoFactor = securityCodeService.createTwoFactorCode(user, 10);
        TwoFactorCodeEntity twoFactor = generatedTwoFactor.entity();
        twoFactorCodeRepository.save(twoFactor);

        emailOutboxService.enqueueSecurity(
            user,
            "Código de verificación 2FA",
            emailTemplateService.securityCode(
                "Código de verificación 2FA",
                "Usa este código para completar tu inicio de sesión.",
                generatedTwoFactor.rawCode()
            )
        );
    }

    @Transactional
    public void enroll(Long userId) {
        UserEntity user = findUser(userId);
        if (Boolean.TRUE.equals(user.getHas2fa())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "2FA ya está activado");
        }
        SecurityCodeService.GeneratedTwoFactorCode generatedTwoFactor = securityCodeService.createTwoFactorCode(user, 10);
        twoFactorCodeRepository.save(generatedTwoFactor.entity());
        emailOutboxService.enqueueSecurity(
            user,
            "Confirmación de activación de 2FA",
            emailTemplateService.securityCode(
                "Confirmación de activación de 2FA",
                "Recibimos una solicitud para activar la autenticación en dos pasos.",
                generatedTwoFactor.rawCode()
            )
        );
    }

    @Transactional
    public void verifyEnrollment(Long userId, TwoFactorCodeInput request) {
        UserEntity user = findUser(userId);
        TwoFactorCodeEntity latest = findLatestCode(user);

        if (latest.getExpiraAt().isBefore(OffsetDateTime.now()) || !secretHashService.matches(request.code(), latest.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Código inválido o expirado");
        }

        latest.setUsado(true);
        user.setHas2fa(true);
        userRepository.save(user);
        twoFactorCodeRepository.save(latest);
    }

    @Transactional
    public void requestDisable(Long userId) {
        UserEntity user = findUser(userId);
        if (!Boolean.TRUE.equals(user.getHas2fa())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "2FA ya está desactivado");
        }
        SecurityCodeService.GeneratedTwoFactorCode generatedTwoFactor = securityCodeService.createTwoFactorCode(user, 10);
        twoFactorCodeRepository.save(generatedTwoFactor.entity());
        emailOutboxService.enqueueSecurity(
            user,
            "Confirmación de desactivación de 2FA",
            emailTemplateService.securityCode(
                "Confirmación de desactivación de 2FA",
                "Recibimos una solicitud para desactivar la autenticación en dos pasos.",
                generatedTwoFactor.rawCode()
            )
        );
    }

    @Transactional
    public void confirmDisable(Long userId, TwoFactorCodeInput request) {
        UserEntity user = findUser(userId);
        if (!Boolean.TRUE.equals(user.getHas2fa())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "2FA ya está desactivado");
        }
        TwoFactorCodeEntity latest = findLatestCode(user);

        if (latest.getExpiraAt().isBefore(OffsetDateTime.now()) || !secretHashService.matches(request.code(), latest.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Código inválido o expirado");
        }

        latest.setUsado(true);
        user.setHas2fa(false);
        userRepository.save(user);
        twoFactorCodeRepository.save(latest);
    }

    private UserEntity findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private TwoFactorCodeEntity findLatestCode(UserEntity user) {
        return twoFactorCodeRepository.findTopByUsuarioAndUsadoFalseOrderByIdDesc(user)
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "No existe código 2FA"));
    }
}
