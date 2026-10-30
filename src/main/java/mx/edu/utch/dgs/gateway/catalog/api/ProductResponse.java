package mx.edu.utch.dgs.gateway.catalog.api;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import mx.edu.utch.dgs.gateway.common.MoneySerializer;

import java.math.BigDecimal;

public record ProductResponse(
        String productId,
        String title,
        String description,
        String category,
        @JsonSerialize(using = MoneySerializer.class) BigDecimal priceUsd
) {
}
