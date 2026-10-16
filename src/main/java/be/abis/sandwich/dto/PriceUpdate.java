package be.abis.sandwich.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Body of {@code PUT /api/sandwiches/{id}/price}. */
public record PriceUpdate(
        @NotNull(message = "basePrice is required")
        @PositiveOrZero(message = "basePrice must not be negative")
        Double basePrice) {
}
