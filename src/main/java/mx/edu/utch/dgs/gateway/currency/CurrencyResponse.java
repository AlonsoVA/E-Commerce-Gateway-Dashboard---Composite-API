package mx.edu.utch.dgs.gateway.currency;

import java.math.BigDecimal;

public record CurrencyResponse(
        String base,
        String quote,
        BigDecimal rate,
        String date,
        String source,
        boolean cached
) {
}
