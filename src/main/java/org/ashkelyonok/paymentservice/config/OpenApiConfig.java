package org.ashkelyonok.paymentservice.config;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .version("1.0.0")
                        .description("Microservice for managing orders.")
                        .contact(new Contact()
                                .name("Anastasia Shkelyonok")
                                .email("anastasia.shkelyonok@gmail.com")))
                .servers(
                        List.of(new Server().url("http://localhost:8083").description("Development server")));
    }
}
