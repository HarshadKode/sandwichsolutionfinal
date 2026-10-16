package be.abis.sandwich.dto;

import be.abis.sandwich.model.Sandwich;

/** A sandwich as the API shows it. */
public record SandwichResponse(int id, String name, String description, String category, double basePrice) {

    public static SandwichResponse from(Sandwich sandwich) {
        return new SandwichResponse(sandwich.getId(), sandwich.getName(), sandwich.getDescription(),
                sandwich.getCategory(), sandwich.getBasePrice());
    }
}
