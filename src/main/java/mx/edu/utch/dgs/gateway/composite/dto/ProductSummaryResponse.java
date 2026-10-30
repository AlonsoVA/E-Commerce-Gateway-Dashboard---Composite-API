package mx.edu.utch.dgs.gateway.composite.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import mx.edu.utch.dgs.gateway.common.MoneySerializer;

import java.math.BigDecimal;
import java.util.List;

public record ProductSummaryResponse(
        String productId,
        String title,
        String description,
        String category,
        @JsonSerialize(using = MoneySerializer.class) BigDecimal priceUsd,
        @JsonSerialize(using = MoneySerializer.class) BigDecimal priceMxn,
        BigDecimal exchangeRate,
        String rateDate,
        String stockStatus,
        Integer availableUnits,
        String warehouseLocation,
        String estimatedDelivery,
        String currencyConversionSource,
        String status,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> errors,
        Orchestration orchestration
) {
}
