package com.riceerp.backend.service;

import com.riceerp.backend.dto.SaleItemRequest;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderBillingTest {
    private static void assertDiscount(double expected, java.math.BigDecimal actual) {
        assertEquals(0, java.math.BigDecimal.valueOf(expected).compareTo(actual));
    }
    @Test void largeDiscountKeepsEveryPaisaAcrossPartialInvoices() {
        var order = order(300000000000000.0, 3, 1, 0);
        var discount = new java.math.BigDecimal("823456789012345.67");
        order.setDiscount(discount);
        var first = OrderBilling.discountFor(order, List.of(invoice(300000000000000.0, 1)), List.of());
        assertEquals(new java.math.BigDecimal("274485596337448.56"), first);
        var earlier = previous(300000000000000.0, 0); earlier.setDiscount(first);
        order.getItems().get(0).setDeliveredQuantity(2);
        var second = OrderBilling.discountFor(order, List.of(invoice(300000000000000.0, 1)), List.of(earlier));
        assertEquals(new java.math.BigDecimal("274485596337448.55"), second);
        var next = previous(300000000000000.0, 0); next.setDiscount(second);
        order.getItems().get(0).setDeliveredQuantity(3);
        var third = OrderBilling.discountFor(order, List.of(invoice(300000000000000.0, 1)), List.of(earlier, next));
        assertEquals(new java.math.BigDecimal("274485596337448.56"), third);
        assertEquals(discount, first.add(second).add(third));
    }
    SalesOrder order(double price, int quantity, int delivered, double discount) {
        Product p = new Product(); p.setId(1L);
        SalesOrderItem line = new SalesOrderItem(); line.setProduct(p); line.setUnitPrice(price);
        line.setOrderedQuantity(quantity); line.setDeliveredQuantity(delivered);
        SalesOrder order = new SalesOrder(); order.setItems(List.of(line)); order.setSubtotal(price * quantity); order.setDiscount(discount); return order;
    }
    SaleItemRequest invoice(double price, double quantity) {
        SaleItemRequest line = new SaleItemRequest(); line.setProductId(1L); line.setPrice(price); line.setQuantity(quantity); return line;
    }
    Sale previous(double total, double discount) { Sale sale = new Sale(); sale.setTotal(total); sale.setDiscount(discount); return sale; }
    @Test void partialInvoicesKeepTheWholeAgreedDiscount() {
        SalesOrder order = order(100, 10, 6, 100);
        assertDiscount(60, OrderBilling.discountFor(order, List.of(invoice(100, 6)), List.of()));
        order.getItems().get(0).setDeliveredQuantity(10);
        assertDiscount(40, OrderBilling.discountFor(order, List.of(invoice(100, 4)), List.of(previous(600, 60))));
    }
    @Test void indivisibleDiscountCentsAreAllocatedExactlyOnce() {
        SalesOrder order = order(1, 3, 1, 1);
        assertDiscount(.33, OrderBilling.discountFor(order, List.of(invoice(1, 1)), List.of()));
        order.getItems().get(0).setDeliveredQuantity(2);
        assertDiscount(.34, OrderBilling.discountFor(order, List.of(invoice(1, 1)), List.of(previous(1, .33))));
        order.getItems().get(0).setDeliveredQuantity(3);
        assertDiscount(.33, OrderBilling.discountFor(order, List.of(invoice(1, 1)), List.of(previous(1, .33), previous(1, .34))));
    }
    @Test void mixedPricesAllocateDiscountByValueRatherThanBagCount() {
        SalesOrder order = order(100, 1, 1, 30);
        Product p = new Product(); p.setId(2L);
        SalesOrderItem second = new SalesOrderItem(); second.setProduct(p); second.setUnitPrice(200); second.setOrderedQuantity(1);
        order.setItems(List.of(order.getItems().get(0), second)); order.setSubtotal(300);
        assertDiscount(10, OrderBilling.discountFor(order, List.of(invoice(100, 1)), List.of()));
    }
    @Test void oldInvoiceMissingDiscountNeedsReconciliation() {
        assertThrows(BusinessRuleException.class, () -> OrderBilling.discountFor(order(100, 10, 10, 100),
                List.of(invoice(100, 4)), List.of(previous(600, 0))));
    }
    @Test void missingEarlierInvoiceCannotBeBilledAgain() {
        assertThrows(BusinessRuleException.class, () -> OrderBilling.discountFor(order(100, 10, 10, 100), List.of(invoice(100, 4)), List.of()));
    }
    @Test void priceAndOrderTotalMismatchAreRejected() {
        assertThrows(BusinessRuleException.class, () -> OrderBilling.discountFor(order(100, 10, 6, 100), List.of(invoice(90, 6)), List.of()));
        SalesOrder order = order(100, 10, 6, 100); order.setSubtotal(900);
        assertThrows(BusinessRuleException.class, () -> OrderBilling.discountFor(order, List.of(invoice(100, 6)), List.of()));
    }
    @Test void zeroAndFullDiscountsRemainValid() {
        assertDiscount(0, OrderBilling.discountFor(order(100, 10, 6, 0), List.of(invoice(100, 6)), List.of()));
        assertDiscount(600, OrderBilling.discountFor(order(100, 10, 6, 1000), List.of(invoice(100, 6)), List.of()));
    }
}
