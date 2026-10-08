package com.riceerp.backend.repository;

import com.riceerp.backend.entity.*;
import com.riceerp.backend.security.TenantIdentifierResolver;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class SupplierQuotePersistenceTest {
    @Test void supplierPriceSurvivesReloadWithoutRounding() {
        var config = new Configuration().addAnnotatedClass(Supplier.class).addAnnotatedClass(Product.class)
                .addAnnotatedClass(SupplierProduct.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:supplier_quote_decimal;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        var price = new BigDecimal("123456789012345.6789");
        try (var factory = config.buildSessionFactory()) {
            Long id;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var tx = session.beginTransaction();
                var supplier = new Supplier(); supplier.setSupplierName("Supplier"); supplier.setPhone("100"); session.persist(supplier);
                var product = new Product(); product.setProductName("Product"); session.persist(product);
                var quote = new SupplierProduct(); quote.setSupplier(supplier); quote.setProduct(product); quote.setPurchasePrice(price);
                session.persist(quote); tx.commit(); id = quote.getId();
            }
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                assertEquals(price, session.find(SupplierProduct.class, id).getPurchasePrice());
            }
        }
    }
}
