package com.example.resilient_api.infrastructure.entrypoints.util;

import com.example.resilient_api.infrastructure.entrypoints.dto.CapacityDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class APIResponse {
    private String code;
    private String message;
    private String identifier;
    private String date;
    private CapacityDTO data;
    private List<ErrorDTO> errors;
}
