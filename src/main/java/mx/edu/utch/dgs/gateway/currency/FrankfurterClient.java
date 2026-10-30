package mx.edu.utch.dgs.gateway.currency;

import mx.edu.utch.dgs.gateway.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Component
public class FrankfurterClient {

    private static final Logger log = LoggerFactory.getLogger(FrankfurterClient.class);

    private final RestClient restClient;
    private final AppProperties properties;
    private final Object lock = new Object();
    private volatile CachedRate cache;

    public FrankfurterClient(RestClient restClient, AppProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public ExchangeQuote latestUsdToMxn() {
        CachedRate fresh = readFresh();
        if (fresh != null) {
            return fresh.quote().withCached(true);
        }
        synchronized (lock) {
            fresh = readFresh();
            if (fresh != null) {
                return fresh.quote().withCached(true);
            }
            try {
                ExchangeQuote quote = fetch();
                cache = new CachedRate(quote, Instant.now());
                return quote;
            } catch (RuntimeException exception) {
                if (cache != null) {
                    log.warn("Frankfurter no disponible. Se usa la última tasa conocida: {}", exception.getMessage());
                    return cache.quote().markCached();
                }
                throw exception;
            }
        }
    }

    public void clearCache() {
        cache = null;
    }

    private ExchangeQuote fetch() {
        String baseUrl = properties.currency().baseUrl().replaceAll("/$", "");
        try {
            FrankfurterResponse body = restClient.get()
                    .uri(baseUrl + "/latest?from=USD&to=MXN")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.warn("Frankfurter respondió HTTP {}", response.getStatusCode().value());
                        throw new CurrencyUnavailableException("No fue posible obtener el tipo de cambio USD/MXN");
                    })
                    .body(FrankfurterResponse.class);
            if (body == null || body.rates() == null || body.rates().get("MXN") == null) {
                throw new CurrencyUnavailableException("La respuesta de Frankfurter no incluye la tasa MXN");
            }
            BigDecimal rate = body.rates().get("MXN");
            return new ExchangeQuote(rate, body.date(), CurrencySources.FRANKFURTER, false);
        } catch (CurrencyUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new CurrencyUnavailableException("No fue posible obtener el tipo de cambio USD/MXN", exception);
        }
    }

    private CachedRate readFresh() {
        CachedRate current = cache;
        if (current == null) {
            return null;
        }
        Duration ttl = properties.currency().cacheTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return null;
        }
        if (Instant.now().isBefore(current.storedAt().plus(ttl))) {
            return current;
        }
        return null;
    }

    private record CachedRate(ExchangeQuote quote, Instant storedAt) {
    }
}
