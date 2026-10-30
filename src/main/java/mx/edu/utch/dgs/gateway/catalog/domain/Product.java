package mx.edu.utch.dgs.gateway.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "products", schema = "catalog")
public class Product {

    @Id
    @Column(length = 16)
    private String id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 600)
    private String description;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(name = "price_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceUsd;

    protected Product() {
    }

    public Product(String id, String title, String description, String category, BigDecimal priceUsd) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priceUsd = priceUsd;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPriceUsd() {
        return priceUsd;
    }
}
