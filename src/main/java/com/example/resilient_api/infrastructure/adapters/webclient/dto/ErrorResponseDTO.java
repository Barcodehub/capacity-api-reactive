package com.example.resilient_api.infrastructure.adapters.webclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorResponseDTO {
    private String code;
    private String message;
    private String identifier;
    private String date;
    private List<ErrorDetailDTO> errors;
}
