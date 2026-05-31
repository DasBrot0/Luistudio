package com.luistudio.reservas.dto.user;

public record UserLookupResponse(
    String code,
    String firstName,
    String lastName,
    String fullName
) {
}
