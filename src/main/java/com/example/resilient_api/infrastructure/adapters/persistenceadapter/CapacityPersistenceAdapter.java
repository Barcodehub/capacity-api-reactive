package com.example.resilient_api.infrastructure.adapters.persistenceadapter;

import com.example.resilient_api.domain.model.Capacity;
import com.example.resilient_api.domain.model.PaginationRequest;
import com.example.resilient_api.domain.spi.CapacityPersistencePort;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.CapacityEntity;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.CapacityTechnologyEntity;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.mapper.CapacityEntityMapper;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.CapacityRepository;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.CapacityTechnologyRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@AllArgsConstructor
@Slf4j
public class CapacityPersistenceAdapter implements CapacityPersistencePort {

    private final CapacityRepository capacityRepository;
    private final CapacityTechnologyRepository capacityTechnologyRepository;
    private final CapacityEntityMapper capacityEntityMapper;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Capacity> save(Capacity capacity) {
        log.info("Saving capacity with name: {}", capacity.name());

        // Guardar la capacidad
        return capacityRepository.save(capacityEntityMapper.toEntity(capacity))
                .flatMap(savedCapacityEntity -> {
                    // Guardar las relaciones con tecnologías
                    List<Long> technologyIds = capacity.technologyIds();
                    if (technologyIds != null && !technologyIds.isEmpty()) {
                        return saveCapacityTechnologies(savedCapacityEntity.getId(), technologyIds)
                                .then(Mono.just(capacityEntityMapper.toModel(savedCapacityEntity)))
                                .map(savedCapacity -> new Capacity(
                                        savedCapacity.id(),
                                        savedCapacity.name(),
                                        savedCapacity.description(),
                                        technologyIds
                                ));
                    }
                    return Mono.just(capacityEntityMapper.toModel(savedCapacityEntity));
                })
                .doOnSuccess(savedCapacity -> log.info("Capacity saved successfully with id: {}", savedCapacity.id()))
                .doOnError(error -> log.error("Error saving capacity", error));
    }

    @Override
    public Mono<Boolean> existByName(String name) {
        return capacityRepository.findByName(name)
                .map(capacityEntity -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Flux<Long> findExistingIdsByIds(List<Long> ids) {
        return capacityRepository.findAllByIdIn(ids)
                .map(CapacityEntity::getId);
    }

    @Override
    public Flux<Capacity> findAllPaginated(PaginationRequest paginationRequest) {
        String orderBy = buildOrderByClause(paginationRequest);
        String query = """
                SELECT c.id, c.name, c.description
                FROM capacity c
                LEFT JOIN capacity_technology ct ON c.id = ct.capacity_id
                GROUP BY c.id, c.name, c.description
                ORDER BY %s
                LIMIT :limit OFFSET :offset
                """.formatted(orderBy);

        return databaseClient.sql(query)
                .bind("limit", paginationRequest.size())
                .bind("offset", paginationRequest.getOffset())
                .map((row, metadata) -> {
                    CapacityEntity entity = new CapacityEntity();
                    entity.setId(row.get("id", Long.class));
                    entity.setName(row.get("name", String.class));
                    entity.setDescription(row.get("description", String.class));
                    return entity;
                })
                .all()
                .map(capacityEntityMapper::toModel);
    }

    @Override
    public Mono<Long> count() {
        return capacityRepository.count();
    }

    @Override
    public Flux<Long> findTechnologyIdsByCapacityId(Long capacityId) {
        return capacityTechnologyRepository.findAllByCapacityId(capacityId)
                .map(CapacityTechnologyEntity::getTechnologyId);
    }

    private String buildOrderByClause(PaginationRequest paginationRequest) {
        String direction = paginationRequest.sortDirection() == PaginationRequest.SortDirection.ASC ? "ASC" : "DESC";

        return switch (paginationRequest.sortBy()) {
            case NAME -> "c.name " + direction;
            case TECHNOLOGY_COUNT -> "COUNT(ct.technology_id) " + direction + ", c.name ASC";
        };
    }

    private Mono<Void> saveCapacityTechnologies(Long capacityId, List<Long> technologyIds) {
        return Flux.fromIterable(technologyIds)
                .map(technologyId -> {
                    CapacityTechnologyEntity entity = new CapacityTechnologyEntity();
                    entity.setCapacityId(capacityId);
                    entity.setTechnologyId(technologyId);
                    return entity;
                })
                .flatMap(capacityTechnologyRepository::save)
                .then();
    }
}

