package be.abis.sandwich.dto;

import jakarta.validation.constraints.NotNull;

/** Points to an existing sandwich. Only the id counts: name, price, ... are always taken from the database. */
public record SandwichRefRequest(@NotNull(message = "sandwich id is required") Integer id) {
}
