package be.abis.sandwich.repository;

import be.abis.sandwich.model.Course;
import be.abis.sandwich.model.SandwichOrder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public class DBSandwichOrderRepository implements SandwichOrderRepository {

    private final JdbcClient jdbc;

    public DBSandwichOrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SandwichOrder> findSandwichOrder(LocalDate orderDate, int courseId) {
        return jdbc.sql("SELECT o.id, o.order_date, o.total_price, c.id AS course_id, c.title " +
                        "FROM sandwich_orders o JOIN abis_courses c ON c.id = o.course_id " +
                        "WHERE o.order_date = :orderDate AND o.course_id = :courseId")
                .param("orderDate", orderDate)
                .param("courseId", courseId)
                .query((rs, rowNum) -> {
                    SandwichOrder order = new SandwichOrder(
                            rs.getObject("order_date", LocalDate.class),
                            new Course(rs.getInt("course_id"), rs.getString("title")));
                    order.setId(rs.getInt("id"));
                    order.setTotalPrice(rs.getDouble("total_price"));
                    return order;
                })
                .optional();
    }

    @Override
    public SandwichOrder addSandwichOrder(SandwichOrder sandwichOrder) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO sandwich_orders (order_date, course_id) VALUES (:orderDate, :courseId)")
                .param("orderDate", sandwichOrder.getOrderDate())
                .param("courseId", sandwichOrder.getCourse().getId())
                .update(keys, "ID");
        sandwichOrder.setId(keys.getKey().intValue());
        return sandwichOrder;
    }

    @Override
    public void updateTotalPrice(int orderId, double totalPrice) {
        jdbc.sql("UPDATE sandwich_orders SET total_price = :totalPrice WHERE id = :id")
                .param("totalPrice", totalPrice)
                .param("id", orderId)
                .update();
    }
}
