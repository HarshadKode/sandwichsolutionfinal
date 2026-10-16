package be.abis.sandwich.repository;

import be.abis.sandwich.model.Sandwich;

import java.util.List;
import java.util.Optional;

public interface SandwichRepository {

    List<Sandwich> findAllSandwiches();
    Optional<Sandwich> findSandwichById(int id);
    Optional<Sandwich> findSandwichByName(String name);
    List<Sandwich> findSandwichesByCategory(String category);
    /** Inserts the sandwich and returns it with its generated id. */
    Sandwich addSandwich(Sandwich sandwich);
    void updatePrice(Sandwich sandwich);
    void deleteSandwich(int id);

}
