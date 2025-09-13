package se.moln.ecommerceintegration.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Standard validation/exception error response")
public record ErrorResponse(
        @Schema(example = "validation_failed") String error,
        @Schema(example = "Validation failed") String message,
        List<ValidationError> details
) {
}