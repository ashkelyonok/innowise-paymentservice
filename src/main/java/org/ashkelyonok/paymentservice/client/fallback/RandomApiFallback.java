package org.ashkelyonok.paymentservice.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.paymentservice.client.RandomApiClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class RandomApiFallback implements RandomApiClient {

    @Override
    public String fetchRandomNumberString() {
        log.warn("Random API is unreachable. Generating fallback random number locally.");
        int fallbackNumber = ThreadLocalRandom.current().nextInt(1, 101);
        return String.valueOf(fallbackNumber);
    }
}
