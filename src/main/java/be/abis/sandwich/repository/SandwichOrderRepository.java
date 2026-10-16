package be.abis.sandwich.repository;

import be.abis.sandwich.model.SandwichOrder;

import java.time.LocalDate;
import java.util.Optional;

public interface SandwichOrderRepository {

    /** There is one order per course per day. */
    Optional<SandwichOrder> findSandwichOrder(LocalDate orderDate, int courseId);
    /** Inserts the order (its course must already exist) and returns it with its generated id. */
    SandwichOrder addSandwichOrder(SandwichOrder sandwichOrder);
    void updateTotalPrice(int orderId, double totalPrice);

}
