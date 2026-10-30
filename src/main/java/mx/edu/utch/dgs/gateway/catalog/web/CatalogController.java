package mx.edu.utch.dgs.gateway.catalog.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import mx.edu.utch.dgs.gateway.catalog.api.ProductResponse;
import mx.edu.utch.dgs.gateway.catalog.service.CatalogService;
import mx.edu.utch.dgs.gateway.common.ProductIds;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@Tag(name = "Catálogo", description = "Microservicio propio. Información general del producto y precio base en USD.")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "Listar el catálogo")
    @GetMapping("/microservices/products")
    public List<ProductResponse> list() {
        return catalogService.list();
    }

    @Operation(summary = "Consultar un producto")
    @GetMapping("/microservices/products/{id}")
    public ProductResponse get(
            @Parameter(description = "Identificador del producto", example = "P-101")
            @PathVariable @Pattern(regexp = ProductIds.PATTERN, message = ProductIds.MESSAGE) String id
    ) {
        return catalogService.getById(id);
    }
}
