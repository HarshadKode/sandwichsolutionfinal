package be.abis.sandwich.controller;

import be.abis.sandwich.exception.DuplicateSandwichException;
import be.abis.sandwich.exception.SandwichInUseException;
import be.abis.sandwich.exception.SandwichNotFoundException;
import be.abis.sandwich.model.Sandwich;
import be.abis.sandwich.service.SandwichService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SandwichController.class)
class SandwichControllerTest {

    private static final String PINKY_JSON = """
            {"name": "Pinky", "description": "Special", "category": "Specials", "basePrice": 4.8}""";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SandwichService sandwichService;

    private final Sandwich hesp = new Sandwich(1, "Hesp", "Ham", "Vlees", 3.2);

    @Test
    void getReturnsTheSandwichesAsJson() throws Exception {
        when(sandwichService.findSandwiches(null, null)).thenReturn(List.of(hesp));

        mockMvc.perform(get("/api/sandwiches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Hesp"))
                .andExpect(jsonPath("$[0].category").value("Vlees"))
                .andExpect(jsonPath("$[0].basePrice").value(3.2));
    }

    @Test
    void theApiOnlyExposesTheFieldsOfTheDto() throws Exception {
        when(sandwichService.findSandwiches(null, null)).thenReturn(List.of(hesp));

        mockMvc.perform(get("/api/sandwiches"))
                .andExpect(jsonPath("$[0]", org.hamcrest.Matchers.aMapWithSize(5)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].basePrice").exists());
    }

    @Test
    void getPassesTheFiltersToTheService() throws Exception {
        when(sandwichService.findSandwiches("Vlees", "he")).thenReturn(List.of(hesp));

        mockMvc.perform(get("/api/sandwiches").param("category", "Vlees").param("name", "he"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Hesp"));
    }

    @Test
    void getByIdReturnsTheSandwich() throws Exception {
        when(sandwichService.findSandwichById(1)).thenReturn(hesp);

        mockMvc.perform(get("/api/sandwiches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hesp"));
    }

    @Test
    void getByIdOfAnUnknownSandwichIs404WithAMessage() throws Exception {
        when(sandwichService.findSandwichById(99)).thenThrow(new SandwichNotFoundException(99));

        mockMvc.perform(get("/api/sandwiches/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Sandwich with id 99 does not exist"));
    }

    @Test
    void postCreatesTheSandwichAndAnswers201WithALocation() throws Exception {
        when(sandwichService.addSandwich(any(Sandwich.class))).thenAnswer(call -> {
            Sandwich s = call.getArgument(0);
            s.setId(42);
            return s;
        });

        mockMvc.perform(post("/api/sandwiches").contentType(MediaType.APPLICATION_JSON).content(PINKY_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/sandwiches/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("Pinky"));
    }

    @Test
    void postIgnoresAnIdSentByTheClient() throws Exception {
        when(sandwichService.addSandwich(any(Sandwich.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(post("/api/sandwiches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 1, \"name\": \"Pinky\", \"category\": \"Specials\", \"basePrice\": 4.8}"))
                .andExpect(status().isCreated());

        org.mockito.ArgumentCaptor<Sandwich> captor = org.mockito.ArgumentCaptor.forClass(Sandwich.class);
        verify(sandwichService).addSandwich(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getId()).isZero();
    }

    @Test
    void postWithInvalidDataIs400ListingTheProblems() throws Exception {
        mockMvc.perform(post("/api/sandwiches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"basePrice\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("name is required"),
                        org.hamcrest.Matchers.containsString("category is required"),
                        org.hamcrest.Matchers.containsString("basePrice must not be negative"))));
        verify(sandwichService, never()).addSandwich(any());
    }

    @Test
    void postOfADuplicateNameIs409() throws Exception {
        when(sandwichService.addSandwich(any(Sandwich.class))).thenThrow(new DuplicateSandwichException("Pinky"));

        mockMvc.perform(post("/api/sandwiches").contentType(MediaType.APPLICATION_JSON).content(PINKY_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A sandwich called 'Pinky' already exists"));
    }

    @Test
    void postOfBrokenJsonIs400() throws Exception {
        mockMvc.perform(post("/api/sandwiches").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void putPriceUpdatesThePrice() throws Exception {
        when(sandwichService.updatePrice(1, 3.9)).thenReturn(new Sandwich(1, "Hesp", "Ham", "Vlees", 3.9));

        mockMvc.perform(put("/api/sandwiches/1/price").contentType(MediaType.APPLICATION_JSON).content("{\"basePrice\": 3.9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basePrice").value(3.9));
    }

    @Test
    void putPriceWithoutOrWithNegativePriceIs400() throws Exception {
        mockMvc.perform(put("/api/sandwiches/1/price").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("basePrice is required"));
        mockMvc.perform(put("/api/sandwiches/1/price").contentType(MediaType.APPLICATION_JSON).content("{\"basePrice\": -2}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putPriceOfAnUnknownSandwichIs404() throws Exception {
        when(sandwichService.updatePrice(99, 1.0)).thenThrow(new SandwichNotFoundException(99));

        mockMvc.perform(put("/api/sandwiches/99/price").contentType(MediaType.APPLICATION_JSON).content("{\"basePrice\": 1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAnswers204() throws Exception {
        mockMvc.perform(delete("/api/sandwiches/1")).andExpect(status().isNoContent());

        verify(sandwichService).deleteSandwich(1);
    }

    @Test
    void deleteOfASandwichInAnOrderIs409() throws Exception {
        doThrow(new SandwichInUseException("Hesp")).when(sandwichService).deleteSandwich(1);

        mockMvc.perform(delete("/api/sandwiches/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Sandwich 'Hesp' is part of existing orders and cannot be removed"));
    }

    @Test
    void unexpectedErrorsAre500WithoutLeakingDetails() throws Exception {
        when(sandwichService.findSandwiches(null, null)).thenThrow(new IllegalStateException("secret database password"));

        mockMvc.perform(get("/api/sandwiches"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Something went wrong on the server"));
    }
}
