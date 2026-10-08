package com.riceerp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.Set;

class BusinessResponseContractTest {
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    @ParameterizedTest
    @ValueSource(strings = {"Product", "Supplier", "Purchase", "PurchaseItem", "Delivery", "DeliveryItem", "SalesOrder", "SalesOrderItem", "SupplierInvoice", "SupplierInvoiceItem", "SupplierProduct", "StockAdjustment", "GoodsReceipt", "GoodsReceiptItem", "StockMovement", "PurchaseReturn", "VisitCheckIn", "VisitSchedule", "ReconciliationResult", "SaleItem", "User", "UserProfile", "BeatPlan", "BeatPlanEntry", "PriceHistory"})
    void explicitResponsePreservesBusinessFieldsAndExcludesPersistenceMetadata(String name) throws Exception {
        Class<?> entityType = Class.forName("com.riceerp.backend.entity." + name);
        Object entity = entityType.getConstructor().newInstance();
        Class<?> responseType = Class.forName("com.riceerp.backend.dto." + name + "Response");
        Object response = responseType.getMethod("from", entityType).invoke(null, entity);
        var original = json.valueToTree(entity);
        var result = json.valueToTree(response);
        Set<String> fields = new HashSet<>(); original.fieldNames().forEachRemaining(fields::add);
        fields.remove("version");
        Set<String> actual = new HashSet<>(); result.fieldNames().forEachRemaining(actual::add);
        assertEquals(fields, actual, name);
        assertFalse(actual.contains("passwordHash")); assertFalse(actual.contains("password"));
        for (var component : responseType.getRecordComponents()) {
            assertFalse(component.getGenericType().getTypeName().contains(".entity."), component.getName());
        }
    }
}

