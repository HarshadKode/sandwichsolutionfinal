package be.abis.sandwich.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and description of the generated API documentation (Swagger UI at /swagger-ui.html). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sandwichOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Abis Sandwich API")
                .version("1.0")
                .description("Manage the sandwiches on the order list, order sandwiches for a course, " +
                        "print the order to a .txt file and calculate its total price."));
    }
}
