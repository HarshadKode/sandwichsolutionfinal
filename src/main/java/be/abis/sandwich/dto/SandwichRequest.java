package be.abis.sandwich.dto;

import be.abis.sandwich.model.Sandwich;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/sandwiches}. There is no id: the database generates it. */
public record SandwichRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name can have at most 100 characters")
        String name,
        @Size(max = 500, message = "description can have at most 500 characters")
        String description,
        @NotBlank(message = "category is required")
        @Size(max = 50, message = "category can have at most 50 characters")
        String category,
        @NotNull(message = "basePrice is required")
        @PositiveOrZero(message = "basePrice must not be negative")
        Double basePrice) {

    public Sandwich toEntity() {
        return new Sandwich(0, name.trim(), description, category.trim(), basePrice);
    }
}
