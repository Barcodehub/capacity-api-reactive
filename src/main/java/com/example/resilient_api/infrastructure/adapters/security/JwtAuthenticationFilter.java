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

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        return jwtPort.validateAndExtractPayload(token)
                .flatMap(payload -> authenticateUser(payload, exchange, chain))
                .onErrorResume(ex -> {
                    log.error("Error validating JWT token: {}", ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    private boolean isPublicEndpoint(String path) {
        // Endpoints públicos (no requieren autenticación)
        return !path.contains("/enroll") && !path.contains("/unenroll") && !path.contains("/my-bootcamps")
                || path.startsWith("/actuator");
    }

    private Mono<Void> authenticateUser(JwtPayload payload, ServerWebExchange exchange, WebFilterChain chain) {
        // Asignar rol basado en isAdmin
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(Boolean.TRUE.equals(payload.isAdmin()) ? "ROLE_ADMIN" : "ROLE_USER")
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(payload, null, authorities);

        // Store payload in exchange attributes for later use
        exchange.getAttributes().put("jwtPayload", payload);

        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }
}
