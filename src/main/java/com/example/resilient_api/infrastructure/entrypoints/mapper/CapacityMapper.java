package com.example.resilient_api.infrastructure.entrypoints.mapper;

import com.example.resilient_api.domain.model.Capacity;
import com.example.resilient_api.infrastructure.entrypoints.dto.CapacityDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CapacityMapper {
    Capacity capacityDTOToCapacity(CapacityDTO capacityDTO);
}

