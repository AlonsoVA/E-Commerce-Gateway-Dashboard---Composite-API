package mx.edu.utch.dgs.gateway.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FrankfurterResponse(
        BigDecimal amount,
        String base,
        String date,
        Map<String, BigDecimal> rates
) {
}
