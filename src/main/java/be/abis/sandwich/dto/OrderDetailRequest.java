package be.abis.sandwich.dto;

import be.abis.sandwich.enumeration.BreadType;
import be.abis.sandwich.model.Sandwich;
import be.abis.sandwich.model.SandwichOrderDetail;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/orders/details}: one person orders a sandwich for the order of a course. */
public record OrderDetailRequest(
        @NotNull(message = "sandwich is required") @Valid SandwichRefRequest sandwich,
        @NotNull(message = "amount is required")
        @Min(value = 1, message = "amount must be at least 1")
        @Max(value = 2, message = "you can order at most 2 sandwiches")
        Integer amount,
        @NotNull(message = "breadType is required (WHITE or BROWN)") BreadType breadType,
        @NotNull(message = "vegetables is required (true or false)") Boolean vegetables,
        @Size(max = 200, message = "comment can have at most 200 characters") String comment,
        @NotNull(message = "person is required") @Valid PersonRequest person,
        @NotNull(message = "sandwichOrder is required") @Valid OrderRequest sandwichOrder) {

    /** The sandwich only has its id: the service looks up the real sandwich. */
    public SandwichOrderDetail toEntity() {
        Sandwich reference = new Sandwich();
        reference.setId(sandwich.id());
        SandwichOrderDetail detail = new SandwichOrderDetail(reference, amount, breadType, vegetables, comment, person.toEntity());
        detail.setSandwichOrder(sandwichOrder.toEntity());
        return detail;
    }
}
