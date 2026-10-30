package mx.edu.utch.dgs.gateway.bootstrap;

import mx.edu.utch.dgs.gateway.catalog.domain.Product;
import mx.edu.utch.dgs.gateway.catalog.repository.ProductRepository;
import mx.edu.utch.dgs.gateway.config.InternalBaseUrl;
import mx.edu.utch.dgs.gateway.inventory.domain.StockItem;
import mx.edu.utch.dgs.gateway.inventory.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SampleDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataLoader.class);

    private final ProductRepository products;
    private final StockRepository stock;
    private final InternalBaseUrl baseUrl;

    public SampleDataLoader(ProductRepository products, StockRepository stock, InternalBaseUrl baseUrl) {
        this.products = products;
        this.stock = stock;
        this.baseUrl = baseUrl;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (products.count() == 0) {
            products.saveAll(catalog());
            stock.saveAll(warehouses());
            log.info("Se cargaron {} productos de ejemplo", products.count());
        } else {
            log.info("El catálogo ya tiene datos. No se vuelve a cargar.");
        }
        log.info("Tablero: {}/", baseUrl.get());
        log.info("Swagger: {}/swagger-ui/index.html", baseUrl.get());
        log.info("Resumen de ejemplo: {}/api/v1/product-summary/P-101", baseUrl.get());
    }

    private List<Product> catalog() {
        return List.of(
                new Product("P-101", "Laptop Gamer XYZ", "Equipo para juego y edición, con gráficos dedicados y teclado retroiluminado.", "Cómputo", money("1200.00")),
                new Product("P-102", "Audífonos Inalámbricos Pulse", "Cancelación de ruido y 30 horas de batería para el trayecto o la oficina.", "Audio", money("89.90")),
                new Product("P-103", "Monitor UltraSharp 27", "Panel IPS 4K de 27 pulgadas para diseño y trabajo de escritorio.", "Cómputo", money("349.50")),
                new Product("P-104", "Teclado Mecánico Roble", "Interruptores mecánicos, estructura de aluminio y distribución en español.", "Accesorios", money("79.00")),
                new Product("P-105", "Silla Ergonómica Sierra", "Soporte lumbar ajustable para jornadas largas de desarrollo.", "Oficina", money("260.00")),
                new Product("P-106", "SSD NVMe 2 TB", "Unidad de 2 TB para sistema, proyectos y partidas.", "Componentes", money("149.00"))
        );
    }

    private List<StockItem> warehouses() {
        return List.of(
                new StockItem("P-101", 15, "CEDIS Chihuahua", 48),
                new StockItem("P-102", 42, "CEDIS Chihuahua", 24),
                new StockItem("P-103", 8, "Almacén Delicias", 72),
                new StockItem("P-104", 0, "CEDIS Chihuahua", 24),
                new StockItem("P-105", 6, "Almacén Juárez", 96),
                new StockItem("P-106", 53, "CEDIS Chihuahua", 24)
        );
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
