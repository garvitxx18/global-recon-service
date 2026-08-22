package global.recon.service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ReconProperties.class, LlmProperties.class})
public class ApplicationConfig {
}
