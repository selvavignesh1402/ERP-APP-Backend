package com.riceerp.backend.repository;

import com.riceerp.backend.entity.*;
import com.riceerp.backend.dto.PurchaseResponse;
import com.riceerp.backend.security.TenantIdentifierResolver;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PurchaseTotalPersistenceTest {
    @Test void totalSurvivesDatabaseReloadAndNumericJsonSerialization() throws Exception {
        var config = new Configuration().addAnnotatedClass(Purchase.class).addAnnotatedClass(Supplier.class)
                .addAnnotatedClass(Product.class).addAnnotatedClass(PurchaseItem.class)
                .addAnnotatedClass(GoodsReceipt.class).addAnnotatedClass(GoodsReceiptItem.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:purchase_total_decimal;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        var total = new BigDecimal("123456789012345.6789");
        try (var factory = config.buildSessionFactory()) {
            Long id, lineId, receiptItemId;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var tx = session.beginTransaction();
                var supplier = new Supplier(); supplier.setSupplierName("Supplier"); supplier.setPhone("123"); session.persist(supplier);
                var purchase = new Purchase(); purchase.setSupplier(supplier); purchase.setInvoiceNumber("PO-DECIMAL");
                purchase.setTotalAmount(total); session.persist(purchase);
                var product = new Product(); product.setProductName("Exact price"); session.persist(product);
                var line = new PurchaseItem(); line.setPurchase(purchase); line.setProduct(product);
                line.setQuantity(1); line.setPrice(total); session.persist(line);
                var receipt = new GoodsReceipt(); receipt.setPurchase(purchase); receipt.setReceiptNumber("GRN-DECIMAL"); session.persist(receipt);
                var received = new GoodsReceiptItem(); received.setReceipt(receipt); received.setProduct(product);
                received.setOrderedQty(1); received.setReceivedQty(1); received.setUnitPrice(total); session.persist(received);
                tx.commit(); id = purchase.getId(); lineId = line.getId(); receiptItemId = received.getId();
            }
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var purchase = session.find(Purchase.class, id); assertEquals(total, purchase.getTotalAmount());
                var json = new ObjectMapper().registerModule(new JavaTimeModule()).enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
                var response = json.readTree(json.writeValueAsString(PurchaseResponse.from(purchase)));
                assertTrue(response.get("totalAmount").isNumber()); assertEquals(total, response.get("totalAmount").decimalValue());
                var line = session.find(PurchaseItem.class, lineId);
                var received = session.find(GoodsReceiptItem.class, receiptItemId);
                assertEquals(total, line.getPrice()); assertEquals(total, received.getUnitPrice());
                var lineJson = json.readTree(json.writeValueAsString(com.riceerp.backend.dto.PurchaseItemResponse.from(line)));
                var receiptJson = json.readTree(json.writeValueAsString(com.riceerp.backend.dto.GoodsReceiptItemResponse.from(received)));
                assertTrue(lineJson.get("price").isNumber()); assertEquals(total, lineJson.get("price").decimalValue());
                assertTrue(receiptJson.get("unitPrice").isNumber()); assertEquals(total, receiptJson.get("unitPrice").decimalValue());
            }
        }
    }
}
