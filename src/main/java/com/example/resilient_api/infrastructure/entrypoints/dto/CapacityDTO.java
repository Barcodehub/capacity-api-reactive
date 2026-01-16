package com.example.resilient_api.infrastructure.entrypoints.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(toBuilder = true)
public class CapacityDTO {
    private Long id;
    private String name;
    private String description;
    private List<Long> technologyIds;

}
