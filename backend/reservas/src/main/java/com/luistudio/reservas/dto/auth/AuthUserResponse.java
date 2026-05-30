package com.luistudio.reservas.dto.auth;

public record AuthUserResponse(
    Long id,
    String code,
    String firstName,
    String lastName,
    String email,
    String role,
    String status
) {
}
