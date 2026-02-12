package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.Capacity;
import com.example.resilient_api.domain.model.CapacityWithTechnologies;
import com.example.resilient_api.domain.model.Page;
import com.example.resilient_api.domain.model.PaginationRequest;
import com.example.resilient_api.domain.model.TechnologySummary;
import com.example.resilient_api.domain.spi.CapacityPersistencePort;
import com.example.resilient_api.domain.spi.TechnologyExternalServicePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapacityUseCaseTest {

    @Mock
    private CapacityPersistencePort capacityPersistencePort;

    @Mock
    private TechnologyExternalServicePort technologyExternalServicePort;

    @InjectMocks
    private CapacityUseCase capacityUseCase;

    private Capacity validCapacity;
    private String messageId;

    @BeforeEach
    void setUp() {
        validCapacity = new Capacity(null, "Backend Development", "Backend technologies", List.of(1L, 2L, 3L));
        messageId = "test-message-id-123";
    }

    @Test
    void registerCapacity_WithValidData_ShouldReturnSavedCapacity() {
        // Arrange
        Capacity savedCapacity = new Capacity(1L, "Backend Development", "Backend technologies", List.of(1L, 2L, 3L));
        when(capacityPersistencePort.existByName(anyString())).thenReturn(Mono.just(false));
        when(technologyExternalServicePort.checkTechnologiesExist(anyList(), anyString()))
                .thenReturn(Mono.just(Map.of(1L, true, 2L, true, 3L, true)));
        when(capacityPersistencePort.save(any(Capacity.class))).thenReturn(Mono.just(savedCapacity));

        // Act & Assert
        StepVerifier.create(capacityUseCase.registerCapacity(validCapacity, messageId))
                .expectNext(savedCapacity)
                .verifyComplete();

        verify(capacityPersistencePort).existByName("Backend Development");
        verify(technologyExternalServicePort).checkTechnologiesExist(List.of(1L, 2L, 3L), messageId);
        verify(capacityPersistencePort).save(validCapacity);
    }

    @Test
    void registerCapacity_WithExistingName_ShouldThrowBusinessException() {
        // Arrange
        when(technologyExternalServicePort.checkTechnologiesExist(anyList(), anyString()))
                .thenReturn(Mono.just(Map.of(1L, true, 2L, true, 3L, true)));
        when(capacityPersistencePort.existByName(anyString())).thenReturn(Mono.just(true));

        // Act & Assert
        StepVerifier.create(capacityUseCase.registerCapacity(validCapacity, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.TECHNOLOGY_ALREADY_EXISTS)
                .verify();

        verify(technologyExternalServicePort).checkTechnologiesExist(List.of(1L, 2L, 3L), messageId);
        verify(capacityPersistencePort).existByName("Backend Development");
        verify(capacityPersistencePort, never()).save(any(Capacity.class));
    }

    @Test
    void registerCapacity_WithNullName_ShouldThrowBusinessException() {
        // Arrange
        Capacity invalidCapacity = new Capacity(null, null, "Description", List.of(1L, 2L, 3L));

        // Act & Assert
        StepVerifier.create(capacityUseCase.registerCapacity(invalidCapacity, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.TECHNOLOGY_NAME_REQUIRED)
                .verify();
    }

    @Test
    void registerCapacity_WithLessThanMinTechnologies_ShouldThrowBusinessException() {
        // Arrange
        Capacity invalidCapacity = new Capacity(null, "Name", "Description", List.of(1L, 2L));

        // Act & Assert
        StepVerifier.create(capacityUseCase.registerCapacity(invalidCapacity, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.CAPACITY_TECHNOLOGIES_MIN)
                .verify();
    }

    @Test
    void registerCapacity_WithMoreThanMaxTechnologies_ShouldThrowBusinessException() {
        // Arrange
        List<Long> tooManyTechs = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L);
        Capacity invalidCapacity = new Capacity(null, "Name", "Description", tooManyTechs);

        // Act & Assert
        StepVerifier.create(capacityUseCase.registerCapacity(invalidCapacity, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.CAPACITY_TECHNOLOGIES_MAX)
                .verify();
    }

    @Test
    void registerCapacity_WithDuplicateTechnologies_ShouldThrowBusinessException() {
        // Arrange
        Capacity invalidCapacity = new Capacity(null, "Name", "Description", List.of(1L, 2L, 1L, 3L));

        // Act & Assert
        StepVerifier.create(capacityUseCase.registerCapacity(invalidCapacity, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.CAPACITY_TECHNOLOGIES_DUPLICATED)
                .verify();
    }

    @Test
    void registerCapacity_WithNonExistingTechnology_ShouldThrowBusinessException() {
        // Arrange
        when(technologyExternalServicePort.checkTechnologiesExist(anyList(), anyString()))
                .thenReturn(Mono.just(Map.of(1L, true, 2L, false, 3L, true)));

        // Act & Assert
        StepVerifier.create(capacityUseCase.registerCapacity(validCapacity, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.TECHNOLOGIES_NOT_FOUND)
                .verify();

        verify(technologyExternalServicePort).checkTechnologiesExist(List.of(1L, 2L, 3L), messageId);
        verify(capacityPersistencePort, never()).save(any(Capacity.class));
    }

    @Test
    void listCapacities_ShouldReturnPagedCapacities() {
        // Arrange
        PaginationRequest paginationRequest = new PaginationRequest(0, 10,
                PaginationRequest.SortField.NAME, PaginationRequest.SortDirection.ASC);

        Capacity capacity1 = new Capacity(1L, "Backend", "Backend dev", List.of(1L, 2L, 3L));
        Capacity capacity2 = new Capacity(2L, "Frontend", "Frontend dev", List.of(4L, 5L, 6L));

        TechnologySummary tech1 = new TechnologySummary(1L, "Java");
        TechnologySummary tech2 = new TechnologySummary(2L, "Spring");
        TechnologySummary tech3 = new TechnologySummary(3L, "MySQL");

        when(capacityPersistencePort.count()).thenReturn(Mono.just(2L));
        when(capacityPersistencePort.findAllPaginated(any(PaginationRequest.class)))
                .thenReturn(Flux.just(capacity1, capacity2));
        when(capacityPersistencePort.findTechnologyIdsByCapacityId(1L))
                .thenReturn(Flux.just(1L, 2L, 3L));
        when(capacityPersistencePort.findTechnologyIdsByCapacityId(2L))
                .thenReturn(Flux.just(4L, 5L, 6L));
        when(technologyExternalServicePort.getTechnologiesByIds(List.of(1L, 2L, 3L), messageId))
                .thenReturn(Flux.just(tech1, tech2, tech3));
        when(technologyExternalServicePort.getTechnologiesByIds(List.of(4L, 5L, 6L), messageId))
                .thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(capacityUseCase.listCapacities(paginationRequest, messageId))
                .expectNextMatches(page ->
                    page.content().size() == 2 &&
                    page.totalElements() == 2 &&
                    page.page() == 0 &&
                    page.size() == 10
                )
                .verifyComplete();

        verify(capacityPersistencePort).count();
        verify(capacityPersistencePort).findAllPaginated(paginationRequest);
    }

    @Test
    void checkCapacitiesExist_WithExistingIds_ShouldReturnAllTrue() {
        // Arrange
        List<Long> ids = List.of(1L, 2L);
        when(capacityPersistencePort.findExistingIdsByIds(ids))
                .thenReturn(Flux.just(1L, 2L));

        // Act & Assert
        StepVerifier.create(capacityUseCase.checkCapacitiesExist(ids, messageId))
                .expectNextMatches(result ->
                    result.size() == 2 &&
                    result.get(1L) &&
                    result.get(2L)
                )
                .verifyComplete();
    }

    @Test
    void getCapacitiesWithTechnologies_WithValidIds_ShouldReturnCapacities() {
        // Arrange
        List<Long> ids = List.of(1L, 2L);
        Capacity capacity1 = new Capacity(1L, "Backend", "Backend dev", List.of(1L, 2L));
        TechnologySummary tech1 = new TechnologySummary(1L, "Java");
        TechnologySummary tech2 = new TechnologySummary(2L, "Spring");

        when(capacityPersistencePort.findAllByIdIn(ids))
                .thenReturn(Flux.just(capacity1));
        when(capacityPersistencePort.findTechnologyIdsByCapacityId(1L))
                .thenReturn(Flux.just(1L, 2L));
        when(technologyExternalServicePort.getTechnologiesByIds(List.of(1L, 2L), messageId))
                .thenReturn(Flux.just(tech1, tech2));

        // Act & Assert
        StepVerifier.create(capacityUseCase.getCapacitiesWithTechnologies(ids, messageId))
                .expectNextMatches(capacityWithTech ->
                    capacityWithTech.id().equals(1L) &&
                    capacityWithTech.technologies().size() == 2
                )
                .verifyComplete();
    }

    @Test
    void deleteCapacitiesByIds_WithValidIds_ShouldDeleteSuccessfully() {
        // Arrange
        List<Long> ids = List.of(1L, 2L);
        when(capacityPersistencePort.findTechnologyIdsByCapacityId(1L))
                .thenReturn(Flux.just(1L, 2L, 3L));
        when(capacityPersistencePort.findTechnologyIdsByCapacityId(2L))
                .thenReturn(Flux.just(4L, 5L));
        when(capacityPersistencePort.countTechnologyReferences(anyLong()))
                .thenReturn(Mono.just(1L));
        when(capacityPersistencePort.deleteById(anyLong())).thenReturn(Mono.empty());
        when(capacityPersistencePort.deleteCapacityTechnologiesByCapacityId(anyLong()))
                .thenReturn(Mono.empty());
        when(technologyExternalServicePort.notifyTechnologyReferencesDecrement(anyList(), anyString()))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(capacityUseCase.deleteCapacitiesByIds(ids, messageId))
                .verifyComplete();

        verify(capacityPersistencePort).deleteById(1L);
        verify(capacityPersistencePort).deleteById(2L);
        verify(capacityPersistencePort).deleteCapacityTechnologiesByCapacityId(1L);
        verify(capacityPersistencePort).deleteCapacityTechnologiesByCapacityId(2L);
    }
}
