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
import com.luistudio.reservas.service.email.EmailTemplateService;
import java.time.OffsetDateTime;
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

    public TwoFactorService(
        UserRepository userRepository,
        TwoFactorCodeRepository twoFactorCodeRepository,
        JwtService jwtService,
        DtoMapper dtoMapper,
        EmailOutboxService emailOutboxService,
        EmailTemplateService emailTemplateService,
        SecurityCodeService securityCodeService,
        SecretHashService secretHashService
    ) {
        this.userRepository = userRepository;
        this.twoFactorCodeRepository = twoFactorCodeRepository;
        this.jwtService = jwtService;
        this.dtoMapper = dtoMapper;
        this.emailOutboxService = emailOutboxService;
        this.emailTemplateService = emailTemplateService;
        this.securityCodeService = securityCodeService;
        this.secretHashService = secretHashService;
    }

    @Transactional
    public LoginResponse verifyLoginCode(Long userId, String code) {
        UserEntity user = findUser(userId);
        TwoFactorCodeEntity latest = twoFactorCodeRepository.findTopByUsuarioAndUsadoFalseOrderByIdDesc(user)
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "No existe cÃ³digo 2FA activo"));

        if (latest.getExpiraAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CÃ³digo 2FA expirado");
        }

        if (!secretHashService.matches(code, latest.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CÃ³digo 2FA invÃ¡lido");
        }

        latest.setUsado(true);
        twoFactorCodeRepository.save(latest);

        String token = jwtService.generateToken(user.getId(), user.getRol().getNombre());
        return new LoginResponse(token, null, false, dtoMapper.toAuthUser(user), "2FA validado");
    }

    @Transactional
    public void sendLoginCode(UserEntity user) {
        SecurityCodeService.GeneratedTwoFactorCode generatedTwoFactor = securityCodeService.createTwoFactorCode(user, 10);
        TwoFactorCodeEntity twoFactor = generatedTwoFactor.entity();
        twoFactorCodeRepository.save(twoFactor);

        emailOutboxService.enqueue(
            user,
            "CÃ³digo de verificaciÃ³n 2FA",
            emailTemplateService.securityCode(
                "CÃ³digo de verificaciÃ³n 2FA",
                "Usa este cÃ³digo para completar tu inicio de sesiÃ³n.",
                generatedTwoFactor.rawCode()
            ),
            null
        );
    }

    @Transactional
    public void enroll(Long userId) {
        UserEntity user = findUser(userId);
        if (Boolean.TRUE.equals(user.getHas2fa())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "2FA ya estÃ¡ activado");
        }
        SecurityCodeService.GeneratedTwoFactorCode generatedTwoFactor = securityCodeService.createTwoFactorCode(user, 10);
        twoFactorCodeRepository.save(generatedTwoFactor.entity());
        emailOutboxService.enqueue(
            user,
            "ConfirmaciÃ³n de activaciÃ³n de 2FA",
            emailTemplateService.securityCode(
                "ConfirmaciÃ³n de activaciÃ³n de 2FA",
                "Recibimos una solicitud para activar la autenticaciÃ³n en dos pasos.",
                generatedTwoFactor.rawCode()
            ),
            null
        );
    }

    @Transactional
    public void verifyEnrollment(Long userId, TwoFactorCodeInput request) {
        UserEntity user = findUser(userId);
        TwoFactorCodeEntity latest = findLatestCode(user);

        if (latest.getExpiraAt().isBefore(OffsetDateTime.now()) || !secretHashService.matches(request.code(), latest.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CÃ³digo invÃ¡lido o expirado");
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
            throw new BusinessException(HttpStatus.BAD_REQUEST, "2FA ya estÃ¡ desactivado");
        }
        SecurityCodeService.GeneratedTwoFactorCode generatedTwoFactor = securityCodeService.createTwoFactorCode(user, 10);
        twoFactorCodeRepository.save(generatedTwoFactor.entity());
        emailOutboxService.enqueue(
            user,
            "ConfirmaciÃ³n de desactivaciÃ³n de 2FA",
            emailTemplateService.securityCode(
                "ConfirmaciÃ³n de desactivaciÃ³n de 2FA",
                "Recibimos una solicitud para desactivar la autenticaciÃ³n en dos pasos.",
                generatedTwoFactor.rawCode()
            ),
            null
        );
    }

    @Transactional
    public void confirmDisable(Long userId, TwoFactorCodeInput request) {
        UserEntity user = findUser(userId);
        if (!Boolean.TRUE.equals(user.getHas2fa())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "2FA ya estÃ¡ desactivado");
        }
        TwoFactorCodeEntity latest = findLatestCode(user);

        if (latest.getExpiraAt().isBefore(OffsetDateTime.now()) || !secretHashService.matches(request.code(), latest.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CÃ³digo invÃ¡lido o expirado");
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
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "No existe cÃ³digo 2FA"));
    }
}
