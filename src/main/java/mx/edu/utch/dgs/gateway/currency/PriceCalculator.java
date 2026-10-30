package mx.edu.utch.dgs.gateway.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceCalculator {

    private PriceCalculator() {
    }

    public static BigDecimal toMxn(BigDecimal priceUsd, BigDecimal usdToMxn) {
        return priceUsd.multiply(usdToMxn).setScale(2, RoundingMode.HALF_UP);
    }
}
