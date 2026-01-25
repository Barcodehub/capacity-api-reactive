package com.example.resilient_api.infrastructure.adapters.security;

import com.example.resilient_api.domain.api.JwtPort;
import com.example.resilient_api.domain.model.JwtPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtPort jwtPort;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().toString();

        log.info("JWT Filter - Processing request: {} {}", method, path);

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.info("JWT Filter - Authorization header: {}", authHeader != null ? "Present" : "Missing");

        // Si NO hay token, continuar sin autenticación
        // Spring Security decidirá si el endpoint requiere autenticación
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.info("JWT Filter - No Bearer token found, continuing without authentication (Spring Security will handle authorization)");
            return chain.filter(exchange);
        }

        // Si HAY token, validarlo y asignar rol
        String token = authHeader.substring(BEARER_PREFIX.length());
        log.info("JWT Filter - Validating JWT token");

        return jwtPort.validateAndExtractPayload(token)
                .flatMap(payload -> {
                    log.info("JWT Filter - Token validated successfully for user: {} (admin: {})",
                            payload.email(), payload.isAdmin());
                    return authenticateUser(payload, exchange, chain);
                })
                .onErrorResume(ex -> {
                    log.error("JWT Filter - Error validating JWT token: {}", ex.getMessage(), ex);
                    // Si el token es inválido, continuar sin autenticación
                    // Spring Security retornará 401 si el endpoint requiere autenticación
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> authenticateUser(JwtPayload payload, ServerWebExchange exchange, WebFilterChain chain) {
        // Asignar rol basado en isAdmin
        String role = Boolean.TRUE.equals(payload.isAdmin()) ? "ROLE_ADMIN" : "ROLE_USER";
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(role)
        );

        log.info("JWT Filter - Assigned role: {} for user: {}", role, payload.email());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(payload, null, authorities);

        // Store payload in exchange attributes for later use
        exchange.getAttributes().put("jwtPayload", payload);

        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }
}
