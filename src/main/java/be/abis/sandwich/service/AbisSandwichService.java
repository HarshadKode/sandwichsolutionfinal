package be.abis.sandwich.service;

import be.abis.sandwich.exception.DuplicateSandwichException;
import be.abis.sandwich.exception.SandwichInUseException;
import be.abis.sandwich.exception.SandwichNotFoundException;
import be.abis.sandwich.model.Sandwich;
import be.abis.sandwich.repository.SandwichRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class AbisSandwichService implements SandwichService {

    private final SandwichRepository sandwichRepository;

    public AbisSandwichService(SandwichRepository sandwichRepository) {
        this.sandwichRepository = sandwichRepository;
    }

    @Override
    public List<Sandwich> findSandwiches(String category, String name) {
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasName = name != null && !name.isBlank();

        List<Sandwich> sandwiches = hasCategory
                ? sandwichRepository.findSandwichesByCategory(category.trim())
                : sandwichRepository.findAllSandwiches();
        if (!hasName) {
            return sandwiches;
        }
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        return sandwiches.stream()
                .filter(s -> s.getName().toLowerCase(Locale.ROOT).contains(wanted))
                .toList();
    }

    @Override
    public Sandwich findSandwichById(int id) {
        return sandwichRepository.findSandwichById(id).orElseThrow(() -> new SandwichNotFoundException(id));
    }

    @Override
    public Sandwich addSandwich(Sandwich sandwich) {
        if (sandwichRepository.findSandwichByName(sandwich.getName()).isPresent()) {
            throw new DuplicateSandwichException(sandwich.getName());
        }
        return sandwichRepository.addSandwich(sandwich);
    }

    @Override
    public Sandwich updatePrice(int id, double basePrice) {
        Sandwich sandwich = findSandwichById(id);
        sandwich.setBasePrice(basePrice);
        sandwichRepository.updatePrice(sandwich);
        return sandwich;
    }

    @Override
    public void deleteSandwich(int id) {
        Sandwich sandwich = findSandwichById(id);
        try {
            sandwichRepository.deleteSandwich(id);
        } catch (DataIntegrityViolationException e) {
            throw new SandwichInUseException(sandwich.getName());
        }
    }
}
