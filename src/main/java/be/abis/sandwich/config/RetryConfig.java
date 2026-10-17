package be.abis.sandwich.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;

@Configuration
public class RetryConfig {

    /**
     * Tries an action again when it failed because it collided with another one that ran at the same moment: a database
     * concurrency failure, or a unique constraint (the other one just created the same row). The waiting time between the
     * attempts is random and doubles every time (25 ms, 50 ms, ... at most 800 ms), so that colliding actions don't
     * collide again at the same moment. After 10 attempts the last exception is thrown.
     */
    @Bean
    public RetryTemplate orderRetryTemplate() {
        return new RetryTemplate(RetryPolicy.builder()
                .includes(ConcurrencyFailureException.class, DataIntegrityViolationException.class)
                .maxRetries(9)
                .delay(Duration.ofMillis(25))
                .jitter(Duration.ofMillis(25))
                .multiplier(2)
                .maxDelay(Duration.ofMillis(800))
                .build());
    }
}
