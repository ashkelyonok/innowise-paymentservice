package org.ashkelyonok.paymentservice.client;

import org.ashkelyonok.paymentservice.client.fallback.RandomApiFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Feign client for interacting with a random number generation API.
 * Provides a method to fetch a random number string from an external service.
 * Includes fallback support for handling service unavailability.
 */
@FeignClient(
        name = "randomApi",
        url = "${random.api.url}",
        fallback = RandomApiFallback.class
)
public interface RandomApiClient {

    /**
     * Fetches a random number from the external API as a string.
     * The response format depends on the external API implementation.
     * In case of failure, the configured fallback will be used.
     *
     * @return a string representation of a random number from the external API
     */
    @GetMapping
    String fetchRandomNumberString();
}
