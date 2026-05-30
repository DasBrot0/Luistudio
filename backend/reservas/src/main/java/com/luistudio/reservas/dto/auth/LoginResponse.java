package com.luistudio.reservas.dto.auth;

public record LoginResponse(
    String token,
    String provisionalToken,
    boolean twoFactorRequired,
    AuthUserResponse user,
    String message
) {
}
