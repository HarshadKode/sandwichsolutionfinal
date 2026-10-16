package be.abis.sandwich.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class SandwichOrder {

    private int id;
    @NotNull(message = "orderDate is required (yyyy-MM-dd)")
    private LocalDate orderDate;
    @NotNull(message = "course is required")
    @Valid
    private Course course;
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

}
