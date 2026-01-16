package com.example.resilient_api.infrastructure.adapters.externalservice;

import com.example.resilient_api.domain.model.TechnologySummary;
import com.example.resilient_api.domain.spi.TechnologyExternalServicePort;
import com.example.resilient_api.infrastructure.adapters.webclient.TechnologyWebClient;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class TechnologyExternalServiceAdapter implements TechnologyExternalServicePort {

    private final TechnologyWebClient technologyWebClient;

    @Override
    public Mono<Map<Long, Boolean>> checkTechnologiesExist(List<Long> technologyIds, String messageId) {
        return technologyWebClient.checkTechnologiesExist(technologyIds, messageId);
    }

    @Override
    public Flux<TechnologySummary> getTechnologiesByIds(List<Long> technologyIds, String messageId) {
        return technologyWebClient.getTechnologiesByIds(technologyIds, messageId);
    }
}

