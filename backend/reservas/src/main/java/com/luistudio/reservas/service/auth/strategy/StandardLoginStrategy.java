package com.luistudio.reservas.service.auth.strategy;

import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.security.JwtService;
import com.luistudio.reservas.service.DtoMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class StandardLoginStrategy implements LoginStrategy {

    private final JwtService jwtService;
    private final DtoMapper dtoMapper;

    public StandardLoginStrategy(JwtService jwtService, DtoMapper dtoMapper) {
        this.jwtService = jwtService;
        this.dtoMapper = dtoMapper;
    }

    @Override
    public boolean supports(UserEntity user) {
        return true;
    }

    @Override
    public LoginResponse buildResponse(UserEntity user) {
        String token = jwtService.generateToken(user.getId(), user.getRol().getNombre());
        return new LoginResponse(token, null, false, dtoMapper.toAuthUser(user), "Login correcto");
    }
}
