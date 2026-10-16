package be.abis.sandwich.service;

import be.abis.sandwich.model.Sandwich;

import java.util.List;

public interface SandwichService {

    /** All sandwiches, optionally only one category and/or only names containing the given text (both case-insensitive). */
    List<Sandwich> findSandwiches(String category, String name);
    Sandwich findSandwichById(int id);
    Sandwich addSandwich(Sandwich sandwich);
    Sandwich updatePrice(int id, double basePrice);
    void deleteSandwich(int id);
}
