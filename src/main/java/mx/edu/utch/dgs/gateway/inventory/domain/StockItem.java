package mx.edu.utch.dgs.gateway.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock", schema = "inventory")
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, length = 16, unique = true)
    private String productId;

    @Column(name = "available_units", nullable = false)
    private int availableUnits;

    @Column(name = "warehouse_location", nullable = false, length = 80)
    private String warehouseLocation;

    @Column(name = "estimated_delivery_hours", nullable = false)
    private int estimatedDeliveryHours;

    protected StockItem() {
    }

    public StockItem(String productId, int availableUnits, String warehouseLocation, int estimatedDeliveryHours) {
        this.productId = productId;
        this.availableUnits = availableUnits;
        this.warehouseLocation = warehouseLocation;
        this.estimatedDeliveryHours = estimatedDeliveryHours;
    }

    public String getProductId() {
        return productId;
    }

    public int getAvailableUnits() {
        return availableUnits;
    }

    public String getWarehouseLocation() {
        return warehouseLocation;
    }

    public int getEstimatedDeliveryHours() {
        return estimatedDeliveryHours;
    }
}
