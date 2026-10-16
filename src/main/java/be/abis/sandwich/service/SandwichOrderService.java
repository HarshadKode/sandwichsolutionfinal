package be.abis.sandwich.service;

import be.abis.sandwich.model.SandwichOrder;
import be.abis.sandwich.model.SandwichOrderDetail;

public interface SandwichOrderService {

    /**
     * Adds a sandwich to the order of the course on that day (the order is created with the first sandwich).
     * A person can order at most 2 sandwiches per order. Returns the saved detail.
     */
    SandwichOrderDetail addSandwichOrderDetail(SandwichOrderDetail sod);

    /** Writes the order to a readable .txt file and returns the text that was written. */
    String printSandwichOrder(SandwichOrder so);

    /** Calculates the total price of the order and saves it in the database. */
    double calculateSandwichOrderPrice(SandwichOrder so);
}
