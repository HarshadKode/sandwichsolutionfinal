package be.abis.sandwich.controller;

import be.abis.sandwich.exception.MaxSandwichesExceededException;
import be.abis.sandwich.exception.SandwichNotFoundException;
import be.abis.sandwich.exception.SandwichOrderNotFoundException;
import be.abis.sandwich.model.SandwichOrder;
import be.abis.sandwich.model.SandwichOrderDetail;
import be.abis.sandwich.service.SandwichOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SandwichOrderController.class)
class SandwichOrderControllerTest {

    private static final String DETAIL_JSON = """
            {
              "sandwich": {"id": 21, "name": "Pinky", "description": "x", "category": "Specials", "basePrice": 4.8},
              "amount": 1,
              "breadType": "WHITE",
              "vegetables": true,
              "comment": "no onions",
              "person": {"firstName": "Ada", "lastName": "Lovelace"},
              "sandwichOrder": {"orderDate": "2026-10-16", "course": {"title": "Spring Boot"}}
            }""";

    private static final String ORDER_JSON = """
            {"orderDate": "2026-10-16", "course": {"title": "Spring Boot"}}""";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SandwichOrderService sandwichOrderService;

    @Test
    void orderingASandwichAnswers201AndPassesTheWholeDetailToTheService() throws Exception {
        mockMvc.perform(post("/api/orders/details").contentType(MediaType.APPLICATION_JSON).content(DETAIL_JSON))
                .andExpect(status().isCreated());

        ArgumentCaptor<SandwichOrderDetail> captor = ArgumentCaptor.forClass(SandwichOrderDetail.class);
        verify(sandwichOrderService).addSandwichOrderDetail(captor.capture());
        SandwichOrderDetail sod = captor.getValue();
        assertThat(sod.getSandwich().getId()).isEqualTo(21);
        assertThat(sod.getAmount()).isEqualTo(1);
        assertThat(sod.getBreadType().name()).isEqualTo("WHITE");
        assertThat(sod.isVegetables()).isTrue();
        assertThat(sod.getComment()).isEqualTo("no onions");
        assertThat(sod.getPerson().getLastName()).isEqualTo("Lovelace");
        assertThat(sod.getSandwichOrder().getOrderDate()).isEqualTo(LocalDate.of(2026, 10, 16));
        assertThat(sod.getSandwichOrder().getCourse().getTitle()).isEqualTo("Spring Boot");
    }

    @Test
    void orderingMoreThanTwoAtOnceIs400() throws Exception {
        mockMvc.perform(post("/api/orders/details").contentType(MediaType.APPLICATION_JSON)
                        .content(DETAIL_JSON.replace("\"amount\": 1", "\"amount\": 3")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("you can order at most 2 sandwiches"));
        verify(sandwichOrderService, never()).addSandwichOrderDetail(any());
    }

    @Test
    void orderWithMissingPersonAndCourseIs400() throws Exception {
        String json = DETAIL_JSON
                .replace("\"person\": {\"firstName\": \"Ada\", \"lastName\": \"Lovelace\"}", "\"person\": {\"firstName\": \"\"}")
                .replace("\"course\": {\"title\": \"Spring Boot\"}", "\"course\": {\"title\": \" \"}");

        mockMvc.perform(post("/api/orders/details").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("first name is required")))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("last name is required")))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("course title is required")));
    }

    @Test
    void anUnknownBreadTypeIs400() throws Exception {
        mockMvc.perform(post("/api/orders/details").contentType(MediaType.APPLICATION_JSON)
                        .content(DETAIL_JSON.replace("WHITE", "RYE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("WHITE or BROWN")));
    }

    @Test
    void orderingAnUnknownSandwichIs404() throws Exception {
        doThrow(new SandwichNotFoundException(21)).when(sandwichOrderService).addSandwichOrderDetail(any());

        mockMvc.perform(post("/api/orders/details").contentType(MediaType.APPLICATION_JSON).content(DETAIL_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void orderingMoreThanTwoSandwichesInTotalIs409() throws Exception {
        doThrow(new MaxSandwichesExceededException("Ada Lovelace", 2)).when(sandwichOrderService).addSandwichOrderDetail(any());

        mockMvc.perform(post("/api/orders/details").contentType(MediaType.APPLICATION_JSON).content(DETAIL_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ada Lovelace can order at most 2 sandwiches per order"));
    }

    @Test
    void printReturnsThePrintedTextAsPlainText() throws Exception {
        when(sandwichOrderService.printSandwichOrder(any(SandwichOrder.class))).thenReturn("SANDWICH ORDER\nTotal: € 4.80\n");

        mockMvc.perform(post("/api/orders/print").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("SANDWICH ORDER\nTotal: € 4.80\n"));
    }

    @Test
    void printOfAnUnknownOrderIs404() throws Exception {
        when(sandwichOrderService.printSandwichOrder(any(SandwichOrder.class)))
                .thenThrow(new SandwichOrderNotFoundException(LocalDate.of(2026, 10, 16), "Spring Boot"));

        mockMvc.perform(post("/api/orders/print").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Spring Boot")));
    }

    @Test
    void priceReturnsTheTotalAsANumber() throws Exception {
        when(sandwichOrderService.calculateSandwichOrderPrice(any(SandwichOrder.class))).thenReturn(12.6);

        mockMvc.perform(post("/api/orders/price").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("12.6"));
    }

    @Test
    void priceWithoutADateIs400() throws Exception {
        mockMvc.perform(post("/api/orders/price").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"course\": {\"title\": \"Spring Boot\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("orderDate is required")));
    }
}
