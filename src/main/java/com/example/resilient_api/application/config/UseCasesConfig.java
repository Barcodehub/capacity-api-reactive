package com.example.resilient_api.application.config;

import com.example.resilient_api.domain.api.CapacityServicePort;
import com.example.resilient_api.domain.spi.CapacityPersistencePort;
import com.example.resilient_api.domain.spi.TechnologyExternalServicePort;
import com.example.resilient_api.domain.usecase.CapacityUseCase;
import com.example.resilient_api.infrastructure.adapters.externalservice.TechnologyExternalServiceAdapter;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.CapacityPersistenceAdapter;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.mapper.CapacityEntityMapper;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.CapacityRepository;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.CapacityTechnologyRepository;
import com.example.resilient_api.infrastructure.adapters.webclient.TechnologyWebClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;

@Configuration
@RequiredArgsConstructor
public class UseCasesConfig {
    private final CapacityRepository capacityRepository;
    private final CapacityTechnologyRepository capacityTechnologyRepository;
    private final CapacityEntityMapper capacityEntityMapper;
    private final TechnologyWebClient technologyWebClient;
    private final DatabaseClient databaseClient;

    @Bean
    public CapacityPersistencePort capacityPersistencePort() {
        return new CapacityPersistenceAdapter(capacityRepository, capacityTechnologyRepository,
                capacityEntityMapper, databaseClient);
    }

    @Bean
    public TechnologyExternalServicePort technologyExternalServicePort() {
        return new TechnologyExternalServiceAdapter(technologyWebClient);
    }

    @Bean
    public CapacityServicePort capacityServicePort(CapacityPersistencePort capacityPersistencePort,
                                                    TechnologyExternalServicePort technologyExternalServicePort) {
        return new CapacityUseCase(capacityPersistencePort, technologyExternalServicePort);
    }
}
