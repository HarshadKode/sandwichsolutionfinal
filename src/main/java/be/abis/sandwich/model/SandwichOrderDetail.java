package be.abis.sandwich.model;

import be.abis.sandwich.enumeration.BreadType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** One person's sandwich in a {@link SandwichOrder}. */
@Entity
@Table(name = "sandwich_order_details")
public class SandwichOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "sandwich_id", nullable = false)
    private Sandwich sandwich;
    @Column(nullable = false)
    private int amount=1;
    @Enumerated(EnumType.STRING)
    @Column(name = "bread_type", nullable = false, length = 10)
    private BreadType breadType;
    @Column(nullable = false)
    private boolean vegetables;
    @Column(name = "order_comment", length = 200)
    private String comment;
    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;
    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
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

    /** Base price of the sandwich times the amount. */
    public BigDecimal getPrice() {
        return BigDecimal.valueOf(sandwich.getBasePrice()).multiply(BigDecimal.valueOf(amount));
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
