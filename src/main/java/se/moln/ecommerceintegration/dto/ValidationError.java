package se.moln.ecommerceintegration.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Field-level validation error")
public record ValidationError(
        @Schema(example = "password") String field,
        @Schema(example = "Pattern") String code,
        @Schema(example = "Password must contain upper, lower, digit and special character")
        String message,
        @Schema(nullable = true, example = "null") Object rejectedValue
) {
}