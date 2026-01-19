package com.example.resilient_api.infrastructure.adapters.webclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BootcampWithCapacitiesDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDate launchDate;
    private Integer duration;
    private List<CapacitySummaryDTO> capacities;
}

