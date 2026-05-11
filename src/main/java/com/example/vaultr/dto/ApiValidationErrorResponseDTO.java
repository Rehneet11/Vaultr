package com.example.vaultr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(
        name = "ApiValidationErrorResponse",
        description = "Problem Details validation error response returned by the API."
)
public class ApiValidationErrorResponseDTO extends ApiErrorResponseDTO {

    @Schema(description = "Validation errors keyed by request field name.", example = "{\"email\":\"must be a well-formed email address\"}")
    public Map<String, String> fieldErrors;
}
