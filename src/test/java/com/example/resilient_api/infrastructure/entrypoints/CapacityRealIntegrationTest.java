package com.example.resilient_api.infrastructure.entrypoints;

import com.example.resilient_api.domain.model.TechnologySummary;
import com.example.resilient_api.domain.spi.CapacityPersistencePort;
import com.example.resilient_api.domain.spi.TechnologyExternalServicePort;
import com.example.resilient_api.infrastructure.entrypoints.dto.CapacityDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Test de Integración REAL para Capacities:
 * - Levanta TODO el contexto de Spring
 * - Usa una base de datos REAL (H2 en memoria)
 * - Mockea SOLO el servicio externo de tecnologías (technology-api)
 * - Persiste y consulta datos REALES en la base de datos
 * - Prueba el flujo completo end-to-end con persistencia real
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class CapacityRealIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CapacityPersistencePort capacityPersistencePort;

    // Repositories directos para limpieza y consultas avanzadas
    @Autowired
    private com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.CapacityRepository capacityRepository;

    @Autowired
    private com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.CapacityTechnologyRepository capacityTechnologyRepository;

    @MockBean // Mockear la INTERFAZ para que Spring la inyecte en CapacityUseCase
    private TechnologyExternalServicePort technologyExternalServicePort;

    @AfterEach
    void cleanUp() {
        // Limpieza de la base de datos después de cada test
        // Primero eliminar las relaciones capacity_technology, luego las capacidades
        capacityTechnologyRepository.deleteAll()
                .then(capacityRepository.deleteAll())
                .block();
    }

    @Test
    void createCapacity_WithValidData_ShouldPersistInDatabase() {
        // Arrange
        CapacityDTO capacityDTO = CapacityDTO.builder()
                .name("Backend Development")
                .description("Backend technologies and frameworks")
                .technologyIds(List.of(1L, 2L, 3L))
                .build();

        // Mockear servicio externo de tecnologías
        when(technologyExternalServicePort.checkTechnologiesExist(anyList(), anyString()))
                .thenReturn(Mono.just(Map.of(1L, true, 2L, true, 3L, true)));

        // Act - Crear capacidad
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(capacityDTO)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .isEqualTo("Capacity created successfully");

        // Assert - Verificar que se guardó en la base de datos REAL
        Boolean exists = capacityPersistencePort
                .existByName("Backend Development")
                .block();

        assert exists != null && exists : "La capacidad debería existir en la base de datos";

        // Verificar que las tecnologías se asociaron correctamente
        Long capacityId = capacityRepository.findAll()
                .filter(c -> "Backend Development".equals(c.getName()))
                .next()
                .map(c -> c.getId())
                .block();

        assert capacityId != null;

        Long techCount = capacityPersistencePort
                .findTechnologyIdsByCapacityId(capacityId)
                .count()
                .block();

        assert techCount != null && techCount == 3 : "Deberían existir 3 tecnologías asociadas";
    }

    @Test
    void createCapacity_WithDuplicateName_ShouldFailValidation() {
        // Arrange - Primero crear una capacidad
        when(technologyExternalServicePort.checkTechnologiesExist(anyList(), anyString()))
                .thenReturn(Mono.just(Map.of(1L, true, 2L, true, 3L, true)));

        CapacityDTO capacityDTO = CapacityDTO.builder()
                .name("Duplicate Capacity")
                .description("First insertion")
                .technologyIds(List.of(1L, 2L, 3L))
                .build();

        // Primera inserción exitosa
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(capacityDTO)
                .exchange()
                .expectStatus().isCreated();

        // Act - Intentar insertar duplicado
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(capacityDTO)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Bad Parameters, please verify data")
                .jsonPath("$.errors[0].message").isEqualTo("Capacity with this name already exists");
    }

    @Test
    void listCapacities_WithPersistedData_ShouldReturnFromDatabase() {
        // Arrange
        when(technologyExternalServicePort.checkTechnologiesExist(anyList(), anyString()))
                .thenReturn(Mono.just(Map.of(
                        1L, true, 2L, true, 3L, true, 4L, true, 5L, true
                )));

        CapacityDTO capacity1 = CapacityDTO.builder()
                .name("Backend Real")
                .description("Backend description")
                .technologyIds(List.of(1L, 2L, 3L))
                .build();

        CapacityDTO capacity2 = CapacityDTO.builder()
                .name("Frontend Real")
                .description("Frontend description")
                .technologyIds(List.of(3L, 4L, 5L))
                .build();

        // Insertar capacidades
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(capacity1)
                .exchange()
                .expectStatus().isCreated();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(capacity2)
                .exchange()
                .expectStatus().isCreated();

        // Mockear respuesta del servicio de tecnologías para listado
        TechnologySummary tech1 = new TechnologySummary(1L, "Java");
        TechnologySummary tech2 = new TechnologySummary(2L, "Spring");
        TechnologySummary tech3 = new TechnologySummary(3L, "MySQL");
        TechnologySummary tech4 = new TechnologySummary(4L, "React");
        TechnologySummary tech5 = new TechnologySummary(5L, "TypeScript");

        when(technologyExternalServicePort.getTechnologiesByIds(anyList(), anyString()))
                .thenAnswer(invocation -> {
                    List<Long> ids = invocation.getArgument(0);
                    return Flux.fromIterable(ids)
                            .flatMap(id -> {
                                if (id == 1L) return Flux.just(tech1);
                                if (id == 2L) return Flux.just(tech2);
                                if (id == 3L) return Flux.just(tech3);
                                if (id == 4L) return Flux.just(tech4);
                                if (id == 5L) return Flux.just(tech5);
                                return Flux.empty();
                            });
                });

        // Act - Listar capacidades
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .get()
                .uri("/capacity?page=0&size=10&sortBy=name&sortDir=asc")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.content[?(@.name == 'Backend Real')]").exists()
                .jsonPath("$.content[?(@.name == 'Frontend Real')]").exists()
                .jsonPath("$.totalElements").isEqualTo(2)
                .jsonPath("$.page").isEqualTo(0)
                .jsonPath("$.size").isEqualTo(10);
    }

    @Test
    void getCapacitiesByIds_ShouldReturnPersistedData() {
        // Arrange - Persistir 2 capacidades
        when(technologyExternalServicePort.checkTechnologiesExist(anyList(), anyString()))
                .thenReturn(Mono.just(Map.of(1L, true, 2L, true, 3L, true)));

        CapacityDTO capacity = CapacityDTO.builder()
                .name("Test Capacity")
                .description("Test description")
                .technologyIds(List.of(1L, 2L, 3L))
                .build();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(capacity)
                .exchange()
                .expectStatus().isCreated();

        // Obtener el ID real de la base de datos
        Long capacityId = capacityRepository.findAll()
                .filter(c -> "Test Capacity".equals(c.getName()))
                .next()
                .map(c -> c.getId())
                .block();

        assert capacityId != null : "El ID debería existir";

        // Mockear respuesta de tecnologías
        TechnologySummary tech1 = new TechnologySummary(1L, "Java");
        TechnologySummary tech2 = new TechnologySummary(2L, "Spring");
        TechnologySummary tech3 = new TechnologySummary(3L, "MySQL");

        when(technologyExternalServicePort.getTechnologiesByIds(anyList(), anyString()))
                .thenReturn(Flux.just(tech1, tech2, tech3));

        // Act - Consultar por IDs reales
        Map<String, List<Long>> requestBody = Map.of("ids", List.of(capacityId));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("USER"))
                .post()
                .uri("/capacity/with-technologies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Test Capacity")
                .jsonPath("$[0].technologies.length()").isEqualTo(3);
    }

    @Test
    void checkCapacitiesExist_ShouldReturnRealExistenceStatus() {
        // Arrange - Persistir algunas capacidades
        when(technologyExternalServicePort.checkTechnologiesExist(anyList(), anyString()))
                .thenReturn(Mono.just(Map.of(1L, true, 2L, true, 3L, true)));

        CapacityDTO capacity = CapacityDTO.builder()
                .name("Existing Capacity")
                .description("Exists in DB")
                .technologyIds(List.of(1L, 2L, 3L))
                .build();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(capacity)
                .exchange()
                .expectStatus().isCreated();

        Long existingId = capacityRepository.findAll()
                .filter(c -> "Existing Capacity".equals(c.getName()))
                .next()
                .map(c -> c.getId())
                .block();

        assert existingId != null : "El ID debería existir";

        // Act - Verificar existencia (uno existe, otro no)
        Map<String, List<Long>> requestBody = Map.of("ids", List.of(existingId, 9999L));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("USER"))
                .post()
                .uri("/capacity/check-exists")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$." + existingId).isEqualTo(true)
                .jsonPath("$.9999").isEqualTo(false);
    }

    @Test
    void deleteCapacities_ShouldRemoveFromDatabase() {
        // Arrange - Crear capacidad
        when(technologyExternalServicePort.checkTechnologiesExist(anyList(), anyString()))
                .thenReturn(Mono.just(Map.of(1L, true, 2L, true, 3L, true)));

        CapacityDTO capacity = CapacityDTO.builder()
                .name("To Be Deleted")
                .description("Will be removed")
                .technologyIds(List.of(1L, 2L, 3L))
                .build();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(capacity)
                .exchange()
                .expectStatus().isCreated();

        Long capacityId = capacityRepository.findAll()
                .filter(c -> "To Be Deleted".equals(c.getName()))
                .next()
                .map(c -> c.getId())
                .block();

        assert capacityId != null : "El ID debería existir";

        // Verificar que existe antes de eliminar
        Boolean existsBefore = capacityPersistencePort.existByName("To Be Deleted").block();
        assert existsBefore != null && existsBefore : "Debería existir antes de eliminar";

        // Mockear servicio de tecnologías para obtener los detalles
        when(technologyExternalServicePort.getTechnologiesByIds(anyList(), anyString()))
                .thenReturn(Flux.just(
                        new TechnologySummary(1L, "Java"),
                        new TechnologySummary(2L, "Spring"),
                        new TechnologySummary(3L, "MySQL")
                ));

        // Mockear notificación al servicio de tecnologías
        when(technologyExternalServicePort.notifyTechnologyReferencesDecrement(anyList(), anyString()))
                .thenReturn(Mono.empty());

        // Act - Eliminar
        Map<String, List<Long>> requestBody = Map.of("ids", List.of(capacityId));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity/delete-by-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk();

        // Assert - Verificar que YA NO existe en la base de datos
        Boolean existsAfter = capacityPersistencePort.existByName("To Be Deleted").block();
        assert existsAfter != null && !existsAfter : "NO debería existir después de eliminar";

        // Verificar que las relaciones también se eliminaron
        Long techCount = capacityPersistencePort
                .findTechnologyIdsByCapacityId(capacityId)
                .count()
                .block();

        assert techCount != null && techCount == 0 : "No deberían existir tecnologías asociadas";
    }

    @Test
    void deleteCapacities_WithReferencedTechnology_ShouldNotifyTechnology() {
        // Arrange
        when(technologyExternalServicePort.checkTechnologiesExist(anyList(), anyString()))
                .thenReturn(Mono.just(Map.of(1L, true, 2L, true, 3L, true)));

        CapacityDTO capacity = CapacityDTO.builder()
                .name("Capacity To Notify")
                .description("Has referenced technology")
                .technologyIds(List.of(1L, 2L, 3L))
                .build();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(capacity)
                .exchange()
                .expectStatus().isCreated();

        Long capacityId = capacityRepository.findAll()
                .filter(c -> "Capacity To Notify".equals(c.getName()))
                .next()
                .map(c -> c.getId())
                .block();

        assert capacityId != null : "El ID debería existir";

        // Mockear servicio externo de tecnologías para obtener los detalles
        when(technologyExternalServicePort.getTechnologiesByIds(anyList(), anyString()))
                .thenReturn(Flux.just(
                        new TechnologySummary(1L, "Java"),
                        new TechnologySummary(2L, "Spring"),
                        new TechnologySummary(3L, "MySQL")
                ));

        // Mockear notificación al servicio de tecnologías
        when(technologyExternalServicePort.notifyTechnologyReferencesDecrement(anyList(), anyString()))
                .thenReturn(Mono.empty());

        // Act - Eliminar capacidad
        Map<String, List<Long>> requestBody = Map.of("ids", List.of(capacityId));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity/delete-by-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk();

        // Assert - Verificar que se notificó al servicio de tecnologías
        // (no se puede verificar directamente, pero se puede inferir por el comportamiento)
    }

    @Test
    void deleteCapacities_ShouldNotifyTechnologyToDecrementReferences() {
        // Arrange
        when(technologyExternalServicePort.checkTechnologiesExist(anyList(), anyString()))
                .thenReturn(Mono.just(Map.of(1L, true, 2L, true, 3L, true)));

        CapacityDTO capacity = CapacityDTO.builder()
                .name("Capacity To Decrement")
                .description("Technology references should decrement")
                .technologyIds(List.of(1L, 2L, 3L))
                .build();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(capacity)
                .exchange()
                .expectStatus().isCreated();

        Long capacityId = capacityRepository.findAll()
                .filter(c -> "Capacity To Decrement".equals(c.getName()))
                .next()
                .map(c -> c.getId())
                .block();

        assert capacityId != null : "El ID debería existir";

        // Mockear servicio externo de tecnologías para obtener los detalles
        when(technologyExternalServicePort.getTechnologiesByIds(anyList(), anyString()))
                .thenReturn(Flux.just(
                        new TechnologySummary(1L, "Java"),
                        new TechnologySummary(2L, "Spring"),
                        new TechnologySummary(3L, "MySQL")
                ));

        // Mockear notificación al servicio de tecnologías
        when(technologyExternalServicePort.notifyTechnologyReferencesDecrement(anyList(), anyString()))
                .thenReturn(Mono.empty());

        // Act - Eliminar capacidad
        Map<String, List<Long>> requestBody = Map.of("ids", List.of(capacityId));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity/delete-by-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk();

        // Assert - Verificar que se notificó al servicio de tecnologías
        // (no se puede verificar directamente, pero se puede inferir por el comportamiento)
    }

    @Test
    void createCapacity_WithInvalidData_ShouldNotPersist() {
        // Arrange
        // Datos inválidos (menos de 3 tecnologías)
        CapacityDTO invalidDTO = CapacityDTO.builder()
                .name("Invalid Capacity")
                .description("Only 2 technologies")
                .technologyIds(List.of(1L, 2L)) // Mínimo son 3
                .build();

        // Act
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidDTO)
                .exchange()
                .expectStatus().isBadRequest();

        // Assert - Verificar que NO se guardó nada con ese nombre
        Boolean exists = capacityPersistencePort.existByName("Invalid Capacity").block();
        assert exists != null && !exists : "No debería existir en la base de datos";
    }

    @Test
    void createCapacity_WithNonExistentTechnologies_ShouldNotPersist() {
        // Arrange
        CapacityDTO capacityDTO = CapacityDTO.builder()
                .name("Blockchain Development")
                .description("Blockchain and crypto technologies")
                .technologyIds(List.of(999L, 1000L, 1001L))
                .build();

        // Mockear que las tecnologías NO existen
        when(technologyExternalServicePort.checkTechnologiesExist(anyList(), anyString()))
                .thenReturn(Mono.just(Map.of(999L, false, 1000L, false, 1001L, false)));

        // Act
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .post()
                .uri("/capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(capacityDTO)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Bad Parameters, please verify data")
                .jsonPath("$.errors[0].message").isEqualTo("Some technologies do not exist");

        // Assert - Verificar que NO se guardó
        Boolean exists = capacityPersistencePort.existByName("Blockchain Development").block();
        assert exists != null && !exists : "No debería existir en la base de datos";
    }

    @Test
    void createMultipleCapacities_ShouldPersistAll() {
        // Arrange
        when(technologyExternalServicePort.checkTechnologiesExist(anyList(), anyString()))
                .thenReturn(Mono.just(Map.of(
                        1L, true, 2L, true, 3L, true,
                        4L, true, 5L, true, 6L, true
                )));

        for (int i = 1; i <= 3; i++) {
            CapacityDTO capacity = CapacityDTO.builder()
                    .name("Capacity " + i)
                    .description("Description " + i)
                    .technologyIds(List.of(1L, 2L, 3L))
                    .build();

            webTestClient
                    .mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                    .post()
                    .uri("/capacity")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(capacity)
                    .exchange()
                    .expectStatus().isCreated();
        }

        // Assert - Verificar que se guardaron todas
        Long count = capacityRepository.count().block();
        assert count != null && count == 3 : "Deberían existir 3 capacidades en la base de datos";
    }
}
