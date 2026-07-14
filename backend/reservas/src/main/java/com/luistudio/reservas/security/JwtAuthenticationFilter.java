package com.luistudio.reservas.security;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.LoginSessionEntity;
import com.luistudio.reservas.repository.LoginSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final LoginSessionRepository loginSessionRepository;
    private final String cookieName;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        LoginSessionRepository loginSessionRepository,
        @Value("${app.auth.cookie-name}") String cookieName
    ) {
        this.jwtService = jwtService;
        this.loginSessionRepository = loginSessionRepository;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && !token.isBlank()) {
            try {
                JwtService.ParsedToken parsed = jwtService.validate(token);

                // Validate JTI — only check for non-provisional tokens (provisional tokens are
                // ephemeral and not stored in login_sessions)
                if (!parsed.provisional()) {
                    Optional<LoginSessionEntity> session = loginSessionRepository.findByJti(parsed.jti());
                    if (session.isEmpty() || session.get().getRevokedAt() != null) {
                        throw new BusinessException(HttpStatus.UNAUTHORIZED, "Sesión revocada");
                    }
                    // Update last_seen_at lazily (fire-and-forget; no transaction needed here)
                    LoginSessionEntity s = session.get();
                    s.setLastSeenAt(OffsetDateTime.now());
                    loginSessionRepository.save(s);
                }

                AuthPrincipal principal = new AuthPrincipal(parsed.userId(), null, parsed.role());

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    token,
                    List.of(new SimpleGrantedAuthority("ROLE_" + parsed.role()))
                );

                authentication.setDetails(parsed);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug(
                    "jwt_authenticated endpoint={} actorRole={} userIdHash={} provisional={}",
                    request.getRequestURI(),
                    parsed.role(),
                    hashId(parsed.userId()),
                    parsed.provisional()
                );
            } catch (BusinessException ex) {
                log.warn(
                    "jwt_invalid endpoint={} optional={} message={}",
                    request.getRequestURI(),
                    isOptionalAuthenticationRequest(request),
                    sanitize(ex.getMessage())
                );
                if (isOptionalAuthenticationRequest(request)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"" + ex.getMessage() + "\"}");
                return;
            }
        } else {
            log.debug("jwt_absent endpoint={}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private boolean isOptionalAuthenticationRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/auth/login".equals(path)
            || "/api/auth/reset-request".equals(path)
            || "/api/auth/reset-confirm".equals(path)
            || "/api/auth/sensitive-change/confirm".equals(path)
            || "/actuator/health".equals(path);
    }

    private String hashId(Long userId) {
        return userId == null ? "anonymous" : Integer.toHexString(Long.hashCode(userId));
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "n/a";
        }
        return message.replaceAll("[\\r\\n]+", " ");
    }
}
