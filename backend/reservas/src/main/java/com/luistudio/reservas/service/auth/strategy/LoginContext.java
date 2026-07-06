package com.luistudio.reservas.service.auth.strategy;

import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.model.UserEntity;

public class LoginContext {

    private LoginStrategy loginStrategy;

    public void setLoginStrategy(LoginStrategy loginStrategy) {
        this.loginStrategy = loginStrategy;
    }

    public LoginResponse login(UserEntity user) {
        return loginStrategy.buildResponse(user, null, null);
    }

    public LoginResponse login(UserEntity user, String ip, String userAgent) {
        return loginStrategy.buildResponse(user, ip, userAgent);
    }
}
