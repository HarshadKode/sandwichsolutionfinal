package be.abis.sandwich.service;

import be.abis.sandwich.model.SandwichOrderDetail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** How the price of an order is calculated: base price of the sandwich times the amount. */
final class OrderPrices {

    private OrderPrices() {
    }

    static BigDecimal price(SandwichOrderDetail detail) {
        return BigDecimal.valueOf(detail.getSandwich().getBasePrice()).multiply(BigDecimal.valueOf(detail.getAmount()));
    }

    /** Sum of the prices of all details, rounded to 2 decimals (BigDecimal, so there are no floating point errors). */
    static double total(List<SandwichOrderDetail> details) {
        return details.stream()
                .map(OrderPrices::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
