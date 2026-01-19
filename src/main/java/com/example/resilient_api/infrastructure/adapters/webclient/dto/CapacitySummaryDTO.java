package com.example.resilient_api.infrastructure.adapters.webclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacitySummaryDTO {
    private Long id;
    private String name;
    private List<TechnologySummaryDTO> technologies;
}

