package com.luistudio.reservas.service.auth.strategy;

import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.model.UserEntity;

public interface LoginStrategy {
    LoginResponse buildResponse(UserEntity user, String ip, String userAgent);

    default LoginResponse buildResponse(UserEntity user) {
        return buildResponse(user, null, null);
    }
}
