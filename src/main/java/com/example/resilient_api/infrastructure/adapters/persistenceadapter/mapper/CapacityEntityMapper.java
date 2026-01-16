package com.example.resilient_api.infrastructure.adapters.persistenceadapter.mapper;

import com.example.resilient_api.domain.model.Capacity;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.CapacityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CapacityEntityMapper {
    @Mapping(target = "technologyIds", ignore = true)
    Capacity toModel(CapacityEntity entity);

    @Mapping(target = "id", ignore = true)
    CapacityEntity toEntity(Capacity capacity);
}

