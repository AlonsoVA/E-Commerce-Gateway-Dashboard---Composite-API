package mx.edu.utch.dgs.gateway.currency;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PriceCalculatorTest {

    @Test
    void multipliesAndRoundsHalfUpToCents() {
        assertThat(PriceCalculator.toMxn(new BigDecimal("1200.00"), new BigDecimal("17.00")))
                .isEqualByComparingTo("20400.00");
        assertThat(PriceCalculator.toMxn(new BigDecimal("10.00"), new BigDecimal("1.005")))
                .isEqualByComparingTo("10.05");
        assertThat(PriceCalculator.toMxn(new BigDecimal("10.00"), new BigDecimal("1.004")))
                .isEqualByComparingTo("10.04");
    }
}
