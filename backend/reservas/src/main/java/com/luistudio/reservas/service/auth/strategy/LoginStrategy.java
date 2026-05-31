package com.luistudio.reservas.service.auth.strategy;

import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.model.UserEntity;

public interface LoginStrategy {
    boolean supports(UserEntity user);

    LoginResponse buildResponse(UserEntity user);
}
