package com.riceerp.backend.repository;

import com.riceerp.backend.entity.*;
import com.riceerp.backend.dto.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.security.TenantIdentifierResolver;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ProductPricePersistenceTest {
    @Test void catalogAndPriceHistoryRetainFourDecimalPlacesAfterReloadAndSerialization() throws Exception {
        var config = new Configuration().addAnnotatedClass(Product.class).addAnnotatedClass(PriceHistory.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:catalog_price_decimals;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        var purchase = new BigDecimal("123456789012345.6788");
        var selling = new BigDecimal("123456789012345.6789");
        try (var factory = config.buildSessionFactory()) {
            Long productId, historyId;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var tx = session.beginTransaction(); var product = new Product();
                product.setProductName("Exact price"); product.setStatus(Status.ACTIVE);
                product.setPurchasePrice(purchase); product.setSellingPrice(selling); session.persist(product);
                var history = new PriceHistory(product, PriceType.SELLING, selling); session.persist(history);
                tx.commit(); productId = product.getId(); historyId = history.getId();
            }
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var product = session.find(Product.class, productId); var history = session.find(PriceHistory.class, historyId);
                assertEquals(purchase, product.getPurchasePrice()); assertEquals(selling, product.getSellingPrice());
                assertEquals(selling, history.getPrice());
                var json = new ObjectMapper().registerModule(new JavaTimeModule()).enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
                var catalog = json.readTree(json.writeValueAsString(ProductResponse.from(product)));
                var priceLog = json.readTree(json.writeValueAsString(PriceHistoryResponse.from(history)));
                assertTrue(catalog.get("purchasePrice").isNumber()); assertTrue(catalog.get("sellingPrice").isNumber());
                assertTrue(priceLog.get("price").isNumber());
                assertEquals(purchase, catalog.get("purchasePrice").decimalValue());
                assertEquals(selling, catalog.get("sellingPrice").decimalValue());
                assertEquals(selling, priceLog.get("price").decimalValue());
            }
        }
    }
}
