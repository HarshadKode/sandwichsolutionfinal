package be.abis.sandwich.controller;

import be.abis.sandwich.dto.PriceUpdate;
import be.abis.sandwich.model.Sandwich;
import be.abis.sandwich.service.SandwichService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/sandwiches")
@Tag(name = "Sandwiches", description = "The sandwiches on the order list")
public class SandwichController {

    private final SandwichService sandwichService;

    public SandwichController(SandwichService sandwichService) {
        this.sandwichService = sandwichService;
    }

    @GetMapping
    @Operation(summary = "Find sandwiches", description = "Without parameters: the full list. Filter by category and/or by (part of) the name.")
    public List<Sandwich> findSandwiches(
            @Parameter(description = "Exact category, e.g. Vlees", example = "Vlees") @RequestParam(required = false) String category,
            @Parameter(description = "Part of the name, e.g. pink", example = "pink") @RequestParam(required = false) String name) {
        return sandwichService.findSandwiches(category, name);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a sandwich by id")
    @ApiResponse(responseCode = "404", description = "No sandwich with this id")
    public Sandwich findSandwichById(@PathVariable int id) {
        return sandwichService.findSandwichById(id);
    }

    @PostMapping
    @Operation(summary = "Add a sandwich", description = "The id is generated; the name must be unique.")
    @ApiResponse(responseCode = "201", description = "Created; the Location header points to the new sandwich")
    @ApiResponse(responseCode = "400", description = "Invalid sandwich")
    @ApiResponse(responseCode = "409", description = "A sandwich with this name already exists")
    public ResponseEntity<Sandwich> addSandwich(@Valid @RequestBody Sandwich sandwich) {
        Sandwich created = sandwichService.addSandwich(sandwich);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}/price")
    @Operation(summary = "Update the price of a sandwich")
    @ApiResponse(responseCode = "400", description = "Missing or negative price")
    @ApiResponse(responseCode = "404", description = "No sandwich with this id")
    public Sandwich updatePrice(@PathVariable int id, @Valid @RequestBody PriceUpdate update) {
        return sandwichService.updatePrice(id, update.basePrice());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a sandwich from the list")
    @ApiResponse(responseCode = "204", description = "Removed")
    @ApiResponse(responseCode = "404", description = "No sandwich with this id")
    @ApiResponse(responseCode = "409", description = "The sandwich is part of existing orders")
    public ResponseEntity<Void> deleteSandwich(@PathVariable int id) {
        sandwichService.deleteSandwich(id);
        return ResponseEntity.noContent().build();
    }
}
