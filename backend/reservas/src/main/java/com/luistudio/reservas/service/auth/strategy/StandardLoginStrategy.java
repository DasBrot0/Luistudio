package com.luistudio.reservas.service.auth.strategy;

import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.security.JwtService;
import com.luistudio.reservas.service.DtoMapper;
import com.luistudio.reservas.service.SessionService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StandardLoginStrategy implements LoginStrategy {

    private final JwtService jwtService;
    private final DtoMapper dtoMapper;
    private final SessionService sessionService;

    public StandardLoginStrategy(JwtService jwtService, DtoMapper dtoMapper, SessionService sessionService) {
        this.jwtService = jwtService;
        this.dtoMapper = dtoMapper;
        this.sessionService = sessionService;
    }

    @Override
    public LoginResponse buildResponse(UserEntity user, String ip, String userAgent) {
        String jti = UUID.randomUUID().toString();
        String token = jwtService.generateToken(user.getId(), user.getRol().getNombre(), jti);
        sessionService.createSession(user, jti, ip, userAgent);
        return new LoginResponse(token, null, false, dtoMapper.toAuthUser(user), "Login correcto");
    }
}
