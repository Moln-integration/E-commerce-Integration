package se.moln.ecommerceintegration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import se.moln.ecommerceintegration.dto.ErrorResponse;
import se.moln.ecommerceintegration.model.ActivityLog;
import se.moln.ecommerceintegration.repository.ActivityLogRepository;

import java.util.List;

@RestController
public class ActivityLogController {

    private final ActivityLogRepository repo;

    public ActivityLogController(ActivityLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/users/history")
    @Operation(
            summary = "Get last 50 activity logs for the authenticated user",
            description = "Returns the most recent 50 activity log entries for the current user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ActivityLog[].class),
                    examples = @ExampleObject(
                            name = "ResponseExample",
                            value = """
                                    [
                                      {
                                        "id": "a5f1d2e3-4b6c-7890-ab12-34567890cdef",
                                        "action": "AUTH_LOGIN",
                                        "userEmail": "user@example.com",
                                        "method": "POST",
                                        "path": "/auth/login",
                                        "status": 200,
                                        "ip": "192.0.2.10",
                                        "userAgent": "Mozilla/5.0",
                                        "durationMs": 23,
                                        "createdAt": "2025-09-13T10:12:34Z"
                                      },
                                      {
                                        "id": "b7f1d2e3-4b6c-7890-ab12-34567890cdef",
                                        "action": "ORDER_CREATE",
                                        "userEmail": "user@example.com",
                                        "method": "POST",
                                        "path": "/orders",
                                        "status": 201,
                                        "ip": "192.0.2.10",
                                        "userAgent": "Mozilla/5.0",
                                        "durationMs": 41,
                                        "createdAt": "2025-09-13T10:15:02Z"
                                      }
                                    ]
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            name = "UnauthorizedExample",
                            value = """
                                    {
                                      "error": "unauthorized",
                                      "message": "Full authentication is required to access this resource",
                                      "details": []
                                    }
                                    """
                    )
            )
    )
    public List<ActivityLog> myHistory(@AuthenticationPrincipal Object principal) {
        String email = extractEmail(principal);
        return repo.findTop50ByUserEmailOrderByCreatedAtDesc(email);
    }

    private String extractEmail(Object principal) {
        if (principal instanceof UserDetails ud) return ud.getUsername();
        if (principal instanceof String s) return s;
        throw new IllegalStateException("Unsupported principal type: " + principal);
    }
}