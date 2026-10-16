package be.abis.sandwich.repository;

import be.abis.sandwich.model.Sandwich;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs against the embedded H2 database that Hibernate creates and data.sql fills. Every test is rolled back. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SandwichRepositoryTest {

    @Autowired
    private SandwichRepository repository;

    @Test
    void findAllByOrderByCategoryAscNameAscReturnsTheMenuSorted() {
        List<Sandwich> all = repository.findAllByOrderByCategoryAscNameAsc();

        assertThat(all).hasSizeGreaterThan(20);
        assertThat(all).extracting(Sandwich::getName).contains("Pinky", "Hesp", "Kip curry");
        assertThat(all).extracting(Sandwich::getCategory).isSorted();
    }

    @Test
    void findByIdFindsAnExistingSandwichAndNothingForAnUnknownId() {
        int id = repository.findAll().get(0).getId();

        assertThat(repository.findById(id)).isPresent();
        assertThat(repository.findById(-1)).isEmpty();
    }

    @Test
    void findByNameIgnoreCaseIgnoresCase() {
        Optional<Sandwich> found = repository.findByNameIgnoreCase("pINKy");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Pinky");
        assertThat(found.get().getCategory()).isEqualTo("Specials");
        assertThat(found.get().getBasePrice()).isEqualTo(4.8);
    }

    @Test
    void findByCategoryOnlyReturnsThatCategorySortedByName() {
        List<Sandwich> chicken = repository.findByCategoryIgnoreCaseOrderByName("kip");

        assertThat(chicken).hasSize(3);
        assertThat(chicken).extracting(Sandwich::getCategory).containsOnly("Kip");
        assertThat(chicken).extracting(Sandwich::getName).isSorted();
    }

    @Test
    void saveGeneratesAnIdAndStoresTheSandwich() {
        Sandwich saved = repository.saveAndFlush(new Sandwich(0, "Testbroodje", "For testing", "Specials", 5.25));

        assertThat(saved.getId()).isPositive();
        Sandwich stored = repository.findById(saved.getId()).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Testbroodje");
        assertThat(stored.getDescription()).isEqualTo("For testing");
        assertThat(stored.getBasePrice()).isEqualTo(5.25);
    }

    @Test
    void theNameMustBeUnique() {
        assertThatThrownBy(() -> repository.saveAndFlush(new Sandwich(0, "Pinky", "again", "Specials", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void changingThePriceOfASavedSandwichIsStored() {
        Sandwich pinky = repository.findByNameIgnoreCase("Pinky").orElseThrow();
        pinky.setBasePrice(9.99);

        repository.saveAndFlush(pinky);

        assertThat(repository.findById(pinky.getId()).orElseThrow().getBasePrice()).isEqualTo(9.99);
    }

    @Test
    void deleteRemovesTheSandwich() {
        Sandwich saved = repository.saveAndFlush(new Sandwich(0, "Wegbroodje", "To be deleted", "Vlees", 1));

        repository.delete(saved);
        repository.flush();

        assertThat(repository.findById(saved.getId())).isEmpty();
    }
}
