package com.example.vaultr.controllers;

import com.example.vaultr.dto.CreateUserRequestDTO;
import com.example.vaultr.dto.CreateUserResponseDTO;
import com.example.vaultr.dto.ApiErrorResponseDTO;
import com.example.vaultr.dto.ApiValidationErrorResponseDTO;
import com.example.vaultr.dto.UserResponseDTO;
import com.example.vaultr.services.IUserService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RateLimiter(name = "userApi")
@Tag(name = "Users", description = "User Management and Onboarding API")
public class UserController {
    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create-user")
    @Operation(
            summary = "Create a New User",
            description = "Registers a new user in the system. Triggers the automatic provisioning of their associated wallet."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User Created Successfully"),

            @ApiResponse(responseCode = "400", description = "Validation Error",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiValidationErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Resource Already Exists",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<CreateUserResponseDTO> createUser(
            @Parameter(description = "User registration payload", required = true)
            @Valid @RequestBody CreateUserRequestDTO userRequestDTO) throws Exception {

        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userRequestDTO));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get User by ID",
            description = "Retrieves the profile details of a user using their globally unique system ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User Found"),
            @ApiResponse(responseCode = "404", description = "Resource Not Found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<UserResponseDTO> getUserById(
            @Parameter(description = "The unique user ID", required = true)
            @PathVariable String id) throws Exception{

        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/email/{email}")
    @Operation(
            summary = "Get User by Email",
            description = "Searches for a user profile using their registered email address."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User Found"),

            @ApiResponse(responseCode = "404", description = "Resource Not Found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<UserResponseDTO> getUserByEmail(
            @Parameter(description = "The email address of the user", required = true)
            @PathVariable String email) throws Exception {

        return ResponseEntity.ok(userService.getUserByEmail(email));
    }
}
