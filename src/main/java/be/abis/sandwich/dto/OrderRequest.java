package be.abis.sandwich.dto;

import be.abis.sandwich.model.SandwichOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Identifies the order of a course on a day. Body of {@code /api/orders/print} and {@code /api/orders/price}. */
public record OrderRequest(
        @NotNull(message = "orderDate is required (yyyy-MM-dd)") LocalDate orderDate,
        @NotNull(message = "course is required") @Valid CourseRequest course) {

    public SandwichOrder toEntity() {
        return new SandwichOrder(orderDate, course.toEntity());
    }
}
