package be.abis.sandwich.repository;

import be.abis.sandwich.model.Person;
import be.abis.sandwich.model.SandwichOrder;
import be.abis.sandwich.model.SandwichOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SandwichOrderDetailRepository extends JpaRepository<SandwichOrderDetail, Integer> {

    @Query("select d from SandwichOrderDetail d where d.sandwichOrder = :order " +
            "order by d.person.lastName, d.person.firstName, d.id")
    List<SandwichOrderDetail> findDetailsByOrder(@Param("order") SandwichOrder order);

    /** Total amount of sandwiches the person already ordered in this order. */
    @Query("select coalesce(sum(d.amount), 0) from SandwichOrderDetail d where d.sandwichOrder = :order and d.person = :person")
    int countSandwichesOfPerson(@Param("order") SandwichOrder order, @Param("person") Person person);
}
