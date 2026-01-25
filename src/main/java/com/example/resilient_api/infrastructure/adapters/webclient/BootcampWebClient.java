package com.example.resilient_api.infrastructure.adapters.webclient;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.infrastructure.adapters.webclient.dto.BootcampDTO;
import com.example.resilient_api.infrastructure.adapters.webclient.dto.BootcampWithCapacitiesDTO;
import com.example.resilient_api.infrastructure.adapters.webclient.dto.ErrorDetailDTO;
import com.example.resilient_api.infrastructure.adapters.webclient.dto.ErrorResponseDTO;
import com.example.resilient_api.infrastructure.adapters.webclient.dto.PageResponse;
import com.example.resilient_api.infrastructure.entrypoints.dto.EnrollmentRequestDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.EnrollmentResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static com.example.resilient_api.domain.enums.TechnicalMessage.BOOTCAMP_SERVICE_ERROR;
import static com.example.resilient_api.infrastructure.entrypoints.util.Constants.X_MESSAGE_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootcampWebClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${external.bootcamp.base-url}")
    private String bootcampBaseUrl;

    public Mono<BootcampDTO> createBootcamp(BootcampDTO bootcampDTO, String messageId) {
        log.info("Calling bootcamp service to create bootcamp with messageId: {}", messageId);

        return webClientBuilder.build()
                .post()
                .uri(bootcampBaseUrl + "/bootcamp")
                .header(X_MESSAGE_ID, messageId)
                .bodyValue(bootcampDTO)
                .retrieve()
                .bodyToMono(BootcampDTO.class)
                .doOnSuccess(result -> log.info("Successfully created bootcamp with messageId: {}", messageId))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Bootcamp service returned error {} for messageId: {}", ex.getStatusCode(), messageId);

                    // Intentar parsear la respuesta de error
                    try {
                        ErrorResponseDTO errorResponse = ex.getResponseBodyAs(ErrorResponseDTO.class);
                        if (errorResponse != null && errorResponse.getErrors() != null && !errorResponse.getErrors().isEmpty()) {
                            ErrorDetailDTO firstError = errorResponse.getErrors().get(0);
                            String errorMessage = firstError.getMessage() != null ? firstError.getMessage() : errorResponse.getMessage();
                            log.error("Bootcamp service error detail: {}", errorMessage);

                            // Si es un error 4xx, es un error de negocio
                            if (ex.getStatusCode().is4xxClientError()) {
                                // Crear un TechnicalMessage dinámico con el mensaje del error
                                return Mono.error(new BusinessException(
                                    createDynamicTechnicalMessage(errorResponse.getCode(), errorMessage, firstError.getParam())
                                ));
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Could not parse error response from bootcamp service: {}", e.getMessage());
                    }

                    // Si no se pudo parsear o es un error 5xx, retornar error técnico genérico
                    return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                })
                .onErrorResume(ex -> {
                    if (ex instanceof BusinessException || ex instanceof TechnicalException) {
                        return Mono.error(ex);
                    }
                    log.error("Unexpected error calling bootcamp service for messageId: {}", messageId, ex);
                    return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                });
    }

    private TechnicalMessage createDynamicTechnicalMessage(String code, String message, String param) {
        // Mapear mensajes comunes del microservicio de bootcamp
        if (message != null) {
            if (message.contains("already exists")) {
                return TechnicalMessage.BOOTCAMP_ALREADY_EXISTS;
            }
            if (message.contains("name") && message.contains("required")) {
                return TechnicalMessage.TECHNOLOGY_NAME_REQUIRED;
            }
            if (message.contains("description") && message.contains("required")) {
                return TechnicalMessage.TECHNOLOGY_DESCRIPTION_REQUIRED;
            }
        }
        // Por defecto, retornar error de parámetros inválidos
        return TechnicalMessage.INVALID_PARAMETERS;
    }

    public Mono<PageResponse<BootcampWithCapacitiesDTO>> listBootcamps(int page, int size, String sortBy,
                                                                         String sortDirection, String messageId) {
        log.info("Calling bootcamp service to list bootcamps with messageId: {}", messageId);

        String uri = String.format("%s/bootcamp?page=%d&size=%d&sortBy=%s&sortDirection=%s",
                bootcampBaseUrl, page, size, sortBy, sortDirection);

        return webClientBuilder.build()
                .get()
                .uri(uri)
                .header(X_MESSAGE_ID, messageId)
                .retrieve()
                .onStatus(status -> status.is5xxServerError(),
                    response -> {
                        log.error("Bootcamp service returned 5xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                    })
                .onStatus(status -> status.is4xxClientError(),
                    response -> {
                        log.error("Bootcamp service returned 4xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                    })
                .bodyToMono(new ParameterizedTypeReference<PageResponse<BootcampWithCapacitiesDTO>>() {})
                .doOnSuccess(result -> log.info("Successfully listed bootcamps with messageId: {}", messageId))
                .doOnError(ex -> log.error("Error calling bootcamp service for messageId: {}", messageId, ex))
                .onErrorResume(ex -> {
                    if (ex instanceof TechnicalException) {
                        return Mono.error(ex);
                    }
                    log.error("Unexpected error calling bootcamp service for messageId: {}", messageId, ex);
                    return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                });
    }

    public Mono<String> deleteBootcamp(Long id, String messageId) {
        log.info("Calling bootcamp service to delete bootcamp {} with messageId: {}", id, messageId);

        return webClientBuilder.build()
                .delete()
                .uri(bootcampBaseUrl + "/bootcamp/" + id)
                .header(X_MESSAGE_ID, messageId)
                .retrieve()
                .onStatus(status -> status.is5xxServerError(),
                    response -> {
                        log.error("Bootcamp service returned 5xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                    })
                .onStatus(status -> status.is4xxClientError(),
                    response -> {
                        log.error("Bootcamp service returned 4xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                    })
                .bodyToMono(String.class)
                .doOnSuccess(result -> log.info("Successfully deleted bootcamp with messageId: {}", messageId))
                .doOnError(ex -> log.error("Error calling bootcamp service for messageId: {}", messageId, ex))
                .onErrorResume(ex -> {
                    if (ex instanceof TechnicalException) {
                        return Mono.error(ex);
                    }
                    log.error("Unexpected error calling bootcamp service for messageId: {}", messageId, ex);
                    return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                });
    }

    public Mono<EnrollmentResponseDTO> enrollInBootcamp(Long bootcampId, Long userId, String messageId, String authToken) {
        log.info("Calling bootcamp service to enroll user {} in bootcamp {} with messageId: {}", userId, bootcampId, messageId);

        EnrollmentRequestDTO requestDTO = EnrollmentRequestDTO.builder()
                .bootcampId(bootcampId)
                .build();

        return webClientBuilder.build()
                .post()
                .uri(bootcampBaseUrl + "/bootcamp/enroll")
                .header(X_MESSAGE_ID, messageId)
                .header("Authorization", authToken)
                .header("X-User-Id", String.valueOf(userId))
                .bodyValue(requestDTO)
                .retrieve()
                .onStatus(status -> status.is5xxServerError(),
                    response -> {
                        log.error("Bootcamp service returned 5xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                    })
                .onStatus(status -> status.is4xxClientError(),
                    response -> {
                        log.error("Bootcamp service returned 4xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                    })
                .bodyToMono(EnrollmentResponseDTO.class)
                .doOnSuccess(result -> log.info("Successfully enrolled in bootcamp with messageId: {}", messageId))
                .doOnError(ex -> log.error("Error calling bootcamp service for messageId: {}", messageId, ex))
                .onErrorResume(ex -> {
                    if (ex instanceof TechnicalException) {
                        return Mono.error(ex);
                    }
                    log.error("Unexpected error calling bootcamp service for messageId: {}", messageId, ex);
                    return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                });
    }

    public Mono<String> unenrollFromBootcamp(Long bootcampId, Long userId, String messageId, String authToken) {
        log.info("Calling bootcamp service to unenroll user {} from bootcamp {} with messageId: {}", userId, bootcampId, messageId);

        return webClientBuilder.build()
                .delete()
                .uri(bootcampBaseUrl + "/bootcamp/" + bootcampId + "/user/" + userId)
                .header(X_MESSAGE_ID, messageId)
                .header("Authorization", authToken)
                .retrieve()
                .onStatus(status -> status.is5xxServerError(),
                    response -> {
                        log.error("Bootcamp service returned 5xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                    })
                .onStatus(status -> status.is4xxClientError(),
                    response -> {
                        log.error("Bootcamp service returned 4xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                    })
                .bodyToMono(String.class)
                .doOnSuccess(result -> log.info("Successfully unenrolled from bootcamp with messageId: {}", messageId))
                .doOnError(ex -> log.error("Error calling bootcamp service for messageId: {}", messageId, ex))
                .onErrorResume(ex -> {
                    if (ex instanceof TechnicalException) {
                        return Mono.error(ex);
                    }
                    log.error("Unexpected error calling bootcamp service for messageId: {}", messageId, ex);
                    return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                });
    }

    public Mono<BootcampDTO[]> getUserBootcamps(Long userId, String messageId, String authToken) {
        log.info("Calling bootcamp service to get bootcamps for user {} with messageId: {}", userId, messageId);

        return webClientBuilder.build()
                .get()
                .uri(bootcampBaseUrl + "/bootcamp/user/" + userId)
                .header(X_MESSAGE_ID, messageId)
                .header("Authorization", authToken)
                .retrieve()
                .onStatus(status -> status.is5xxServerError(),
                    response -> {
                        log.error("Bootcamp service returned 5xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                    })
                .onStatus(status -> status.is4xxClientError(),
                    response -> {
                        log.error("Bootcamp service returned 4xx error for messageId: {}", messageId);
                        return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                    })
                .bodyToMono(BootcampDTO[].class)
                .doOnSuccess(result -> log.info("Successfully retrieved user bootcamps with messageId: {}", messageId))
                .doOnError(ex -> log.error("Error calling bootcamp service for messageId: {}", messageId, ex))
                .onErrorResume(ex -> {
                    if (ex instanceof TechnicalException) {
                        return Mono.error(ex);
                    }
                    log.error("Unexpected error calling bootcamp service for messageId: {}", messageId, ex);
                    return Mono.error(new TechnicalException(BOOTCAMP_SERVICE_ERROR));
                });
    }
}

