package com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "capacity_technology")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CapacityTechonologyEntity {
    @Id
    private Long id;
    private Long capacityId;
    private Long technologyId;
}

