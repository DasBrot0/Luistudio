package com.luistudio.reservas.dto.user;

public record UserResponse(
    Long id,
    String code,
    String email,
    String firstName,
    String lastName,
    String status,
    String role
) {
}
