package mx.edu.utch.dgs.gateway.inventory.service;

import mx.edu.utch.dgs.gateway.common.ProductIds;
import mx.edu.utch.dgs.gateway.inventory.api.InventoryResponse;
import mx.edu.utch.dgs.gateway.inventory.domain.StockItem;
import mx.edu.utch.dgs.gateway.inventory.repository.StockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {

    static final int LOW_STOCK_MAX = 10;

    private final StockRepository stock;

    public InventoryService(StockRepository stock) {
        this.stock = stock;
    }

    public InventoryResponse getByProductId(String id) {
        String productId = ProductIds.normalize(id);
        return stock.findByProductId(productId)
                .map(InventoryService::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No hay registro de inventario para el producto " + productId
                ));
    }

    static String stockStatus(int availableUnits) {
        if (availableUnits <= 0) {
            return "OUT_OF_STOCK";
        }
        if (availableUnits <= LOW_STOCK_MAX) {
            return "LOW_STOCK";
        }
        return "IN_STOCK";
    }

    static String estimatedDelivery(int hours) {
        if (hours <= 24) {
            return "24 horas";
        }
        int days = (hours + 23) / 24;
        return days == 1 ? "1 día hábil" : days + " días hábiles";
    }

    private static InventoryResponse toResponse(StockItem item) {
        return new InventoryResponse(
                item.getProductId(),
                item.getAvailableUnits(),
                stockStatus(item.getAvailableUnits()),
                item.getWarehouseLocation(),
                estimatedDelivery(item.getEstimatedDeliveryHours())
        );
    }
}
