package mx.edu.utch.dgs.gateway.inventory.api;

public record InventoryResponse(
        String productId,
        int availableUnits,
        String stockStatus,
        String warehouseLocation,
        String estimatedDelivery
) {
}
