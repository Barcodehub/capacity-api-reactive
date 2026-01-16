package com.example.resilient_api.domain.api;

import com.example.resilient_api.domain.model.Capacity;
import com.example.resilient_api.domain.model.CapacityWithTechnologies;
import com.example.resilient_api.domain.model.Page;
import com.example.resilient_api.domain.model.PaginationRequest;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface CapacityServicePort {
    Mono<Capacity> registerCapacity(Capacity capacity, String messageId);
    Mono<Map<Long, Boolean>> checkCapacitiesExist(List<Long> ids, String messageId);
    Mono<Page<CapacityWithTechnologies>> listCapacities(PaginationRequest paginationRequest, String messageId);
}

