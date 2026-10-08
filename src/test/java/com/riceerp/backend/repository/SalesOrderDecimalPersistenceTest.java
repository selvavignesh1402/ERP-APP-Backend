package com.riceerp.backend.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.riceerp.backend.dto.SalesOrderResponse;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.security.TenantIdentifierResolver;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class SalesOrderDecimalPersistenceTest {
    @Test void storedOrderAmountsAndPublicJsonKeepExactDecimalValues() throws Exception {
        var config = new Configuration().addAnnotatedClass(SalesOrder.class).addAnnotatedClass(SalesOrderItem.class)
                .addAnnotatedClass(Customer.class).addAnnotatedClass(Product.class)
                .addAnnotatedClass(Organization.class).addAnnotatedClass(User.class)
                .addAnnotatedClass(Delivery.class).addAnnotatedClass(DeliveryItem.class)
                .addAnnotatedClass(Sale.class).addAnnotatedClass(SaleItem.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:order_decimals;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        var subtotal = new BigDecimal("123456789012345.6789");
        var discount = new BigDecimal("0.0001");
        var tax = new BigDecimal("9876543210123.4567");
        var total = subtotal.subtract(discount).add(tax);
        try (var factory = config.buildSessionFactory()) {
            Long id;
            Long[] itemIds = new Long[3];
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var tx = session.beginTransaction();
                var customer = new Customer(); customer.setCustomerName("Decimal customer"); session.persist(customer);
                var order = new SalesOrder(); order.setOrderNumber("SO-DECIMAL"); order.setCustomer(customer);
                order.setSubtotal(subtotal); order.setDiscount(discount); order.setTaxAmount(tax); order.setGrandTotal(total);
                var product = new Product(); product.setProductName("Decimal product"); session.persist(product);
                var orderedItem = new SalesOrderItem(); orderedItem.setProduct(product); orderedItem.setSalesOrder(order);
                orderedItem.setOrderedQuantity(1); orderedItem.setUnitPrice(subtotal); orderedItem.setTotalPrice(subtotal);
                order.setItems(java.util.List.of(orderedItem)); session.persist(order);
                var delivery = new Delivery(); delivery.setDeliveryNumber("DN-DECIMAL"); delivery.setSalesOrder(order);
                var deliveryItem = new DeliveryItem(); deliveryItem.setDelivery(delivery); deliveryItem.setProduct(product);
                deliveryItem.setUnitPrice(subtotal); delivery.setItems(java.util.List.of(deliveryItem)); session.persist(delivery);
                var sale = new Sale(); sale.setBillNumber("INV-DECIMAL"); sale.setPaymentMode(com.riceerp.backend.enums.PaymentMode.CASH);
                session.persist(sale);
                var saleItem = new SaleItem(); saleItem.setSale(sale); saleItem.setProduct(product);
                saleItem.setQuantity(1); saleItem.setPrice(subtotal); session.persist(saleItem);
                tx.commit(); id = order.getId();
                itemIds[0] = orderedItem.getId(); itemIds[1] = deliveryItem.getId(); itemIds[2] = saleItem.getId();
            }
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var loaded = session.find(SalesOrder.class, id);
                assertEquals(0, subtotal.compareTo(loaded.getSubtotal()));
                assertEquals(0, discount.compareTo(loaded.getDiscount()));
                assertEquals(0, tax.compareTo(loaded.getTaxAmount()));
                assertEquals(0, total.compareTo(loaded.getGrandTotal()));
                var orderedItem = session.find(SalesOrderItem.class, itemIds[0]);
                var deliveryItem = session.find(DeliveryItem.class, itemIds[1]);
                var saleItem = session.find(SaleItem.class, itemIds[2]);
                assertEquals(0, subtotal.compareTo(orderedItem.getUnitPrice()));
                assertEquals(0, subtotal.compareTo(orderedItem.getTotalPrice()));
                assertEquals(0, subtotal.compareTo(deliveryItem.getUnitPrice()));
                assertEquals(0, subtotal.compareTo(saleItem.getPrice()));
                var json = new ObjectMapper().registerModule(new JavaTimeModule())
                        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
                var response = json.readTree(json.writeValueAsString(SalesOrderResponse.from(loaded)));
                for (String field : new String[]{"subtotal", "discount", "taxAmount", "grandTotal"})
                    assertTrue(response.get(field).isNumber(), field);
                assertEquals(0, subtotal.compareTo(response.get("subtotal").decimalValue()));
                assertEquals(0, discount.compareTo(response.get("discount").decimalValue()));
                assertEquals(0, tax.compareTo(response.get("taxAmount").decimalValue()));
                assertEquals(0, total.compareTo(response.get("grandTotal").decimalValue()));
                assertEquals(0, subtotal.compareTo(response.get("items").get(0).get("unitPrice").decimalValue()));
                assertEquals(0, subtotal.compareTo(response.get("items").get(0).get("totalPrice").decimalValue()));
                var deliveryJson = json.readTree(json.writeValueAsString(com.riceerp.backend.dto.DeliveryItemResponse.from(deliveryItem)));
                var saleJson = json.readTree(json.writeValueAsString(com.riceerp.backend.dto.SaleItemResponse.from(saleItem)));
                assertTrue(deliveryJson.get("unitPrice").isNumber()); assertTrue(saleJson.get("price").isNumber());
                assertEquals(0, subtotal.compareTo(deliveryJson.get("unitPrice").decimalValue()));
                assertEquals(0, subtotal.compareTo(saleJson.get("price").decimalValue()));
            }
        }
    }
}
