package com.example.resilient_api.infrastructure.adapters.webclient;

import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.infrastructure.adapters.webclient.dto.BootcampDTO;
import com.example.resilient_api.infrastructure.adapters.webclient.dto.BootcampWithCapacitiesDTO;
import com.example.resilient_api.infrastructure.adapters.webclient.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.example.resilient_api.domain.enums.TechnicalMessage.TECHNOLOGY_SERVICE_ERROR;
import static com.example.resilient_api.infrastructure.entrypoints.util.Constants.X_MESSAGE_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootcampWebClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${external.bootcamp.base-url}")
    private String bootcampBaseUrl;

    public Mono<BootcampDTO> createBootcamp(BootcampDTO bootcampDTO, String messageId) {
        log.info("Calling bootcamp service to create bootcamp with messageId: {}", messageId);

        return webClientBuilder.build()
                .post()
                .uri(bootcampBaseUrl + "/bootcamp")
                .header(X_MESSAGE_ID, messageId)
                .bodyValue(bootcampDTO)
                .retrieve()
                .onStatus(status -> status.is5xxServerError(),
                    response -> {
                        log.error("Bootcamp service returned 5xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                    })
                .onStatus(status -> status.is4xxClientError(),
                    response -> {
                        log.error("Bootcamp service returned 4xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                    })
                .bodyToMono(BootcampDTO.class)
                .doOnSuccess(result -> log.info("Successfully created bootcamp with messageId: {}", messageId))
                .doOnError(ex -> log.error("Error calling bootcamp service for messageId: {}", messageId, ex))
                .onErrorResume(ex -> {
                    if (ex instanceof TechnicalException) {
                        return Mono.error(ex);
                    }
                    log.error("Unexpected error calling bootcamp service for messageId: {}", messageId, ex);
                    return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                });
    }

    public Mono<PageResponse<BootcampWithCapacitiesDTO>> listBootcamps(int page, int size, String sortBy,
                                                                         String sortDirection, String messageId) {
        log.info("Calling bootcamp service to list bootcamps with messageId: {}", messageId);

        String uri = String.format("%s/bootcamp?page=%d&size=%d&sortBy=%s&sortDirection=%s",
                bootcampBaseUrl, page, size, sortBy, sortDirection);

        return webClientBuilder.build()
                .get()
                .uri(uri)
                .header(X_MESSAGE_ID, messageId)
                .retrieve()
                .onStatus(status -> status.is5xxServerError(),
                    response -> {
                        log.error("Bootcamp service returned 5xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                    })
                .onStatus(status -> status.is4xxClientError(),
                    response -> {
                        log.error("Bootcamp service returned 4xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                    })
                .bodyToMono(new ParameterizedTypeReference<PageResponse<BootcampWithCapacitiesDTO>>() {})
                .doOnSuccess(result -> log.info("Successfully listed bootcamps with messageId: {}", messageId))
                .doOnError(ex -> log.error("Error calling bootcamp service for messageId: {}", messageId, ex))
                .onErrorResume(ex -> {
                    if (ex instanceof TechnicalException) {
                        return Mono.error(ex);
                    }
                    log.error("Unexpected error calling bootcamp service for messageId: {}", messageId, ex);
                    return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                });
    }

    public Mono<String> deleteBootcamp(Long id, String messageId) {
        log.info("Calling bootcamp service to delete bootcamp {} with messageId: {}", id, messageId);

        return webClientBuilder.build()
                .delete()
                .uri(bootcampBaseUrl + "/bootcamp/" + id)
                .header(X_MESSAGE_ID, messageId)
                .retrieve()
                .onStatus(status -> status.is5xxServerError(),
                    response -> {
                        log.error("Bootcamp service returned 5xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                    })
                .onStatus(status -> status.is4xxClientError(),
                    response -> {
                        log.error("Bootcamp service returned 4xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                    })
                .bodyToMono(String.class)
                .doOnSuccess(result -> log.info("Successfully deleted bootcamp with messageId: {}", messageId))
                .doOnError(ex -> log.error("Error calling bootcamp service for messageId: {}", messageId, ex))
                .onErrorResume(ex -> {
                    if (ex instanceof TechnicalException) {
                        return Mono.error(ex);
                    }
                    log.error("Unexpected error calling bootcamp service for messageId: {}", messageId, ex);
                    return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                });
    }
}

