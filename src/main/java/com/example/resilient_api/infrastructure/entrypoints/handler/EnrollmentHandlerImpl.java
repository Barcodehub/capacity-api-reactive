package com.example.resilient_api.infrastructure.entrypoints.handler;

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

    private final BootcampWebClient bootcampWebClient;

    public Mono<ServerResponse> enrollUser(ServerRequest request) {
        String messageId = request.headers().firstHeader(X_MESSAGE_ID);
        if (messageId == null || messageId.isEmpty()) {
            messageId = UUID.randomUUID().toString();
        }

        log.info("Received enroll user request with messageId: {}", messageId);
        String finalMessageId = messageId;

        // Obtener el payload del contexto (ya validado por el filtro JWT)
        JwtPayload jwtPayload = (JwtPayload) request.exchange().getAttributes().get("jwtPayload");

        if (jwtPayload == null) {
            log.warn("JWT payload not found in request context");
            return ServerResponse.status(401).bodyValue("Authentication required");
        }

        log.info("User {} (id: {}) attempting to enroll in bootcamp", jwtPayload.email(), jwtPayload.userId());

        return request.bodyToMono(EnrollmentRequestDTO.class)
                .flatMap(enrollmentRequest ->
                        bootcampWebClient.enrollInBootcamp(
                                enrollmentRequest.getBootcampId(),
                                jwtPayload.userId(),
                                finalMessageId,
                                request.headers().firstHeader(HttpHeaders.AUTHORIZATION)
                        ))
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

        // Obtener el payload del contexto (ya validado por el filtro JWT)
        JwtPayload jwtPayload = (JwtPayload) request.exchange().getAttributes().get("jwtPayload");

        if (jwtPayload == null) {
            log.warn("JWT payload not found in request context");
            return ServerResponse.status(401).bodyValue("Authentication required");
        }

        log.info("User {} (id: {}) attempting to unenroll from bootcamp {}",
                jwtPayload.email(), jwtPayload.userId(), bootcampId);

        // Usar el userId del token (solo puede desinscribirse a sí mismo)
        return bootcampWebClient.unenrollFromBootcamp(
                        bootcampId,
                        jwtPayload.userId(),
                        finalMessageId,
                        request.headers().firstHeader(HttpHeaders.AUTHORIZATION)
                )
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

        // Obtener el payload del contexto (ya validado por el filtro JWT)
        JwtPayload jwtPayload = (JwtPayload) request.exchange().getAttributes().get("jwtPayload");

        if (jwtPayload == null) {
            log.warn("JWT payload not found in request context");
            return ServerResponse.status(401).bodyValue("Authentication required");
        }

        log.info("Fetching bootcamps for user {} (id: {})", jwtPayload.email(), jwtPayload.userId());

        // Usar el userId del token (solo puede ver sus propios bootcamps)
        return bootcampWebClient.getUserBootcamps(
                        jwtPayload.userId(),
                        finalMessageId,
                        request.headers().firstHeader(HttpHeaders.AUTHORIZATION)
                )
                .flatMap(bootcamps -> ServerResponse.ok().bodyValue(bootcamps))
                .doOnSuccess(response -> log.info("Successfully processed get user bootcamps request with messageId: {}", finalMessageId))
                .doOnError(error -> log.error("Error processing get user bootcamps request with messageId: {}", finalMessageId, error));
    }
}
