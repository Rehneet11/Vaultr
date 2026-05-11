package com.example.vaultr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ApiErrorResponse",
        description = "Problem Details error response returned by the API."
)
public class ApiErrorResponseDTO {

    @Schema(description = "Problem type URI.", example = "about:blank")
    public String type;

    @Schema(description = "Short, human-readable error title.", example = "Resource Not Found")
    public String title;

    @Schema(description = "HTTP status code.", example = "404")
    public int status;

    @Schema(description = "Human-readable explanation of this specific error.", example = "Wallet not found with ID: usr_123")
    public String detail;

    @Schema(description = "Request path where the error occurred.", example = "/api/wallets/usr_123")
    public String instance;
}
