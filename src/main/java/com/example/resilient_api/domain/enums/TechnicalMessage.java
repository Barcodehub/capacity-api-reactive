package com.example.resilient_api.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TechnicalMessage {

    INTERNAL_ERROR("500","Something went wrong, please try again", ""),
    INVALID_REQUEST("400", "Bad Request, please verify data", ""),
    INVALID_PARAMETERS(INVALID_REQUEST.getCode(), "Bad Parameters, please verify data", ""),
    UNSUPPORTED_OPERATION("501", "Method not supported, please try again", ""),
    TECHNOLOGY_CREATED("201", "Capacity created successfully", ""),
    TECHNOLOGY_ALREADY_EXISTS("400", "Capacity with this name already exists", "name"),
    TECHNOLOGY_NAME_REQUIRED("400", "Capacity name is required", "name"),
    TECHNOLOGY_DESCRIPTION_REQUIRED("400", "Capacity description is required", "description"),
    TECHNOLOGY_NAME_TOO_LONG("400", "Capacity name cannot exceed 50 characters", "name"),
    TECHNOLOGY_DESCRIPTION_TOO_LONG("400", "Capacity description cannot exceed 90 characters", "description"),
    CAPACITY_TECHNOLOGIES_REQUIRED("400", "Capacity must have at least 3 technologies", "technologyIds"),
    CAPACITY_TECHNOLOGIES_MIN("400", "Capacity must have at least 3 technologies", "technologyIds"),
    CAPACITY_TECHNOLOGIES_MAX("400", "Capacity cannot have more than 20 technologies", "technologyIds"),
    CAPACITY_TECHNOLOGIES_DUPLICATED("400", "Capacity cannot have duplicate technologies", "technologyIds"),
    TECHNOLOGIES_NOT_FOUND("400", "Some technologies do not exist", "technologyIds"),
    TECHNOLOGY_SERVICE_ERROR("500", "Error communicating with technology service", "")
    ;

    private final String code;
    private final String message;
    private final String param;
}