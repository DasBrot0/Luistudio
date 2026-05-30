package com.luistudio.reservas.security;

public record AuthPrincipal(Long userId, String email, String role) {
}
