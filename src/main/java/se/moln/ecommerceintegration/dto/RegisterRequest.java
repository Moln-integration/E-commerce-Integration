package se.moln.ecommerceintegration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @Size(min = 8) String password,
        @NotBlank String firstName,
        @NotBlank String lastName
) {}