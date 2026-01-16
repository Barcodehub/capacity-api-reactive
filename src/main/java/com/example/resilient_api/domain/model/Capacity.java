package com.example.resilient_api.domain.model;

import java.util.List;

public record Capacity (Long id, String name, String description, List<Long> technologyIds ) {
}
