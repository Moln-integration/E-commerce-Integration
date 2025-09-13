package se.moln.ecommerceintegration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import se.moln.ecommerceintegration.dto.AuthResponse;
import se.moln.ecommerceintegration.dto.LoginRequest;
import se.moln.ecommerceintegration.dto.RegisterRequest;
import se.moln.ecommerceintegration.model.User;
import se.moln.ecommerceintegration.repository.UserRepository;
import se.moln.ecommerceintegration.service.JwtService;
import se.moln.ecommerceintegration.dto.ErrorResponse;
import se.moln.ecommerceintegration.service.UserService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository users;
    private final JwtService jwt;

    public AuthController(UserService userService, UserRepository users, JwtService jwt) {
        this.userService = userService;
        this.users = users;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register a new user",
            description = "Creates a user account and returns an access token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "ValidationErrorExample", value = """
                                    {
                                      "error": "validation_failed",
                                      "message": "Validation failed",
                                      "details": [
                                        {"field":"password","code":"Size","message":"Password must be 8-72 characters","rejectedValue":null},
                                        {"field":"password","code":"Pattern","message":"Password must contain upper, lower, digit and special character","rejectedValue":null}
                                      ]
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already in use",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(name = "ConflictExample", value = """
                                    { "error": "conflict", "message": "Email already in use" }
                                    """)
                    )
            )
    })
    public AuthResponse register(
            @RequestBody(
                    description = "User registration payload",
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            name = "Example",
                            value = """
                                    {
                                      "email": "user@example.com",
                                      "password": "Password123!",
                                      "firstName": "King",
                                      "lastName": "Kong"
                                    }
                                    """
                    ))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody RegisterRequest req
    ) {
        User user = userService.register(req.email(), req.password(), req.firstName(), req.lastName());
        return new AuthResponse(jwt.createAccessToken(user.getId(), user.getEmail(), user.getRole().name()));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in with email and password",
            description = "Validates credentials and returns an access token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "UnauthorizedExample", value = """
                                    { "error": "unauthorized", "message": "Invalid credentials", "details": [] }
                                    """)
                    )
            )
    })
    public AuthResponse login(
            @RequestBody(
                    description = "User credentials",
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            name = "Example",
                            value = """
                                    {
                                      "email": "user@example.com",
                                      "password": "Password123!"
                                    }
                                    """
                    ))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody LoginRequest req
    ) {
        User user = users.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        var enc = new BCryptPasswordEncoder();
        if (!enc.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new AuthResponse(jwt.createAccessToken(user.getId(), user.getEmail(), user.getRole().name()));
    }
}