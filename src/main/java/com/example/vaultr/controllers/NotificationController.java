package com.example.vaultr.controllers;

import com.example.vaultr.dto.ApiErrorResponseDTO;
import com.example.vaultr.entities.Notification;
import com.example.vaultr.exceptions.ResourceNotFoundException;
import com.example.vaultr.repositories.NotificationRepository;
import com.example.vaultr.repositories.UserRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RateLimiter(name = "notificationApi")
@Tag(name = "Notifications", description = "Async Event Notification API")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(
            summary = "Get User Notifications",
            description = "Retrieves a paginated list of transaction notifications for the authenticated user, sorted by newest first. Triggered asynchronously via the Kafka event pipeline."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications Retrieved Successfully"),

            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or Invalid User ID Header",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource Not Found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class)
                    )
            )
    })
    public ResponseEntity<Page<Notification>> getUserNotifications(
            @RequestHeader(value = "X-User-Id", required = true)
            @Parameter(description = "Authenticated User ID injected by the upstream API Gateway", required = true)
            String userId,

            @RequestParam(defaultValue = "0")
            @Parameter(description = "Page number (zero-based index)", example = "0")
            int page,

            @RequestParam(defaultValue = "10")
            @Parameter(description = "Number of notifications per page", example = "10")
            int size) {

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(page, size)
        );

        return ResponseEntity.ok(notifications);
    }
}
