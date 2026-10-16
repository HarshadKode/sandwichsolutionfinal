package be.abis.sandwich.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public class Sandwich {

    private int id;
    @NotBlank(message = "name is required")
    private String name;
    private String description;
    @NotBlank(message = "category is required")
    private String category;
    @PositiveOrZero(message = "basePrice must not be negative")
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
