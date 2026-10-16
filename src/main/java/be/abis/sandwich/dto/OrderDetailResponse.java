package be.abis.sandwich.dto;

import be.abis.sandwich.enumeration.BreadType;
import be.abis.sandwich.model.SandwichOrderDetail;

public record OrderDetailResponse(int id, SandwichResponse sandwich, int amount, BreadType breadType, boolean vegetables,
                                  String comment, PersonDto person, OrderResponse sandwichOrder) {

    public static OrderDetailResponse from(SandwichOrderDetail detail) {
        return new OrderDetailResponse(detail.getId(), SandwichResponse.from(detail.getSandwich()), detail.getAmount(),
                detail.getBreadType(), detail.isVegetables(), detail.getComment(), PersonDto.from(detail.getPerson()),
                OrderResponse.from(detail.getSandwichOrder()));
    }
}
