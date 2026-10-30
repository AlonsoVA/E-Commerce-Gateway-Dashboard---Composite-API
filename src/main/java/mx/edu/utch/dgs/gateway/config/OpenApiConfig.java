package mx.edu.utch.dgs.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI ecommerceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("E-Commerce Gateway")
                .version("v1")
                .description("Catálogo e inventario propios, tipo de cambio Frankfurter y API compuesta del detalle de producto."));
    }
}
