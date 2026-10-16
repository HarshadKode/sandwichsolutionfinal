package be.abis.sandwich.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "sandwiches", uniqueConstraints = @UniqueConstraint(name = "uq_sandwiches_name", columnNames = "name"))
public class Sandwich {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 500)
    private String description;
    @Column(nullable = false, length = 50)
    private String category;
    @Column(name = "base_price", nullable = false, columnDefinition = "NUMBER(6,2)")
    private double basePrice;

    public Sandwich(){}

    public Sandwich(int id, String name, String description, String category) {
        this.id=id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public Sandwich(int id,String name, String description, String category, double basePrice) {
        this(id,name, description, category);
        this.basePrice = basePrice;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }


}
