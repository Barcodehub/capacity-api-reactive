package com.example.resilient_api.infrastructure.adapters.webclient;

import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.domain.model.TechnologySummary;
import com.example.resilient_api.infrastructure.adapters.webclient.dto.TechnologyIdsRequest;
import com.example.resilient_api.infrastructure.adapters.webclient.dto.TechnologySummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.example.resilient_api.domain.enums.TechnicalMessage.TECHNOLOGY_SERVICE_ERROR;
import static com.example.resilient_api.infrastructure.entrypoints.util.Constants.X_MESSAGE_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TechnologyWebClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${external.technology.base-url}")
    private String technologyBaseUrl;

    public Mono<Map<Long, Boolean>> checkTechnologiesExist(List<Long> technologyIds, String messageId) {
        log.info("Calling technology service to check technologies exist with messageId: {}", messageId);

        return webClientBuilder.build()
                .post()
                .uri(technologyBaseUrl + "/technology/check-exists")
                .header(X_MESSAGE_ID, messageId)
                .bodyValue(new TechnologyIdsRequest(technologyIds))
                .retrieve()
                .onStatus(status -> status.is5xxServerError(),
                    response -> {
                        log.error("Technology service returned 5xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                    })
                .onStatus(status -> status.is4xxClientError(),
                    response -> {
                        log.error("Technology service returned 4xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                    })
                .bodyToMono(Map.class)
                .cast(Map.class)
                .map(map -> (Map<Long, Boolean>) map)
                .doOnSuccess(result -> log.info("Successfully received response from technology service with messageId: {}", messageId))
                .doOnError(ex -> log.error("Error calling technology service for messageId: {}", messageId, ex))
                .onErrorResume(ex -> {
                    if (ex instanceof TechnicalException) {
                        return Mono.error(ex);
                    }
                    log.error("Unexpected error calling technology service for messageId: {}", messageId, ex);
                    return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                });
    }

    public Flux<TechnologySummary> getTechnologiesByIds(List<Long> technologyIds, String messageId) {
        log.info("Calling technology service to get technologies by ids with messageId: {}", messageId);

        return webClientBuilder.build()
                .post()
                .uri(technologyBaseUrl + "/technology/by-ids")
                .header(X_MESSAGE_ID, messageId)
                .bodyValue(new TechnologyIdsRequest(technologyIds))
                .retrieve()
                .onStatus(status -> status.is5xxServerError(),
                    response -> {
                        log.error("Technology service returned 5xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                    })
                .onStatus(status -> status.is4xxClientError(),
                    response -> {
                        log.error("Technology service returned 4xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                    })
                .bodyToFlux(TechnologySummaryResponse.class)
                .map(response -> new TechnologySummary(response.getId(), response.getName()))
                .doOnComplete(() -> log.info("Successfully received technologies from technology service with messageId: {}", messageId))
                .doOnError(ex -> log.error("Error calling technology service for messageId: {}", messageId, ex))
                .onErrorResume(ex -> {
                    if (ex instanceof TechnicalException) {
                        return Flux.error(ex);
                    }
                    log.error("Unexpected error calling technology service for messageId: {}", messageId, ex);
                    return Flux.error(new TechnicalException(TECHNOLOGY_SERVICE_ERROR));
                });
    }
}
