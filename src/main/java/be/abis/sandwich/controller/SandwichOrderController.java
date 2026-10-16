package be.abis.sandwich.controller;

import be.abis.sandwich.dto.OrderDetailRequest;
import be.abis.sandwich.dto.OrderDetailResponse;
import be.abis.sandwich.dto.OrderRequest;
import be.abis.sandwich.service.SandwichOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Talks DTOs only: the entities of the model package never leave the service layer. */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Sandwich orders", description = "Order sandwiches; print and price the order of a course")
public class SandwichOrderController {

    private final SandwichOrderService sandwichOrderService;

    public SandwichOrderController(SandwichOrderService sandwichOrderService) {
        this.sandwichOrderService = sandwichOrderService;
    }

    @PostMapping("/details")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Order a sandwich",
            description = "Adds a sandwich to the order of the course on that day. The order, the course and the person are created if they don't exist yet. " +
                    "Only the id of the sandwich is used. A person can order at most 2 sandwiches per order.")
    @ApiResponse(responseCode = "400", description = "Invalid order")
    @ApiResponse(responseCode = "404", description = "Unknown sandwich")
    @ApiResponse(responseCode = "409", description = "The person already ordered the maximum of 2 sandwiches")
    public OrderDetailResponse addSandwichOrderDetail(@Valid @RequestBody OrderDetailRequest request) {
        return OrderDetailResponse.from(sandwichOrderService.addSandwichOrderDetail(request.toEntity()));
    }

    @PostMapping(value = "/print", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Print the order", description = "Writes the order of the course on that day to a .txt file and returns the same text.")
    @ApiResponse(responseCode = "404", description = "There is no order for this course on this day")
    public String printSandwichOrder(@Valid @RequestBody OrderRequest request) {
        return sandwichOrderService.printSandwichOrder(request.toEntity());
    }

    @PostMapping("/price")
    @Operation(summary = "Calculate the total price", description = "Calculates the total price of the order and saves it in the database. Returns the price.")
    @ApiResponse(responseCode = "404", description = "There is no order for this course on this day")
    public double calculateSandwichOrderPrice(@Valid @RequestBody OrderRequest request) {
        return sandwichOrderService.calculateSandwichOrderPrice(request.toEntity());
    }
}
