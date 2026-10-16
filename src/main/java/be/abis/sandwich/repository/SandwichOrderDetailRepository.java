package be.abis.sandwich.repository;

import be.abis.sandwich.model.SandwichOrder;
import be.abis.sandwich.model.SandwichOrderDetail;

import java.util.List;

public interface SandwichOrderDetailRepository {

    /** Inserts the detail (order, sandwich and person must already exist) and returns it with its generated id. */
    SandwichOrderDetail addSandwichOrderDetail(SandwichOrderDetail detail);
    List<SandwichOrderDetail> findDetailsByOrder(SandwichOrder sandwichOrder);
    /** Total amount of sandwiches the person already ordered in this order. */
    int countSandwichesOfPerson(int orderId, int personId);

}
