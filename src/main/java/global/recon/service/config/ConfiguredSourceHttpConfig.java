package global.recon.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ConfiguredSourceHttpConfig {

    @Bean
    public RestClient configuredSourceRestClient(ReconProperties properties) {
        ReconProperties.Sources sources = properties.getSources();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(Math.max(1, sources.getConnectTimeoutMs())));
        factory.setReadTimeout(Duration.ofMillis(Math.max(1, sources.getReadTimeoutMs())));
        return RestClient.builder().requestFactory(factory).build();
    }
}
