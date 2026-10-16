package be.abis.sandwich.repository;

import be.abis.sandwich.model.Sandwich;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs against the embedded H2 database filled by schema.sql and data.sql. Every test is rolled back. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DBSandwichRepository.class)
class DBSandwichRepositoryTest {

    @Autowired
    private DBSandwichRepository repository;

    @Test
    void findAllSandwichesReturnsTheMenuSortedByCategoryAndName() {
        List<Sandwich> all = repository.findAllSandwiches();

        assertThat(all).hasSizeGreaterThan(20);
        assertThat(all).extracting(Sandwich::getName).contains("Pinky", "Hesp", "Kip curry");
        assertThat(all).extracting(Sandwich::getCategory).isSorted();
    }

    @Test
    void findSandwichByIdFindsAnExistingSandwich() {
        int id = repository.findAllSandwiches().get(0).getId();

        Optional<Sandwich> found = repository.findSandwichById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(id);
    }

    @Test
    void findSandwichByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findSandwichById(-1)).isEmpty();
    }

    @Test
    void findSandwichByNameIgnoresCase() {
        Optional<Sandwich> found = repository.findSandwichByName("pINKy");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Pinky");
        assertThat(found.get().getCategory()).isEqualTo("Specials");
        assertThat(found.get().getBasePrice()).isEqualTo(4.8);
    }

    @Test
    void findSandwichesByCategoryOnlyReturnsThatCategory() {
        List<Sandwich> chicken = repository.findSandwichesByCategory("kip");

        assertThat(chicken).hasSize(3);
        assertThat(chicken).extracting(Sandwich::getCategory).containsOnly("Kip");
    }

    @Test
    void addSandwichGeneratesAnIdAndStoresTheSandwich() {
        Sandwich added = repository.addSandwich(new Sandwich(0, "Testbroodje", "For testing", "Specials", 5.25));

        assertThat(added.getId()).isPositive();
        Sandwich stored = repository.findSandwichById(added.getId()).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Testbroodje");
        assertThat(stored.getDescription()).isEqualTo("For testing");
        assertThat(stored.getBasePrice()).isEqualTo(5.25);
    }

    @Test
    void updatePriceChangesOnlyThePrice() {
        Sandwich pinky = repository.findSandwichByName("Pinky").orElseThrow();
        pinky.setBasePrice(9.99);

        repository.updatePrice(pinky);

        Sandwich stored = repository.findSandwichById(pinky.getId()).orElseThrow();
        assertThat(stored.getBasePrice()).isEqualTo(9.99);
        assertThat(stored.getName()).isEqualTo("Pinky");
    }

    @Test
    void deleteSandwichRemovesIt() {
        Sandwich added = repository.addSandwich(new Sandwich(0, "Wegbroodje", "To be deleted", "Vlees", 1));

        repository.deleteSandwich(added.getId());

        assertThat(repository.findSandwichById(added.getId())).isEmpty();
    }
}
