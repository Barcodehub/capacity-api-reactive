package com.example.resilient_api.infrastructure.entrypoints.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityWithTechnologiesDTO {
    private Long id;
    private String name;
    private String description;
    private List<TechnologySummaryDTO> technologies;
}

