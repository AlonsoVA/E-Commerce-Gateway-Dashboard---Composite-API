package mx.edu.utch.dgs.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import mx.edu.utch.dgs.gateway.currency.FrankfurterClient;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class GatewayIntegrationTest {

    private static final MockWebServer FRANKFURTER = new MockWebServer();
    private static final AtomicBoolean CURRENCY_DOWN = new AtomicBoolean(false);

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private FrankfurterClient frankfurterClient;

    @DynamicPropertySource
    static void frankfurter(DynamicPropertyRegistry registry) throws IOException {
        if (FRANKFURTER.getPort() == -1) {
            FRANKFURTER.start();
        }
        registry.add("app.currency.base-url", GatewayIntegrationTest::frankfurterBase);
        registry.add("app.currency.cache-ttl", () -> "0s");
    }

    @BeforeAll
    static void dispatcher() {
        FRANKFURTER.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (CURRENCY_DOWN.get()) {
                    return new MockResponse().setResponseCode(503).setBody("no disponible");
                }
                return new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                                {"amount":1.0,"base":"USD","date":"2026-10-28","rates":{"MXN":17.00}}
                                """);
            }
        });
    }

    @AfterAll
    static void stopFrankfurter() throws IOException {
        FRANKFURTER.shutdown();
    }

    @BeforeEach
    void resetCurrency() {
        CURRENCY_DOWN.set(false);
        frankfurterClient.clearCache();
    }

    @Test
    void catalogReturnsTheSampleLaptop() {
        JsonNode body = ok("/microservices/products/P-101");
        assertThat(body.get("product_id").asText()).isEqualTo("P-101");
        assertThat(body.get("title").asText()).isEqualTo("Laptop Gamer XYZ");
        assertThat(body.get("price_usd").decimalValue()).isEqualByComparingTo("1200.00");
        assertThat(body.has("price_mxn")).isFalse();
    }

    @Test
    void inventoryReportsStockAndWarehouse() {
        JsonNode laptop = ok("/microservices/inventory/P-101");
        assertThat(laptop.get("available_units").asInt()).isEqualTo(15);
        assertThat(laptop.get("stock_status").asText()).isEqualTo("IN_STOCK");
        assertThat(laptop.get("warehouse_location").asText()).isEqualTo("CEDIS Chihuahua");
        assertThat(laptop.get("estimated_delivery").asText()).isEqualTo("2 días hábiles");

        assertThat(ok("/microservices/inventory/P-103").get("stock_status").asText()).isEqualTo("LOW_STOCK");
        assertThat(ok("/microservices/inventory/P-104").get("stock_status").asText()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    void summaryMatchesTheCompositeContract() {
        ResponseEntity<String> raw = http.getForEntity("/api/v1/product-summary/P-101", String.class);
        assertThat(raw.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(raw.getBody()).contains("\"price_usd\":1200.00");
        assertThat(raw.getBody()).contains("\"price_mxn\":20400.00");
        assertThat(raw.getBody()).contains("\"currency_conversion_source\":\"Frankfurter External API\"");
        assertThat(raw.getBody()).contains("\"status\":\"SUCCESS\"");

        JsonNode body = ok("/api/v1/products/p-101/summary");
        assertThat(body.get("product_id").asText()).isEqualTo("P-101");
        assertThat(body.get("title").asText()).isEqualTo("Laptop Gamer XYZ");
        assertThat(body.get("stock_status").asText()).isEqualTo("IN_STOCK");
        assertThat(body.get("available_units").asInt()).isEqualTo(15);
        assertThat(body.get("orchestration").get("strategy").asText()).isEqualTo("PARALLEL");
        assertThat(body.get("orchestration").get("calls")).hasSize(3);
    }

    @Test
    void unknownProductReturns404() {
        ResponseEntity<JsonNode> response = http.getForEntity("/api/v1/product-summary/P-999", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("message").asText()).contains("P-999");
    }

    @Test
    void invalidIdReturns400() {
        ResponseEntity<JsonNode> response = http.getForEntity("/api/v1/product-summary/producto-demasiado", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void currencyFailureDegradesToPartial() {
        CURRENCY_DOWN.set(true);
        JsonNode body = ok("/api/v1/product-summary/P-101");
        assertThat(body.get("status").asText()).isEqualTo("PARTIAL");
        assertThat(body.get("title").asText()).isEqualTo("Laptop Gamer XYZ");
        assertThat(body.get("stock_status").asText()).isEqualTo("IN_STOCK");
        assertThat(body.get("price_mxn").isNull()).isTrue();
        assertThat(body.get("errors").toString()).contains("tipo de cambio");
    }

    @Test
    void staleRateIsUsedWhenFrankfurterFailsAfterASuccessfulCall() {
        ok("/api/v1/product-summary/P-102");
        CURRENCY_DOWN.set(true);
        JsonNode body = ok("/api/v1/product-summary/P-102");
        assertThat(body.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(body.get("currency_conversion_source").asText()).isEqualTo("Frankfurter External API (caché)");
        assertThat(body.get("price_mxn").decimalValue()).isEqualByComparingTo(new BigDecimal("89.90").multiply(new BigDecimal("17.00")));
    }

    private JsonNode ok(String path) {
        ResponseEntity<JsonNode> response = http.getForEntity(path, JsonNode.class);
        assertThat(response.getStatusCode())
                .withFailMessage(response.getBody() == null ? path : response.getBody().toString())
                .isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private static String frankfurterBase() {
        String base = FRANKFURTER.url("").toString();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
