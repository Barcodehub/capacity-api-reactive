package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.infrastructure.adapters.webclient.BootcampWebClient;
import com.example.resilient_api.infrastructure.adapters.webclient.dto.BootcampDTO;
import com.example.resilient_api.infrastructure.adapters.webclient.dto.BootcampWithCapacitiesDTO;
import com.example.resilient_api.infrastructure.adapters.webclient.dto.PageResponse;
import com.example.resilient_api.infrastructure.entrypoints.util.APIResponse;
import com.example.resilient_api.infrastructure.entrypoints.util.ErrorDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.Instant;
import java.util.List;

import static com.example.resilient_api.infrastructure.entrypoints.util.Constants.X_MESSAGE_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootcampHandlerImpl {

    private final BootcampWebClient bootcampWebClient;

    public Mono<ServerResponse> createBootcamp(ServerRequest request) {
        String messageId = getMessageId(request);
        return request.bodyToMono(BootcampDTO.class)
                .flatMap(bootcamp -> bootcampWebClient.createBootcamp(bootcamp, messageId)
                        .doOnSuccess(result -> log.info("Bootcamp created successfully with messageId: {}", messageId))
                )
                .flatMap(result -> ServerResponse.status(HttpStatus.CREATED)
                        .bodyValue("Bootcamp created successfully"))
                .contextWrite(Context.of(X_MESSAGE_ID, messageId))
                .doOnError(ex -> log.error("Error creating bootcamp for messageId: {}", messageId, ex))
                .onErrorResume(TechnicalException.class, ex -> handleTechnicalException(ex, messageId))
                .onErrorResume(ex -> handleUnexpectedException(ex, messageId));
    }

    public Mono<ServerResponse> listBootcamps(ServerRequest request) {
        String messageId = getMessageId(request);

        // Extraer parámetros de query
        int page = request.queryParam("page")
                .map(Integer::parseInt)
                .orElse(0);
        int size = request.queryParam("size")
                .map(Integer::parseInt)
                .orElse(10);
        String sortBy = request.queryParam("sortBy")
                .map(String::toUpperCase)
                .orElse("NAME");
        String sortDirection = request.queryParam("sortDirection")
                .map(String::toUpperCase)
                .orElse("ASC");

        return bootcampWebClient.listBootcamps(page, size, sortBy, sortDirection, messageId)
                .flatMap(pageResponse -> ServerResponse.ok().bodyValue(pageResponse))
                .contextWrite(Context.of(X_MESSAGE_ID, messageId))
                .doOnSuccess(response -> log.info("Bootcamps listed successfully with messageId: {}", messageId))
                .doOnError(ex -> log.error("Error listing bootcamps for messageId: {}", messageId, ex))
                .onErrorResume(TechnicalException.class, ex -> handleTechnicalException(ex, messageId))
                .onErrorResume(ex -> handleUnexpectedException(ex, messageId));
    }

    public Mono<ServerResponse> deleteBootcamp(ServerRequest request) {
        String messageId = getMessageId(request);
        Long id = Long.valueOf(request.pathVariable("id"));

        return bootcampWebClient.deleteBootcamp(id, messageId)
                .doOnSuccess(result -> log.info("Bootcamp deleted successfully with messageId: {}", messageId))
                .flatMap(result -> ServerResponse.status(HttpStatus.OK)
                        .bodyValue(result))
                .contextWrite(Context.of(X_MESSAGE_ID, messageId))
                .doOnError(ex -> log.error("Error deleting bootcamp for messageId: {}", messageId, ex))
                .onErrorResume(TechnicalException.class, ex -> handleTechnicalException(ex, messageId))
                .onErrorResume(ex -> handleUnexpectedException(ex, messageId));
    }

    private Mono<ServerResponse> handleTechnicalException(TechnicalException ex, String messageId) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                messageId,
                TechnicalMessage.INTERNAL_ERROR,
                List.of(buildErrorDTO(ex.getTechnicalMessage())));
    }

    private Mono<ServerResponse> handleUnexpectedException(Throwable ex, String messageId) {
        log.error("Unexpected error occurred for messageId: {}", messageId, ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                messageId,
                TechnicalMessage.INTERNAL_ERROR,
                List.of(ErrorDTO.builder()
                        .code(TechnicalMessage.INTERNAL_ERROR.getCode())
                        .message(TechnicalMessage.INTERNAL_ERROR.getMessage())
                        .build()));
    }

    private ErrorDTO buildErrorDTO(TechnicalMessage technicalMessage) {
        return ErrorDTO.builder()
                .code(technicalMessage.getCode())
                .message(technicalMessage.getMessage())
                .param(technicalMessage.getParam())
                .build();
    }

    private Mono<ServerResponse> buildErrorResponse(HttpStatus httpStatus, String identifier, TechnicalMessage error,
                                                    List<ErrorDTO> errors) {
        return Mono.defer(() -> {
            APIResponse apiErrorResponse = APIResponse
                    .builder()
                    .code(error.getCode())
                    .message(error.getMessage())
                    .identifier(identifier)
                    .date(Instant.now().toString())
                    .errors(errors)
                    .build();
            return ServerResponse.status(httpStatus)
                    .bodyValue(apiErrorResponse);
        });
    }

    private String getMessageId(ServerRequest serverRequest) {
        return serverRequest.headers().firstHeader(X_MESSAGE_ID);
    }
}

