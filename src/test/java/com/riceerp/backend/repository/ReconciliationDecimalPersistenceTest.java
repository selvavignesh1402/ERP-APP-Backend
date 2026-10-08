package com.riceerp.backend.repository;

import com.riceerp.backend.entity.*;
import com.riceerp.backend.dto.*;
import com.riceerp.backend.enums.ReconciliationStatus;
import com.riceerp.backend.security.TenantIdentifierResolver;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReconciliationDecimalPersistenceTest {
    @Test void resultsAndDetailSurviveReloadAndResponseSerialization() throws Exception {
        var config = new Configuration().addAnnotatedClass(ReconciliationResult.class)
                .addAnnotatedClass(SupplierInvoice.class).addAnnotatedClass(Supplier.class).addAnnotatedClass(Purchase.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:reconciliation_decimal;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        var exact = new BigDecimal("123456789012345.67");
        var price = new BigDecimal("123456789012345.6789");
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule()).enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        try (var factory = config.buildSessionFactory()) {
            Long id;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var tx = session.beginTransaction();
                var supplier = new Supplier(); supplier.setSupplierName("Supplier"); supplier.setPhone("123"); session.persist(supplier);
                var purchase = new Purchase(); purchase.setSupplier(supplier); purchase.setInvoiceNumber("PO-1"); session.persist(purchase);
                var invoice = new SupplierInvoice(); invoice.setSupplier(supplier); invoice.setInvoiceNumber("SI-1"); session.persist(invoice);
                var result = new ReconciliationResult(); result.setPurchase(purchase); result.setInvoice(invoice);
                result.setStatus(ReconciliationStatus.MATCHED); result.setAmountMatched(exact);
                result.setAmountOnPurchase(exact); result.setAmountOnInvoice(exact);
                var detail = new ReconciliationItemDetail(); detail.setOrderedAmount(exact); detail.setBilledAmount(exact);
                detail.setOrderedPrice(price); detail.setBilledPrice(price);
                result.setDetails(mapper.writeValueAsString(List.of(detail)));
                session.persist(result); tx.commit(); id = result.getId();
            }
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var result = session.find(ReconciliationResult.class, id);
                for (BigDecimal amount : List.of(result.getAmountMatched(), result.getAmountOnPurchase(), result.getAmountOnInvoice()))
                    assertEquals(0, exact.compareTo(amount));
                var response = mapper.readTree(mapper.writeValueAsString(ReconciliationResultResponse.from(result)));
                for (String field : List.of("amountMatched", "amountOnPurchase", "amountOnInvoice")) {
                    assertTrue(response.get(field).isNumber()); assertEquals(0, exact.compareTo(response.get(field).decimalValue()));
                }
                var detail = mapper.readTree(response.get("details").textValue()).get(0);
                assertEquals(price, detail.get("orderedPrice").decimalValue());
                assertEquals(price, detail.get("billedPrice").decimalValue());
                assertEquals(exact, detail.get("orderedAmount").decimalValue());
                assertEquals(exact, detail.get("billedAmount").decimalValue());
            }
        }
    }
}
