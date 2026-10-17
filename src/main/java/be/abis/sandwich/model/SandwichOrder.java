package be.abis.sandwich.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/** The sandwich order of one course on one day. */
@Entity
@Table(name = "sandwich_orders",
        uniqueConstraints = @UniqueConstraint(name = "uq_orders_date_course", columnNames = {"order_date", "course_id"}))
public class SandwichOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;
    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    @Column(name = "total_price", nullable = false, columnDefinition = "NUMBER(8,2)")
    private double totalPrice;

    public SandwichOrder(){

    }

    public SandwichOrder(LocalDate orderDate, Course course) {
        this.orderDate = orderDate;
        this.course = course;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    /** Total price of the order, filled in when the price has been calculated (and saved in the database). */
    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    /** The total price of these details: the sum of their prices, rounded to 2 decimals (BigDecimal, so no floating point errors). */
    public static double totalPriceOf(List<SandwichOrderDetail> details) {
        return details.stream()
                .map(SandwichOrderDetail::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

}
