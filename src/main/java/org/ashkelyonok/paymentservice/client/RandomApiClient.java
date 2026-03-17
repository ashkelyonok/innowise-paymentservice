package org.ashkelyonok.paymentservice.client;

import org.ashkelyonok.paymentservice.client.fallback.RandomApiFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "randomApi",
        url = "${random.api.url}",
        fallback = RandomApiFallback.class
)
public interface RandomApiClient {

    @GetMapping
    String fetchRandomNumberString();
}
