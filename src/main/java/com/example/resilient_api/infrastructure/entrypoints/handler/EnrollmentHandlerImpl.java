package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.api.JwtPort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.JwtPayload;
import com.example.resilient_api.infrastructure.adapters.webclient.BootcampWebClient;
import com.example.resilient_api.infrastructure.entrypoints.dto.EnrollmentRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.example.resilient_api.infrastructure.entrypoints.util.Constants.X_MESSAGE_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentHandlerImpl {

    private static final String BEARER_PREFIX = "Bearer ";
    private final BootcampWebClient bootcampWebClient;
    private final JwtPort jwtPort;

    public Mono<ServerResponse> enrollUser(ServerRequest request) {
        String messageId = request.headers().firstHeader(X_MESSAGE_ID);
        if (messageId == null || messageId.isEmpty()) {
            messageId = UUID.randomUUID().toString();
        }

        log.info("Received enroll user request with messageId: {}", messageId);
        String finalMessageId = messageId;

        return extractJwtPayload(request)
                .flatMap(jwtPayload -> {
                    // Validar que NO sea admin
                    if (Boolean.TRUE.equals(jwtPayload.isAdmin())) {
                        log.warn("Admin user {} attempted to enroll in bootcamp", jwtPayload.userId());
                        return Mono.error(new BusinessException(TechnicalMessage.UNAUTHORIZED_ACTION));
                    }

                    return request.bodyToMono(EnrollmentRequestDTO.class)
                            .flatMap(enrollmentRequest ->
                                    bootcampWebClient.enrollInBootcamp(
                                            enrollmentRequest.getBootcampId(),
                                            jwtPayload.userId(),
                                            finalMessageId,
                                            request.headers().firstHeader(HttpHeaders.AUTHORIZATION)
                                    ));
                })
                .flatMap(response -> ServerResponse.status(201).bodyValue(response))
                .doOnSuccess(response -> log.info("Successfully processed enroll user request with messageId: {}", finalMessageId))
                .doOnError(error -> log.error("Error processing enroll user request with messageId: {}", finalMessageId, error));
    }

    public Mono<ServerResponse> unenrollUser(ServerRequest request) {
        String messageId = request.headers().firstHeader(X_MESSAGE_ID);
        if (messageId == null || messageId.isEmpty()) {
            messageId = UUID.randomUUID().toString();
        }

        log.info("Received unenroll user request with messageId: {}", messageId);
        String finalMessageId = messageId;

        Long bootcampId = Long.parseLong(request.pathVariable("bootcampId"));

        return extractJwtPayload(request)
                .flatMap(jwtPayload -> {
                    // Validar que NO sea admin
                    if (Boolean.TRUE.equals(jwtPayload.isAdmin())) {
                        log.warn("Admin user {} attempted to unenroll from bootcamp", jwtPayload.userId());
                        return Mono.error(new BusinessException(TechnicalMessage.UNAUTHORIZED_ACTION));
                    }

                    // Usar el userId del token (solo puede desinscribirse a sí mismo)
                    return bootcampWebClient.unenrollFromBootcamp(
                            bootcampId,
                            jwtPayload.userId(),
                            finalMessageId,
                            request.headers().firstHeader(HttpHeaders.AUTHORIZATION)
                    );
                })
                .flatMap(response -> ServerResponse.ok().bodyValue(response))
                .doOnSuccess(response -> log.info("Successfully processed unenroll user request with messageId: {}", finalMessageId))
                .doOnError(error -> log.error("Error processing unenroll user request with messageId: {}", finalMessageId, error));
    }

    public Mono<ServerResponse> getUserBootcamps(ServerRequest request) {
        String messageId = request.headers().firstHeader(X_MESSAGE_ID);
        if (messageId == null || messageId.isEmpty()) {
            messageId = UUID.randomUUID().toString();
        }

        log.info("Received get user bootcamps request with messageId: {}", messageId);
        String finalMessageId = messageId;

        return extractJwtPayload(request)
                .flatMap(jwtPayload ->
                        // Usar el userId del token (solo puede ver sus propios bootcamps)
                        bootcampWebClient.getUserBootcamps(
                                jwtPayload.userId(),
                                finalMessageId,
                                request.headers().firstHeader(HttpHeaders.AUTHORIZATION)
                        ))
                .flatMap(bootcamps -> ServerResponse.ok().bodyValue(bootcamps))
                .doOnSuccess(response -> log.info("Successfully processed get user bootcamps request with messageId: {}", finalMessageId))
                .doOnError(error -> log.error("Error processing get user bootcamps request with messageId: {}", finalMessageId, error));
    }

    private Mono<JwtPayload> extractJwtPayload(ServerRequest request) {
        String authHeader = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or invalid Authorization header");
            return Mono.error(new BusinessException(TechnicalMessage.TOKEN_REQUIRED));
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        return jwtPort.validateAndExtractPayload(token);
    }
}
