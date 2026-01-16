package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.Capacity;
import com.example.resilient_api.domain.model.CapacityWithTechnologies;
import com.example.resilient_api.domain.model.Page;
import com.example.resilient_api.domain.model.PaginationRequest;
import com.example.resilient_api.domain.model.TechnologySummary;
import com.example.resilient_api.domain.api.CapacityServicePort;
import com.example.resilient_api.domain.spi.CapacityPersistencePort;
import com.example.resilient_api.domain.spi.TechnologyExternalServicePort;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CapacityUseCase implements CapacityServicePort {

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 90;
    private static final int MIN_TECHNOLOGIES = 3;
    private static final int MAX_TECHNOLOGIES = 20;

    private final CapacityPersistencePort capacityPersistencePort;
    private final TechnologyExternalServicePort technologyExternalServicePort;

    public CapacityUseCase(CapacityPersistencePort capacityPersistencePort,
                           TechnologyExternalServicePort technologyExternalServicePort) {
        this.capacityPersistencePort = capacityPersistencePort;
        this.technologyExternalServicePort = technologyExternalServicePort;
    }

    @Override
    public Mono<Capacity> registerCapacity(Capacity capacity, String messageId) {
        return validateCapacity(capacity)
                .then(validateTechnologies(capacity.technologyIds()))
                .then(checkTechnologiesExistInExternalService(capacity.technologyIds(), messageId))
                .then(capacityPersistencePort.existByName(capacity.name()))
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.TECHNOLOGY_ALREADY_EXISTS)))
                .flatMap(exists -> capacityPersistencePort.save(capacity));
    }

    @Override
    public Mono<Map<Long, Boolean>> checkCapacitiesExist(List<Long> ids, String messageId) {
        if (ids == null || ids.isEmpty()) {
            return Mono.just(Map.of());
        }

        return capacityPersistencePort.findExistingIdsByIds(ids)
                .collect(Collectors.toSet())
                .map(existingIds -> ids.stream()
                        .collect(Collectors.toMap(
                                id -> id,
                                existingIds::contains
                        ))
                );
    }

    @Override
    public Mono<Page<CapacityWithTechnologies>> listCapacities(PaginationRequest paginationRequest, String messageId) {
        // Obtener el conteo total y las capacidades en paralelo
        Mono<Long> totalCount = capacityPersistencePort.count();
        Mono<List<Capacity>> capacities = capacityPersistencePort
                .findAllPaginated(paginationRequest)
                .collectList();

        return Mono.zip(totalCount, capacities)
                .flatMap(tuple -> {
                    Long total = tuple.getT1();
                    List<Capacity> capacityList = tuple.getT2();

                    if (capacityList.isEmpty()) {
                        return Mono.just(Page.of(List.of(), paginationRequest.page(), paginationRequest.size(), total));
                    }

                    // Enriquecer cada capacidad con sus tecnologías
                    return enrichCapacitiesWithTechnologies(capacityList, messageId)
                            .collectList()
                            .map(enrichedCapacities -> Page.of(
                                    enrichedCapacities,
                                    paginationRequest.page(),
                                    paginationRequest.size(),
                                    total
                            ));
                });
    }

    private reactor.core.publisher.Flux<CapacityWithTechnologies> enrichCapacitiesWithTechnologies(
            List<Capacity> capacities, String messageId) {

        return reactor.core.publisher.Flux.fromIterable(capacities)
                .concatMap(capacity ->
                    capacityPersistencePort.findTechnologyIdsByCapacityId(capacity.id())
                            .collectList()
                            .flatMap(techIds -> {
                                if (techIds.isEmpty()) {
                                    return Mono.just(new CapacityWithTechnologies(
                                            capacity.id(),
                                            capacity.name(),
                                            capacity.description(),
                                            List.of()
                                    ));
                                }

                                // Consultar las tecnologías al servicio externo
                                return technologyExternalServicePort.getTechnologiesByIds(techIds, messageId)
                                        .collectList()
                                        .map(technologies -> new CapacityWithTechnologies(
                                                capacity.id(),
                                                capacity.name(),
                                                capacity.description(),
                                                technologies
                                        ));
                            })
                );
    }

    private Mono<Void> validateCapacity(Capacity capacity) {
        if (capacity.name() == null || capacity.name().trim().isEmpty()) {
            return Mono.error(new BusinessException(TechnicalMessage.TECHNOLOGY_NAME_REQUIRED));
        }
        if (capacity.description() == null || capacity.description().trim().isEmpty()) {
            return Mono.error(new BusinessException(TechnicalMessage.TECHNOLOGY_DESCRIPTION_REQUIRED));
        }
        if (capacity.name().length() > MAX_NAME_LENGTH) {
            return Mono.error(new BusinessException(TechnicalMessage.TECHNOLOGY_NAME_TOO_LONG));
        }
        if (capacity.description().length() > MAX_DESCRIPTION_LENGTH) {
            return Mono.error(new BusinessException(TechnicalMessage.TECHNOLOGY_DESCRIPTION_TOO_LONG));
        }
        return Mono.empty();
    }

    private Mono<Void> validateTechnologies(List<Long> technologyIds) {
        // Validar que se proporcionen tecnologías
        if (technologyIds == null || technologyIds.isEmpty()) {
            return Mono.error(new BusinessException(TechnicalMessage.CAPACITY_TECHNOLOGIES_REQUIRED));
        }

        // Validar mínimo de tecnologías
        if (technologyIds.size() < MIN_TECHNOLOGIES) {
            return Mono.error(new BusinessException(TechnicalMessage.CAPACITY_TECHNOLOGIES_MIN));
        }

        // Validar máximo de tecnologías
        if (technologyIds.size() > MAX_TECHNOLOGIES) {
            return Mono.error(new BusinessException(TechnicalMessage.CAPACITY_TECHNOLOGIES_MAX));
        }

        // Validar que no haya tecnologías duplicadas
        if (technologyIds.size() != new HashSet<>(technologyIds).size()) {
            return Mono.error(new BusinessException(TechnicalMessage.CAPACITY_TECHNOLOGIES_DUPLICATED));
        }

        return Mono.empty();
    }

    private Mono<Void> checkTechnologiesExistInExternalService(List<Long> technologyIds, String messageId) {
        return technologyExternalServicePort.checkTechnologiesExist(technologyIds, messageId)
                .flatMap(existenceMap -> {
                    // Verificar que todas las tecnologías existan
                    boolean allExist = existenceMap.values().stream().allMatch(exists -> exists);
                    if (!allExist) {
                        return Mono.error(new BusinessException(TechnicalMessage.TECHNOLOGIES_NOT_FOUND));
                    }
                    return Mono.empty();
                });
    }
}


