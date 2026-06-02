package com.luistudio.reservas.service.auth.strategy;

import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.model.TwoFactorCodeEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.TwoFactorCodeRepository;
import com.luistudio.reservas.security.JwtService;
import com.luistudio.reservas.service.DtoMapper;
import com.luistudio.reservas.service.EmailOutboxService;
import com.luistudio.reservas.service.factory.SecurityEntityFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class TwoFactorLoginStrategy implements LoginStrategy {

    private final TwoFactorCodeRepository twoFactorCodeRepository;
    private final SecurityEntityFactory securityEntityFactory;
    private final EmailOutboxService emailOutboxService;
    private final JwtService jwtService;
    private final DtoMapper dtoMapper;

    public TwoFactorLoginStrategy(
        TwoFactorCodeRepository twoFactorCodeRepository,
        SecurityEntityFactory securityEntityFactory,
        EmailOutboxService emailOutboxService,
        JwtService jwtService,
        DtoMapper dtoMapper
    ) {
        this.twoFactorCodeRepository = twoFactorCodeRepository;
        this.securityEntityFactory = securityEntityFactory;
        this.emailOutboxService = emailOutboxService;
        this.jwtService = jwtService;
        this.dtoMapper = dtoMapper;
    }

    @Override
    public boolean supports(UserEntity user) {
        return Boolean.TRUE.equals(user.getHas2fa());
    }

    @Override
    public LoginResponse buildResponse(UserEntity user) {
        SecurityEntityFactory.GeneratedTwoFactorCode generatedTwoFactor = securityEntityFactory.newTwoFactorCode(user, 10);
        TwoFactorCodeEntity twoFactor = generatedTwoFactor.entity();
        twoFactorCodeRepository.save(twoFactor);

        emailOutboxService.enqueue(
            user,
            "C\u00f3digo de verificaci\u00f3n 2FA",
            "Usa este c\u00f3digo para completar tu inicio de sesi\u00f3n.\nC\u00f3digo de verificaci\u00f3n: " + generatedTwoFactor.rawCode(),
            null
        );

        String provisionalToken = jwtService.generateProvisionalToken(user.getId(), user.getRol().getNombre());

        return new LoginResponse(null, provisionalToken, true, dtoMapper.toAuthUser(user), "C\u00f3digo 2FA enviado");
    }
}

