package mx.edu.utch.dgs.gateway.composite.service;

import mx.edu.utch.dgs.gateway.catalog.api.ProductResponse;
import mx.edu.utch.dgs.gateway.common.ResourceNotFoundException;
import mx.edu.utch.dgs.gateway.composite.client.CatalogHttpClient;
import mx.edu.utch.dgs.gateway.composite.client.InventoryHttpClient;
import mx.edu.utch.dgs.gateway.composite.dto.Orchestration;
import mx.edu.utch.dgs.gateway.composite.dto.ProductSummaryResponse;
import mx.edu.utch.dgs.gateway.currency.CurrencySources;
import mx.edu.utch.dgs.gateway.currency.CurrencyUnavailableException;
import mx.edu.utch.dgs.gateway.currency.ExchangeQuote;
import mx.edu.utch.dgs.gateway.currency.FrankfurterClient;
import mx.edu.utch.dgs.gateway.currency.PriceCalculator;
import mx.edu.utch.dgs.gateway.inventory.api.InventoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Consulta catálogo, inventario y divisas al mismo tiempo y arma un solo contrato.
 * Si el producto no existe, responde 404. Si inventario o divisas fallan, responde 200 con estado PARTIAL.
 */
@Service
public class ProductSummaryService {

    private static final Logger log = LoggerFactory.getLogger(ProductSummaryService.class);

    private final CatalogHttpClient catalogHttpClient;
    private final InventoryHttpClient inventoryHttpClient;
    private final FrankfurterClient frankfurterClient;
    private final ExecutorService upstreamExecutor;

    public ProductSummaryService(
            CatalogHttpClient catalogHttpClient,
            InventoryHttpClient inventoryHttpClient,
            FrankfurterClient frankfurterClient,
            @Qualifier("upstreamExecutor") ExecutorService upstreamExecutor
    ) {
        this.catalogHttpClient = catalogHttpClient;
        this.inventoryHttpClient = inventoryHttpClient;
        this.frankfurterClient = frankfurterClient;
        this.upstreamExecutor = upstreamExecutor;
    }

    public ProductSummaryResponse summarize(String productId) {
        long start = System.nanoTime();
        CompletableFuture<CallOutcome<ProductResponse>> productCall =
                dispatch("catalog", () -> catalogHttpClient.getById(productId));
        CompletableFuture<CallOutcome<InventoryResponse>> inventoryCall =
                dispatch("inventory", () -> inventoryHttpClient.getByProductId(productId));
        CompletableFuture<CallOutcome<ExchangeQuote>> currencyCall =
                dispatch("currency", frankfurterClient::latestUsdToMxn);

        CompletableFuture.allOf(productCall, inventoryCall, currencyCall).join();

        CallOutcome<ProductResponse> product = productCall.join();
        CallOutcome<InventoryResponse> inventory = inventoryCall.join();
        CallOutcome<ExchangeQuote> currency = currencyCall.join();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        if (!product.ok()) {
            if (isCatalogNotFound(product.error())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el producto " + productId);
            }
            log.error("Catálogo no disponible para {}", productId, product.error());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El catálogo de productos no está disponible");
        }

        ProductResponse item = product.body();
        List<String> errors = new ArrayList<>();

        String stockStatus = "UNAVAILABLE";
        Integer availableUnits = null;
        String warehouse = null;
        String delivery = null;
        if (inventory.ok()) {
            InventoryResponse stock = inventory.body();
            stockStatus = stock.stockStatus();
            availableUnits = stock.availableUnits();
            warehouse = stock.warehouseLocation();
            delivery = stock.estimatedDelivery();
        } else {
            errors.add(messageOf(inventory.error(), "No fue posible consultar el inventario"));
        }

        BigDecimal priceMxn = null;
        BigDecimal rate = null;
        String rateDate = null;
        String source = CurrencySources.FRANKFURTER;
        if (currency.ok()) {
            ExchangeQuote quote = currency.body();
            rate = quote.rate();
            rateDate = quote.date();
            source = quote.source();
            priceMxn = PriceCalculator.toMxn(item.priceUsd(), rate);
        } else {
            errors.add(messageOf(currency.error(), "No fue posible obtener el tipo de cambio USD/MXN"));
        }

        String status = errors.isEmpty() ? "SUCCESS" : "PARTIAL";
        log.info("Resumen de {} en {} ms con estado {}", productId, elapsedMs, status);

        return new ProductSummaryResponse(
                item.productId(),
                item.title(),
                item.description(),
                item.category(),
                item.priceUsd(),
                priceMxn,
                rate,
                rateDate,
                stockStatus,
                availableUnits,
                warehouse,
                delivery,
                source,
                status,
                errors.isEmpty() ? null : List.copyOf(errors),
                new Orchestration("PARALLEL", elapsedMs, List.of(product.trace(), inventory.trace(), currency.trace()))
        );
    }

    private <T> CompletableFuture<CallOutcome<T>> dispatch(String service, Supplier<T> call) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            try {
                return CallOutcome.success(service, call.get(), elapsedMs(start));
            } catch (Exception exception) {
                log.warn("La consulta a {} falló en {} ms: {}", service, elapsedMs(start), exception.getMessage());
                return CallOutcome.failure(service, exception, elapsedMs(start));
            }
        }, upstreamExecutor);
    }

    private static long elapsedMs(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private static boolean isCatalogNotFound(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ResourceNotFoundException notFound && "catalog".equals(notFound.getService())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String messageOf(Throwable error, String fallback) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ResourceNotFoundException || current instanceof CurrencyUnavailableException) {
                if (current.getMessage() != null && !current.getMessage().isBlank()) {
                    return current.getMessage();
                }
            }
            current = current.getCause();
        }
        return fallback;
    }
}
