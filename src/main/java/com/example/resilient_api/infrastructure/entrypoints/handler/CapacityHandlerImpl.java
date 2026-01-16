package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.api.CapacityServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.infrastructure.entrypoints.dto.CapacityDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.CapacityIdsRequest;
import com.example.resilient_api.infrastructure.entrypoints.dto.CapacityWithTechnologiesDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.PageResponse;
import com.example.resilient_api.infrastructure.entrypoints.dto.TechnologySummaryDTO;
import com.example.resilient_api.domain.model.PaginationRequest;
import com.example.resilient_api.infrastructure.entrypoints.mapper.CapacityMapper;
import com.example.resilient_api.infrastructure.entrypoints.util.APIResponse;
import com.example.resilient_api.infrastructure.entrypoints.util.ErrorDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.Instant;
import java.util.List;

import static com.example.resilient_api.infrastructure.entrypoints.util.Constants.X_MESSAGE_ID;
import static com.example.resilient_api.infrastructure.entrypoints.util.Constants.TECHNOLOGY_ERROR;

@Component
@RequiredArgsConstructor
@Slf4j
public class CapacityHandlerImpl {

    private final CapacityServicePort capacityServicePort;
    private final CapacityMapper capacityMapper;

    public Mono<ServerResponse> createCapacity(ServerRequest request) {
        String messageId = getMessageId(request);
        return request.bodyToMono(CapacityDTO.class)
                .flatMap(capacity -> capacityServicePort.registerCapacity(
                        capacityMapper.capacityDTOToCapacity(capacity), messageId)
                        .doOnSuccess(savedCapacity -> log.info("Capacity created successfully with messageId: {}", messageId))
                )
                .flatMap(savedCapacity -> ServerResponse.status(HttpStatus.CREATED)
                        .bodyValue(TechnicalMessage.TECHNOLOGY_CREATED.getMessage()))
                .contextWrite(Context.of(X_MESSAGE_ID, messageId))
                .doOnError(ex -> log.error(TECHNOLOGY_ERROR, ex))
                .onErrorResume(BusinessException.class, ex -> handleBusinessException(ex, messageId))
                .onErrorResume(TechnicalException.class, ex -> handleTechnicalException(ex, messageId))
                .onErrorResume(ex -> handleUnexpectedException(ex, messageId));
    }

    public Mono<ServerResponse> checkCapacitiesExist(ServerRequest request) {
        String messageId = getMessageId(request);
        return request.bodyToMono(CapacityIdsRequest.class)
                .flatMap(idsRequest -> {
                    List<Long> ids = idsRequest.getIds() != null ? idsRequest.getIds() : List.of();
                    return capacityServicePort.checkCapacitiesExist(ids, messageId)
                            .doOnSuccess(result -> log.info("Capacities existence checked successfully with messageId: {}", messageId));
                })
                .flatMap(result -> ServerResponse.status(HttpStatus.OK).bodyValue(result))
                .contextWrite(Context.of(X_MESSAGE_ID, messageId))
                .doOnError(ex -> log.error("Error checking technologies existence for messageId: {}", messageId, ex))
                .onErrorResume(TechnicalException.class, ex -> handleTechnicalException(ex, messageId))
                .onErrorResume(ex -> handleUnexpectedException(ex, messageId));
    }

    public Mono<ServerResponse> listCapacities(ServerRequest request) {
        String messageId = getMessageId(request);

        // Extraer parámetros de query
        int page = request.queryParam("page")
                .map(Integer::parseInt)
                .orElse(PaginationRequest.DEFAULT_PAGE);
        int size = request.queryParam("size")
                .map(Integer::parseInt)
                .orElse(PaginationRequest.DEFAULT_SIZE);
        PaginationRequest.SortField sortBy = request.queryParam("sortBy")
                .map(String::toUpperCase)
                .map(PaginationRequest.SortField::valueOf)
                .orElse(PaginationRequest.SortField.NAME);
        PaginationRequest.SortDirection sortDirection = request.queryParam("sortDirection")
                .map(String::toUpperCase)
                .map(PaginationRequest.SortDirection::valueOf)
                .orElse(PaginationRequest.SortDirection.ASC);

        PaginationRequest paginationRequest = new PaginationRequest(page, size, sortBy, sortDirection);

        return capacityServicePort.listCapacities(paginationRequest, messageId)
                .map(pageResult -> {
                    // Mapear de dominio a DTO
                    List<CapacityWithTechnologiesDTO> content = pageResult.content().stream()
                            .map(capacity -> CapacityWithTechnologiesDTO.builder()
                                    .id(capacity.id())
                                    .name(capacity.name())
                                    .description(capacity.description())
                                    .technologies(capacity.technologies().stream()
                                            .map(tech -> TechnologySummaryDTO.builder()
                                                    .id(tech.id())
                                                    .name(tech.name())
                                                    .build())
                                            .toList())
                                    .build())
                            .toList();

                    return PageResponse.<CapacityWithTechnologiesDTO>builder()
                            .content(content)
                            .page(pageResult.page())
                            .size(pageResult.size())
                            .totalElements(pageResult.totalElements())
                            .totalPages(pageResult.totalPages())
                            .first(pageResult.first())
                            .last(pageResult.last())
                            .build();
                })
                .flatMap(pageResponse -> ServerResponse.ok().bodyValue(pageResponse))
                .contextWrite(Context.of(X_MESSAGE_ID, messageId))
                .doOnSuccess(response -> log.info("Capacities listed successfully with messageId: {}", messageId))
                .doOnError(ex -> log.error("Error listing capacities for messageId: {}", messageId, ex))
                .onErrorResume(TechnicalException.class, ex -> handleTechnicalException(ex, messageId))
                .onErrorResume(ex -> handleUnexpectedException(ex, messageId));
    }

    private Mono<ServerResponse> handleBusinessException(BusinessException ex, String messageId) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                messageId,
                TechnicalMessage.INVALID_PARAMETERS,
                List.of(buildErrorDTO(ex.getTechnicalMessage())));
    }

    private Mono<ServerResponse> handleTechnicalException(TechnicalException ex, String messageId) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                messageId,
                TechnicalMessage.INTERNAL_ERROR,
                List.of(buildErrorDTO(ex.getTechnicalMessage())));
    }

    private Mono<ServerResponse> handleUnexpectedException(Throwable ex, String messageId) {
        log.error("Unexpected error occurred for messageId: {}", messageId, ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                messageId,
                TechnicalMessage.INTERNAL_ERROR,
                List.of(ErrorDTO.builder()
                        .code(TechnicalMessage.INTERNAL_ERROR.getCode())
                        .message(TechnicalMessage.INTERNAL_ERROR.getMessage())
                        .build()));
    }

    private ErrorDTO buildErrorDTO(TechnicalMessage technicalMessage) {
        return ErrorDTO.builder()
                .code(technicalMessage.getCode())
                .message(technicalMessage.getMessage())
                .param(technicalMessage.getParam())
                .build();
    }

    private Mono<ServerResponse> buildErrorResponse(HttpStatus httpStatus, String identifier, TechnicalMessage error,
                                                    List<ErrorDTO> errors) {
        return Mono.defer(() -> {
            APIResponse apiErrorResponse = APIResponse
                    .builder()
                    .code(error.getCode())
                    .message(error.getMessage())
                    .identifier(identifier)
                    .date(Instant.now().toString())
                    .errors(errors)
                    .build();
            return ServerResponse.status(httpStatus)
                    .bodyValue(apiErrorResponse);
        });
    }

    private String getMessageId(ServerRequest serverRequest) {
        return serverRequest.headers().firstHeader(X_MESSAGE_ID);
    }
}
