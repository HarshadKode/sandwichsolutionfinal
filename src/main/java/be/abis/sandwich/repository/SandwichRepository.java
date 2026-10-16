package be.abis.sandwich.repository;

import be.abis.sandwich.model.Sandwich;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SandwichRepository extends JpaRepository<Sandwich, Integer> {

    List<Sandwich> findAllByOrderByCategoryAscNameAsc();

    List<Sandwich> findByCategoryIgnoreCaseOrderByName(String category);

    Optional<Sandwich> findByNameIgnoreCase(String name);
}
