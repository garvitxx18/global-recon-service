package global.recon.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI globalReconOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Global Recon Service")
                .version("v1")
                .description("Upload datasets, discover mappings with Gemini, approve a recon plan, and run deterministic Java reconciliation."));
    }
}
