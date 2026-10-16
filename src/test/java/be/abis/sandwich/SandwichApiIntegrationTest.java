package be.abis.sandwich;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The whole application (controllers, services, repositories, H2 database) through HTTP calls. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class SandwichApiIntegrationTest {

    @TempDir
    static Path printDirectory;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("sandwich.print.directory", () -> printDirectory.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    private static String order(int sandwichId, int amount, String first, String last) {
        return """
                {"sandwich": {"id": %d}, "amount": %d, "breadType": "BROWN", "vegetables": true, "comment": "",
                 "person": {"firstName": "%s", "lastName": "%s"},
                 "sandwichOrder": {"orderDate": "2026-10-16", "course": {"title": "Integration"}}}"""
                .formatted(sandwichId, amount, first, last);
    }

    private static final String ORDER = """
            {"orderDate": "2026-10-16", "course": {"title": "Integration"}}""";

    @Test
    void sandwichesCanBeFoundAddedRepricedAndRemoved() throws Exception {
        mockMvc.perform(get("/api/sandwiches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Pinky')].category").value("Specials"));
        mockMvc.perform(get("/api/sandwiches").param("category", "Kip"))
                .andExpect(jsonPath("$.length()").value(3));
        mockMvc.perform(get("/api/sandwiches").param("name", "TONIJN"))
                .andExpect(jsonPath("$.length()").value(2));

        String location = mockMvc.perform(post("/api/sandwiches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Integratiebroodje\", \"description\": \"x\", \"category\": \"Specials\", \"basePrice\": 5}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        assertThat(location).contains("/api/sandwiches/");
        String path = location.substring(location.indexOf("/api"));

        mockMvc.perform(put(path + "/price").contentType(MediaType.APPLICATION_JSON).content("{\"basePrice\": 5.5}"))
                .andExpect(status().isOk());
        mockMvc.perform(get(path)).andExpect(jsonPath("$.basePrice").value(5.5));

        mockMvc.perform(delete(path)).andExpect(status().isNoContent());
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
    }

    @Test
    void ordersAreLimitedPricedSavedAndPrinted() throws Exception {
        int hesp = idOf("Hesp");
        int pinky = idOf("Pinky");

        mockMvc.perform(post("/api/orders/details").contentType(MediaType.APPLICATION_JSON).content(order(pinky, 1, "Ada", "Lovelace")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/orders/details").contentType(MediaType.APPLICATION_JSON).content(order(hesp, 1, "ada", "lovelace")))
                .andExpect(status().isCreated());
        // a third sandwich for the same person is too much
        mockMvc.perform(post("/api/orders/details").contentType(MediaType.APPLICATION_JSON).content(order(hesp, 1, "Ada", "Lovelace")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("at most 2")));
        // someone else may still order
        mockMvc.perform(post("/api/orders/details").contentType(MediaType.APPLICATION_JSON).content(order(hesp, 2, "Bob", "Builder")))
                .andExpect(status().isCreated());

        // 4.8 + 3.2 + 2 * 3.2
        mockMvc.perform(post("/api/orders/price").contentType(MediaType.APPLICATION_JSON).content(ORDER))
                .andExpect(status().isOk())
                .andExpect(content().string("14.4"));

        mockMvc.perform(post("/api/orders/print").contentType(MediaType.APPLICATION_JSON).content(ORDER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ada Lovelace")))
                .andExpect(content().string(containsString("Total: € 14.40")));
        Path file = printDirectory.resolve("sandwich-order_2026-10-16_integration.txt");
        assertThat(file).exists();
        assertThat(Files.readString(file)).contains("Bob Builder", "2x Hesp");

        // a sandwich that is part of an order can no longer be removed
        mockMvc.perform(delete("/api/sandwiches/" + pinky)).andExpect(status().isConflict());
    }

    @Test
    void ordersAtTheSameMomentNeitherFailNorBreakTheLimit() throws Exception {
        int hesp = idOf("Hesp");
        String body = """
                {"sandwich": {"id": %d}, "amount": 1, "breadType": "WHITE", "vegetables": true, "comment": "",
                 "person": {"firstName": "Grace", "lastName": "Hopper"},
                 "sandwichOrder": {"orderDate": "2026-10-16", "course": {"title": "Concurrency"}}}""".formatted(hesp);

        int parallel = 8;
        ExecutorService pool = Executors.newFixedThreadPool(parallel);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < parallel; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/orders/details").contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        // first of the day created the order, person and course; only 2 sandwiches fit, the rest is a clean 409
        assertThat(statuses).filteredOn(code -> code == 201).hasSize(2);
        assertThat(statuses).filteredOn(code -> code == 409).hasSize(parallel - 2);
    }

    @Test
    void pricingAnOrderThatDoesNotExistIs404() throws Exception {
        mockMvc.perform(post("/api/orders/price").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderDate\": \"2000-01-01\", \"course\": {\"title\": \"Nothing\"}}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void theApiIsDocumentedAndTheFrontendIsServed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Abis Sandwich API"))
                .andExpect(jsonPath("$.paths['/api/sandwiches']").exists())
                .andExpect(jsonPath("$.paths['/api/orders/details']").exists());
        // "/" is forwarded to the Angular index.html (MockMvc does not follow forwards itself)
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mockMvc.perform(get("/index.html")).andExpect(status().isOk()).andExpect(content().string(containsString("<app-root>")));
        mockMvc.perform(get("/order")).andExpect(status().isOk()).andExpect(content().string(containsString("<app-root>")));
    }

    private int idOf(String name) throws Exception {
        String json = mockMvc.perform(get("/api/sandwiches").param("name", name))
                .andReturn().getResponse().getContentAsString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\":(\\d+),\"name\":\"" + name + "\"").matcher(json);
        assertThat(m.find()).as("sandwich %s in %s", name, json).isTrue();
        return Integer.parseInt(m.group(1));
    }
}
