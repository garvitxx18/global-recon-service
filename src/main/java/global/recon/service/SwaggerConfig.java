package global.recon.service;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI globalReconOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Global Recon Service")
                        .version("v1")
                        .description("Upload datasets, discover mappings, approve a recon plan, and run deterministic Java reconciliation. Click Authorize and enter your company email before calling any endpoint."))
                .components(new Components().addSecuritySchemes("X-User-Email", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-User-Email")
                        .description("Company email. All data is stored and filtered by this value.")))
                .addSecurityItem(new SecurityRequirement().addList("X-User-Email"));
    }
}
