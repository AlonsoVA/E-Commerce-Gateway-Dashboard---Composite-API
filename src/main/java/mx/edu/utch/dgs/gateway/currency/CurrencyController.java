package mx.edu.utch.dgs.gateway.currency;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Divisas", description = "Consulta de apoyo sobre Frankfurter. La API compuesta llama al proveedor directamente.")
public class CurrencyController {

    private final FrankfurterClient frankfurterClient;

    public CurrencyController(FrankfurterClient frankfurterClient) {
        this.frankfurterClient = frankfurterClient;
    }

    @Operation(summary = "Tipo de cambio USD a MXN")
    @GetMapping("/microservices/currency/usd-mxn")
    public CurrencyResponse usdToMxn() {
        ExchangeQuote quote = frankfurterClient.latestUsdToMxn();
        return new CurrencyResponse("USD", "MXN", quote.rate(), quote.date(), quote.source(), quote.cached());
    }
}
