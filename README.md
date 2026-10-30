# E-Commerce Gateway

API compuesta para el detalle de un producto. El cliente hace una sola petición y el servidor consulta, en paralelo, el catálogo, el inventario y el tipo de cambio USD/MXN.

Asignatura: Desarrollo Web Integral, Unidad 3. Universidad Tecnológica de Chihuahua.

## Qué expone

| Pieza | Método y ruta | Origen |
| --- | --- | --- |
| Catálogo | `GET /microservices/products/{id}` | Propia, PostgreSQL (`catalog.products`) |
| Inventario | `GET /microservices/inventory/{productId}` | Propia, PostgreSQL (`inventory.stock`) |
| Divisas (apoyo) | `GET /microservices/currency/usd-mxn` | Frankfurter, para pruebas y para el tablero |
| API compuesta | `GET /api/v1/product-summary/{id}` | Orquesta las tres fuentes |
| Alias | `GET /api/v1/products/{id}/summary` | La misma operación |
| Tablero | `GET /` | Consume solo el resumen para pintar el detalle |
| Swagger | `GET /swagger-ui/index.html` | Pruebas desde el navegador |

La API compuesta llama a Frankfurter directo (`https://api.frankfurter.app/latest?from=USD&to=MXN`). El endpoint de divisas existe para poder probar esa integración en Swagger y para la comparación de latencia del tablero. No es un cuarto microservicio de negocio.

Productos de ejemplo: `P-101` a `P-106`. `P-101` es Laptop Gamer XYZ, 1200.00 USD, 15 unidades en CEDIS Chihuahua.

## Requisitos

- Java 21
- Maven 3.9 o el wrapper `./mvnw` (si se generó)
- PostgreSQL 15

## Base de datos local

Con el servicio de PostgreSQL ya corriendo en el puerto 5432:

```bash
psql -h 127.0.0.1 -d postgres -f scripts/init-db.sql
```

Eso crea el rol `ecommerce` / `ecommerce`, la base `ecommerce_gateway` y los esquemas `catalog` e `inventory`. Hibernate crea las tablas al arrancar y, si están vacías, carga el catálogo de ejemplo.

Para arrancar la aplicación:

```bash
mvn spring-boot:run
```

O, si PostgreSQL local ya responde:

```bash
./scripts/run.sh
```

## PostgreSQL con Docker

Si el puerto 5432 está libre y prefieres no instalar PostgreSQL, cambia el mapeo del `docker-compose.yml` a `"5432:5432"`. Este proyecto lo publica en **5433** para no chocar con un Postgres que ya esté en 5432.

```bash
docker compose up -d
DB_URL=jdbc:postgresql://127.0.0.1:5433/ecommerce_gateway mvn spring-boot:run
```

En ese caso la imagen ya crea el usuario, la contraseña y la base. Hibernate crea los esquemas.

## Variables

| Variable | Default |
| --- | --- |
| `PORT` | `8080` |
| `DB_URL` | `jdbc:postgresql://127.0.0.1:5432/ecommerce_gateway` |
| `DB_USERNAME` | `ecommerce` |
| `DB_PASSWORD` | `ecommerce` |
| `CURRENCY_BASE_URL` | `https://api.frankfurter.app` |

Frankfurter responde 301 y manda la petición a `https://api.frankfurter.dev/v1`. El cliente HTTP sigue esa redirección. La URL configurada sigue siendo la del planteamiento.

La contraseña del ejemplo es solo para la práctica local.

## Probar

Tablero: [http://localhost:8080](http://localhost:8080)

Detalle directo de la laptop: [http://localhost:8080/?producto=P-101](http://localhost:8080/?producto=P-101)

Swagger: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

```bash
curl -s http://localhost:8080/microservices/products/P-101
curl -s http://localhost:8080/microservices/inventory/P-101
curl -s http://localhost:8080/microservices/currency/usd-mxn
curl -s http://localhost:8080/api/v1/product-summary/P-101
curl -s "https://api.frankfurter.app/latest?from=USD&to=MXN"
```

Contrato mínimo del resumen:

```json
{
  "product_id": "P-101",
  "title": "Laptop Gamer XYZ",
  "price_usd": 1200.00,
  "price_mxn": 20400.00,
  "stock_status": "IN_STOCK",
  "available_units": 15,
  "currency_conversion_source": "Frankfurter External API",
  "status": "SUCCESS"
}
```

`price_mxn` del ejemplo sale con una tasa de 17.00. En ejecución la tasa es la del día que entregue Frankfurter, así que el monto en MXN cambia. La respuesta también trae descripción, almacén, entrega, la tasa y el tiempo de cada llamada (`orchestration`).

Si Frankfurter no responde y no hay una tasa reciente en memoria, el resumen sigue incluyendo producto e inventario con `"status": "PARTIAL"`. Si el producto no existe, la respuesta es 404.

## Pruebas automáticas

```bash
mvn test
```

Usan PostgreSQL local y un sustituto de Frankfurter. No hace falta internet para la suite.

## Estructura

```
src/main/java/mx/edu/utch/dgs/gateway
  catalog/       microservicio de productos
  inventory/     microservicio de almacén
  currency/      cliente HTTP de Frankfurter y conversión a MXN
  composite/     orquestación con CompletableFuture
  bootstrap/     datos de ejemplo
src/main/resources/static   tablero
```

Catálogo e inventario no comparten llave foránea. El `product_id` es el dato con el que la API compuesta une las dos respuestas. En un despliegue real cada uno tendría su propia base; aquí viven en esquemas distintos de la misma instancia para poder correr la evidencia con un solo proceso.

## Ramas

El trabajo de la práctica está en `feature/composite-api` y se integra a `main`.
