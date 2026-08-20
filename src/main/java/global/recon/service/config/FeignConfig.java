package global.recon.service.config;

import feign.Logger;
import feign.Request;
import feign.codec.ErrorDecoder;
import global.recon.service.service.LlmDiscoveryException;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

public class FeignConfig {

    @Bean
    public Request.Options llmRequestOptions() {
        return new Request.Options(5, TimeUnit.SECONDS, 30, TimeUnit.SECONDS, true);
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public ErrorDecoder llmErrorDecoder() {
        return (methodKey, response) -> new LlmDiscoveryException(
                "LLM API error: HTTP " + response.status() + " calling " + methodKey);
    }
}
