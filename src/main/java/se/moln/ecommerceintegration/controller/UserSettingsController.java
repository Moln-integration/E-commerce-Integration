package se.moln.ecommerceintegration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.moln.ecommerceintegration.dto.ErrorResponse;
import se.moln.ecommerceintegration.dto.UpdateSettingsRequest;
import se.moln.ecommerceintegration.dto.UserProfileResponse;
import se.moln.ecommerceintegration.model.User;
import se.moln.ecommerceintegration.service.UserService;

import static se.moln.ecommerceintegration.utils.PrincipalUtils.extractEmail;

@RestController
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserService userService;

    @PutMapping("/me/settings")
    @Operation(
            summary = "Update own profile settings",
            description = "Send only the fields you want to update.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "RequestExample",
                                    value = """
                                            {
                                              "firstName": "Ismete",
                                              "lastName": "Hani"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Updated settings",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserProfileResponse.class),
                            examples = @ExampleObject(
                                    name = "ResponseExample",
                                    value = """
                                            {
                                              "id": "7f3b5c5a-1234-4b5f-9c0a-1a2b3c4d5e6f",
                                              "email": "user@example.com",
                                              "firstName": "Ismete",
                                              "lastName": "Hani",
                                              "role": "CUSTOMER",
                                              "active": true
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request (validation/format)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "BadRequestExample",
                                    value = """
                                            {
                                              "error": "bad_request",
                                              "message": "firstName must not be blank",
                                              "details": []
                                            }
                                            """
                            )
                    )
            ),
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
    })
    public UserProfileResponse updateSettings(
            @AuthenticationPrincipal Object principal,
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdateSettingsRequest req
    ) {
        String email = extractEmail(principal);

        String first = req.firstName();
        if (first != null) {
            first = first.trim();
            if (first.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "firstName must not be blank");
            }
        }

        String last = req.lastName();
        if (last != null) {
            last = last.trim();
            if (last.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lastName must not be blank");
            }
        }

        User u = userService.updateSettings(email, first, last);
        return new UserProfileResponse(
                u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
                u.getRole().name(), Boolean.TRUE.equals(u.getIsActive())
        );
    }
}