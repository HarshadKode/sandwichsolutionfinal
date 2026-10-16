package be.abis.sandwich.repository;

import be.abis.sandwich.enumeration.BreadType;
import be.abis.sandwich.model.Person;
import be.abis.sandwich.model.Sandwich;
import be.abis.sandwich.model.SandwichOrder;
import be.abis.sandwich.model.SandwichOrderDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DBSandwichOrderDetailRepository implements SandwichOrderDetailRepository {

    private final JdbcClient jdbc;

    public DBSandwichOrderDetailRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public SandwichOrderDetail addSandwichOrderDetail(SandwichOrderDetail detail) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO sandwich_order_details " +
                        "(order_id, sandwich_id, person_id, amount, bread_type, vegetables, order_comment) " +
                        "VALUES (:orderId, :sandwichId, :personId, :amount, :breadType, :vegetables, :comment)")
                .param("orderId", detail.getSandwichOrder().getId())
                .param("sandwichId", detail.getSandwich().getId())
                .param("personId", detail.getPerson().getId())
                .param("amount", detail.getAmount())
                .param("breadType", detail.getBreadType().name())
                .param("vegetables", detail.isVegetables() ? 1 : 0)
                .param("comment", detail.getComment())
                .update(keys, "ID");
        detail.setId(keys.getKey().intValue());
        return detail;
    }

    @Override
    public List<SandwichOrderDetail> findDetailsByOrder(SandwichOrder sandwichOrder) {
        return jdbc.sql("SELECT d.id, d.amount, d.bread_type, d.vegetables, d.order_comment, " +
                        "s.id AS sandwich_id, s.name, s.description, s.category, s.base_price, " +
                        "p.id AS person_id, p.first_name, p.last_name " +
                        "FROM sandwich_order_details d " +
                        "JOIN sandwiches s ON s.id = d.sandwich_id " +
                        "JOIN abis_persons p ON p.id = d.person_id " +
                        "WHERE d.order_id = :orderId ORDER BY p.last_name, p.first_name, d.id")
                .param("orderId", sandwichOrder.getId())
                .query((rs, rowNum) -> {
                    Sandwich sandwich = new Sandwich(rs.getInt("sandwich_id"), rs.getString("name"),
                            rs.getString("description"), rs.getString("category"), rs.getDouble("base_price"));
                    Person person = new Person(rs.getInt("person_id"), rs.getString("first_name"), rs.getString("last_name"));
                    SandwichOrderDetail detail = new SandwichOrderDetail(sandwich, rs.getInt("amount"),
                            BreadType.valueOf(rs.getString("bread_type")), rs.getInt("vegetables") == 1,
                            rs.getString("order_comment"), person);
                    detail.setId(rs.getInt("id"));
                    detail.setSandwichOrder(sandwichOrder);
                    return detail;
                })
                .list();
    }

    @Override
    public int countSandwichesOfPerson(int orderId, int personId) {
        return jdbc.sql("SELECT COALESCE(SUM(amount), 0) FROM sandwich_order_details " +
                        "WHERE order_id = :orderId AND person_id = :personId")
                .param("orderId", orderId)
                .param("personId", personId)
                .query(Integer.class)
                .single();
    }
}
