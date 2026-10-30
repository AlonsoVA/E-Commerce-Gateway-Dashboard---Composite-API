const usd = new Intl.NumberFormat("es-MX", { style: "currency", currency: "USD" });
const mxn = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" });
const rateFormat = new Intl.NumberFormat("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 4 });

const STOCK = {
    IN_STOCK: ["En existencia", "ok"],
    LOW_STOCK: ["Pocas unidades", "low"],
    OUT_OF_STOCK: ["Agotado", "out"],
    UNAVAILABLE: ["Sin dato de inventario", "out"]
};

const SERVICE = {
    catalog: "Catálogo",
    inventory: "Inventario",
    currency: "Divisas"
};

const catalogEl = document.querySelector("#catalog");
const detailEl = document.querySelector("#detail");
const measureButton = document.querySelector("#measure");
const measureNote = document.querySelector("#measure-note");
const benchResults = document.querySelector("#bench-results");

let selectedId = null;
let loading = false;

function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text != null) node.textContent = text;
    return node;
}

async function getJson(url) {
    const response = await fetch(url);
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
        const error = new Error(body.message || "No se pudo completar la petición");
        error.status = response.status;
        throw error;
    }
    return body;
}

function renderEmpty(message) {
    detailEl.replaceChildren(
        el("p", "kicker", "API compuesta"),
        el("h2", null, "Elige un producto"),
        el("p", "muted", message || "El detalle se arma con una sola llamada a /api/v1/product-summary/{id}.")
    );
}

function renderDetail(summary) {
    const stock = STOCK[summary.stock_status] || [summary.stock_status, "out"];
    const root = document.createDocumentFragment();

    if (summary.status !== "SUCCESS") {
        const banner = el("div", "banner");
        const text = summary.errors && summary.errors.length
            ? summary.errors.join(" ")
            : "La respuesta llegó incompleta.";
        banner.append(el("p", null, text));
        root.append(banner);
    }

    root.append(el("p", "kicker", "GET /api/v1/product-summary/" + summary.product_id));
    root.append(el("h2", null, summary.title));
    root.append(el("p", "muted", summary.description));

    const pill = el("span", "pill " + stock[1], stock[0]);
    root.append(pill);

    const prices = el("div", "prices");
    prices.append(priceCard("Precio USD", summary.price_usd == null ? "—" : usd.format(summary.price_usd)));
    prices.append(priceCard("Precio MXN", summary.price_mxn == null ? "—" : mxn.format(summary.price_mxn)));
    root.append(prices);

    if (summary.exchange_rate != null) {
        root.append(el(
            "p",
            null,
            "1 USD = " + rateFormat.format(summary.exchange_rate) + " MXN · " + summary.currency_conversion_source
        ));
    }

    const facts = el("dl", "facts");
    addFact(facts, "Categoría", summary.category);
    addFact(facts, "Unidades", summary.available_units == null ? "—" : String(summary.available_units));
    addFact(facts, "Almacén", summary.warehouse_location || "—");
    addFact(facts, "Entrega", summary.estimated_delivery || "—");
    root.append(facts);

    if (summary.orchestration) {
        root.append(renderTraces(summary.orchestration));
    }

    detailEl.replaceChildren(root);
}

function priceCard(label, value) {
    const card = el("div", "price-card");
    card.append(el("span", null, label));
    card.append(el("strong", null, value));
    return card;
}

function addFact(list, label, value) {
    const wrap = el("div");
    wrap.append(el("dt", null, label));
    wrap.append(el("dd", null, value));
    list.append(wrap);
}

function renderTraces(orchestration) {
    const box = el("div", "trace-list");
    box.append(el("p", "kicker", "Orquestación " + orchestration.strategy.toLowerCase() + " · " + orchestration.elapsed_ms + " ms"));
    const max = Math.max(1, ...orchestration.calls.map((call) => call.elapsed_ms));
    orchestration.calls.forEach((call) => {
        const row = el("div", "trace");
        row.append(el("span", null, SERVICE[call.service] || call.service));
        const track = el("div", "trace-track");
        const bar = el("div", "trace-bar" + (call.ok ? "" : " fail"));
        bar.style.width = Math.max(8, (call.elapsed_ms / max) * 100) + "%";
        track.append(bar);
        row.append(track);
        row.append(el("span", null, call.ok ? call.elapsed_ms + " ms" : "falló"));
        box.append(row);
    });
    return box;
}

function renderCatalog(products) {
    catalogEl.replaceChildren();
    products.forEach((product) => {
        const button = el("button", "product");
        button.type = "button";
        button.setAttribute("aria-pressed", "false");
        const idRow = el("span", "product-id");
        idRow.append(el("span", null, product.product_id));
        idRow.append(el("span", null, product.category));
        button.append(idRow);
        button.append(el("strong", null, product.title));
        button.append(el("small", null, usd.format(product.price_usd)));
        button.addEventListener("click", () => selectProduct(product.product_id, button));
        catalogEl.append(button);
    });
}

async function selectProduct(id, button) {
    if (loading) return;
    selectedId = id;
    measureButton.disabled = false;
    measureNote.textContent = "Producto " + id + " listo para la comparación.";
    document.querySelectorAll(".product").forEach((node) => node.setAttribute("aria-pressed", "false"));
    button.setAttribute("aria-pressed", "true");
    detailEl.replaceChildren(el("p", "kicker", "Consultando"), el("h2", null, "Armando el resumen…"));
    if (window.matchMedia("(max-width: 860px)").matches) {
        detailEl.scrollIntoView({ behavior: "smooth", block: "start" });
    }
    try {
        const summary = await getJson("/api/v1/product-summary/" + encodeURIComponent(id));
        renderDetail(summary);
    } catch (error) {
        renderEmpty(error.message);
    }
}

async function timed(work) {
    const start = performance.now();
    const value = await work();
    return { value, ms: performance.now() - start };
}

function metric(title, ms, copy, chosen) {
    const card = el("article", "metric" + (chosen ? " chosen" : ""));
    card.append(el("span", null, title));
    card.append(el("strong", null, Math.round(ms) + " ms"));
    card.append(el("p", null, copy));
    return card;
}

async function measure() {
    if (!selectedId || loading) return;
    loading = true;
    measureButton.disabled = true;
    measureNote.textContent = "Midiendo…";
    benchResults.replaceChildren();
    const id = selectedId;
    try {
        const sequential = await timed(async () => {
            await getJson("/microservices/products/" + id);
            await getJson("/microservices/inventory/" + id);
            await getJson("/microservices/currency/usd-mxn");
        });
        const parallel = await timed(() => Promise.all([
            getJson("/microservices/products/" + id),
            getJson("/microservices/inventory/" + id),
            getJson("/microservices/currency/usd-mxn")
        ]));
        const composite = await timed(() => getJson("/api/v1/product-summary/" + id));
        benchResults.replaceChildren(
            metric("Tres llamadas en serie", sequential.ms, "El navegador espera a catálogo, luego inventario y luego divisas.", false),
            metric("Tres llamadas en paralelo", parallel.ms, "Llegan juntas, pero el cliente sigue acoplado a tres contratos.", false),
            metric("Una llamada compuesta", composite.ms, "El servidor orquesta las tres fuentes y el cliente recibe un solo contrato.", true)
        );
        renderDetail(composite.value);
        measureNote.textContent = "Cifras de esta ejecución en el navegador. La espera externa depende de Frankfurter y de la red.";
    } catch (error) {
        measureNote.textContent = error.message;
    } finally {
        loading = false;
        measureButton.disabled = selectedId == null;
    }
}

async function init() {
    renderEmpty();
    try {
        const products = await getJson("/microservices/products");
        renderCatalog(products);
        const requested = new URLSearchParams(location.search).get("producto");
        if (!requested) return;
        const button = [...document.querySelectorAll(".product")].find((node) => {
            const id = node.querySelector(".product-id span");
            return id && id.textContent === requested.toUpperCase();
        });
        if (button) await selectProduct(requested.toUpperCase(), button);
    } catch (error) {
        catalogEl.append(el("p", "muted", error.message));
    }
}

measureButton.addEventListener("click", measure);
init();
