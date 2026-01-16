package com.example.resilient_api.domain.spi;

import com.example.resilient_api.domain.model.TechnologySummary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface TechnologyExternalServicePort {
    Mono<Map<Long, Boolean>> checkTechnologiesExist(List<Long> technologyIds, String messageId);
    Flux<TechnologySummary> getTechnologiesByIds(List<Long> technologyIds, String messageId);
}

