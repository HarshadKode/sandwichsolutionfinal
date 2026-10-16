package be.abis.sandwich.model;

import be.abis.sandwich.enumeration.BreadType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SandwichOrderDetail {

    private int id;
    @NotNull(message = "sandwich is required")
    private Sandwich sandwich;
    @Min(value = 1, message = "amount must be at least 1")
    @Max(value = 2, message = "you can order at most 2 sandwiches")
    private int amount=1;
    @NotNull(message = "breadType is required (WHITE or BROWN)")
    private BreadType breadType;
    private boolean vegetables;
    private String comment;
    @NotNull(message = "person is required")
    @Valid
    private Person person;
    @NotNull(message = "sandwichOrder is required")
    @Valid
    private SandwichOrder sandwichOrder;

    public SandwichOrderDetail(){}

    public SandwichOrderDetail(Sandwich sandwich, int amount, BreadType breadType, boolean vegetables, String comment, Person person) {
        this.sandwich = sandwich;
        this.amount = amount;
        this.breadType = breadType;
        this.vegetables = vegetables;
        this.comment = comment;
        this.person = person;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Sandwich getSandwich() {
        return sandwich;
    }

    public void setSandwich(Sandwich sandwich) {
        this.sandwich = sandwich;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public BreadType getBreadType() {
        return breadType;
    }

    public void setBreadType(BreadType breadType) {
        this.breadType = breadType;
    }

    public boolean isVegetables() {
        return vegetables;
    }

    public void setVegetables(boolean vegetables) {
        this.vegetables = vegetables;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public SandwichOrder getSandwichOrder() {
        return sandwichOrder;
    }

    public void setSandwichOrder(SandwichOrder sandwichOrder) {
        this.sandwichOrder = sandwichOrder;
    }
}
