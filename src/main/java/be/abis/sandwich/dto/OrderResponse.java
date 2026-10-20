package be.abis.sandwich.dto;

import be.abis.sandwich.model.SandwichOrder;

import java.time.LocalDate;

public record OrderResponse(int id, LocalDate orderDate, CourseResponse course, double totalPrice) {

    public static OrderResponse from(SandwichOrder order) {
        return new OrderResponse(order.getId(), order.getOrderDate(), CourseResponse.from(order.getCourse()), order.getTotalPrice());
    }
}
