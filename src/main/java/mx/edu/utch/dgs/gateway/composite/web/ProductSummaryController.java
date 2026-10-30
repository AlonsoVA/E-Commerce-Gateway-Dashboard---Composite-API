package mx.edu.utch.dgs.gateway.composite.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import mx.edu.utch.dgs.gateway.common.ProductIds;
import mx.edu.utch.dgs.gateway.composite.dto.ProductSummaryResponse;
import mx.edu.utch.dgs.gateway.composite.service.ProductSummaryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Callable;

@RestController
@Validated
@Tag(name = "Composite API", description = "Orquesta catálogo, inventario y Frankfurter en un solo contrato JSON.")
public class ProductSummaryController {

    private final ProductSummaryService productSummaryService;

    public ProductSummaryController(ProductSummaryService productSummaryService) {
        this.productSummaryService = productSummaryService;
    }

    @Operation(summary = "Resumen unificado de un producto")
    @GetMapping({"/api/v1/product-summary/{id}", "/api/v1/products/{id}/summary"})
    public Callable<ProductSummaryResponse> summary(
            @Parameter(description = "Identificador del producto", example = "P-101")
            @PathVariable @Pattern(regexp = ProductIds.PATTERN, message = ProductIds.MESSAGE) String id
    ) {
        String productId = ProductIds.normalize(id);
        // Callable libera el hilo de Tomcat antes de esperar. Las peticiones internas
        // a catálogo e inventario necesitan un hilo del servidor para poder responder.
        return () -> productSummaryService.summarize(productId);
    }
}
