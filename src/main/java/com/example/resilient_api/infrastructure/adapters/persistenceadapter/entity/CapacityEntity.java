package com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "capacities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CapacityEntity {
    @Id
    private Long id;
    private String name;
    private String description;
}

