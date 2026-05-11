package com.example.vaultr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ApiConcurrentRequestErrorResponse",
        description = "Problem Details concurrent request error response returned by the API."
)
public class ApiConcurrentRequestErrorResponseDTO extends ApiErrorResponseDTO {

    @Schema(description = "Suggested wait time before retrying a concurrent request.", example = "5")
    public Integer retryAfterSeconds;
}
