package com.luistudio.reservas.service.auth.strategy;

import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.security.JwtService;
import com.luistudio.reservas.service.DtoMapper;
import com.luistudio.reservas.service.auth.TwoFactorService;
import org.springframework.stereotype.Component;

@Component
public class TwoFactorLoginStrategy implements LoginStrategy {

    private final TwoFactorService twoFactorService;
    private final JwtService jwtService;
    private final DtoMapper dtoMapper;

    public TwoFactorLoginStrategy(
        TwoFactorService twoFactorService,
        JwtService jwtService,
        DtoMapper dtoMapper
    ) {
        this.twoFactorService = twoFactorService;
        this.jwtService = jwtService;
        this.dtoMapper = dtoMapper;
    }

    @Override
    public LoginResponse buildResponse(UserEntity user, String ip, String userAgent) {
        twoFactorService.sendLoginCode(user);
        String provisionalToken = jwtService.generateProvisionalToken(user.getId(), user.getRol().getNombre());
        // Session will be created in TwoFactorService.verifyLoginCode once 2FA is verified
        return new LoginResponse(null, provisionalToken, true, dtoMapper.toAuthUser(user), "Código 2FA enviado");
    }
}
