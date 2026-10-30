package mx.edu.utch.dgs.gateway.currency;

import java.math.BigDecimal;

public record ExchangeQuote(BigDecimal rate, String date, String source, boolean cached) {

    public ExchangeQuote withCached(boolean value) {
        return new ExchangeQuote(rate, date, source, value);
    }

    public ExchangeQuote markCached() {
        return new ExchangeQuote(rate, date, CurrencySources.FRANKFURTER_CACHE, true);
    }
}
