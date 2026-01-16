package com.example.resilient_api.domain.spi;

import com.example.resilient_api.domain.model.Capacity;
import com.example.resilient_api.domain.model.PaginationRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CapacityPersistencePort {
    Mono<Capacity> save(Capacity capacity);
    Mono<Boolean> existByName(String name);
    Flux<Long> findExistingIdsByIds(List<Long> ids);
    Flux<Capacity> findAllPaginated(PaginationRequest paginationRequest);
    Mono<Long> count();
    Flux<Long> findTechnologyIdsByCapacityId(Long capacityId);
}

