package com.example.resilient_api.infrastructure.adapters.security;

import com.example.resilient_api.domain.api.JwtPort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.JwtPayload;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtAdapter implements JwtPort {

    private final SecretKey secretKey;

    public JwtAdapter(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<JwtPayload> validateAndExtractPayload(String token) {
        return Mono.fromCallable(() -> {
            try {
                log.info("JwtAdapter - Starting token validation");
                log.debug("JwtAdapter - Token: {}", token.substring(0, Math.min(20, token.length())) + "...");

                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                Long userId = claims.get("userId", Long.class);
                String email = claims.getSubject();
                Boolean isAdmin = claims.get("isAdmin", Boolean.class);

                log.info("JwtAdapter - Token validated successfully for userId: {}, email: {}, isAdmin: {}",
                        userId, email, isAdmin);

                return new JwtPayload(userId, email, isAdmin);
            } catch (ExpiredJwtException ex) {
                log.error("JwtAdapter - Token expired: {}", ex.getMessage());
                throw new BusinessException(TechnicalMessage.TOKEN_EXPIRED);
            } catch (SignatureException | MalformedJwtException ex) {
                log.error("JwtAdapter - Invalid token signature or format: {}", ex.getMessage());
                throw new BusinessException(TechnicalMessage.TOKEN_INVALID);
            } catch (Exception ex) {
                log.error("JwtAdapter - Unexpected error validating token: {}", ex.getMessage(), ex);
                throw new BusinessException(TechnicalMessage.TOKEN_INVALID);
            }
        });
    }
}
