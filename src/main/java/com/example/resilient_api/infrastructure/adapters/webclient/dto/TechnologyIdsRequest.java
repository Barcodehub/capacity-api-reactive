package com.example.resilient_api.infrastructure.adapters.webclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnologyIdsRequest {
    private List<Long> ids;
}

