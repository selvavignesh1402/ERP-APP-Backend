package com.riceerp.backend.repository;

import com.riceerp.backend.entity.*;
import com.riceerp.backend.dto.*;
import com.riceerp.backend.security.TenantIdentifierResolver;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class SupplierInvoiceDecimalPersistenceTest {
    @Test void invoicePricesAndTotalsSurviveReloadAndNumericJson() throws Exception {
        var config = new Configuration().addAnnotatedClass(SupplierInvoice.class).addAnnotatedClass(SupplierInvoiceItem.class)
                .addAnnotatedClass(Supplier.class).addAnnotatedClass(Purchase.class).addAnnotatedClass(Product.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:supplier_invoice_decimal;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        var exact = new BigDecimal("123456789012345.6789");
        try (var factory = config.buildSessionFactory()) {
            Long invoiceId, lineId;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var tx = session.beginTransaction();
                var supplier = new Supplier(); supplier.setSupplierName("Supplier"); supplier.setPhone("123"); session.persist(supplier);
                var product = new Product(); product.setProductName("Product"); session.persist(product);
                var invoice = new SupplierInvoice(); invoice.setSupplier(supplier); invoice.setInvoiceNumber("SI-DECIMAL");
                invoice.setTotalAmount(exact); session.persist(invoice);
                var item = new SupplierInvoiceItem(); item.setInvoice(invoice); item.setProduct(product); item.setQuantity(1);
                item.setUnitPrice(exact); item.setTotalAmount(exact); session.persist(item);
                tx.commit(); invoiceId = invoice.getId(); lineId = item.getId();
            }
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var invoice = session.find(SupplierInvoice.class, invoiceId);
                var item = session.find(SupplierInvoiceItem.class, lineId);
                assertEquals(exact, invoice.getTotalAmount()); assertEquals(exact, item.getUnitPrice()); assertEquals(exact, item.getTotalAmount());
                var mapper = new ObjectMapper().registerModule(new JavaTimeModule()).enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
                var header = mapper.readTree(mapper.writeValueAsString(SupplierInvoiceResponse.from(invoice)));
                var line = mapper.readTree(mapper.writeValueAsString(SupplierInvoiceItemResponse.from(item)));
                assertTrue(header.get("totalAmount").isNumber()); assertEquals(exact, header.get("totalAmount").decimalValue());
                assertTrue(line.get("unitPrice").isNumber()); assertEquals(exact, line.get("unitPrice").decimalValue());
                assertTrue(line.get("totalAmount").isNumber()); assertEquals(exact, line.get("totalAmount").decimalValue());
            }
        }
    }
}
