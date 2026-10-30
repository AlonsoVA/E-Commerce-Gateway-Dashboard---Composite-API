package mx.edu.utch.dgs.gateway.inventory.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import mx.edu.utch.dgs.gateway.common.ProductIds;
import mx.edu.utch.dgs.gateway.inventory.api.InventoryResponse;
import mx.edu.utch.dgs.gateway.inventory.service.InventoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Inventario", description = "Microservicio propio. Existencias, almacén y tiempo estimado de entrega.")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "Consultar el inventario de un producto")
    @GetMapping("/microservices/inventory/{productId}")
    public InventoryResponse get(
            @Parameter(description = "Identificador del producto", example = "P-101")
            @PathVariable @Pattern(regexp = ProductIds.PATTERN, message = ProductIds.MESSAGE) String productId
    ) {
        return inventoryService.getByProductId(productId);
    }
}
