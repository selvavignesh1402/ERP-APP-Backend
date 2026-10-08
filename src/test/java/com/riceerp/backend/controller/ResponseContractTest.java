package com.riceerp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.riceerp.backend.dto.CustomerResponse;
import com.riceerp.backend.dto.PaymentResponse;
import com.riceerp.backend.dto.SaleResponse;
import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.enums.PaymentMode;
import com.riceerp.backend.enums.ReferenceType;
import com.riceerp.backend.enums.Status;
import com.riceerp.backend.entity.Payment;
import com.riceerp.backend.entity.Sale;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Locks the JSON field names of the migrated response DTOs.
 *
 * <p>Replacing entity serialization with DTOs is only safe while the wire format is
 * unchanged. This compares each DTO's field set against what the entity used to
 * serialize, so an accidental rename on either side fails here instead of in the app.
 */
class ResponseContractTest {

    // Spring Boot registers the Java-time module on its auto-configured mapper;
    // a bare ObjectMapper does not, so mirror that here.
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Set<String> fields(Object value) {
        var node = mapper.valueToTree(value);
        Set<String> names = new TreeSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private Set<String> responseFields(Object entity, Object response) {
        Set<String> entityNames = fields(entity);
        Set<String> responseNames = fields(response);
        // Everything the entity exposed must still be present on the response.
        responseNames.retainAll(entityNames);
        return responseNames;
    }

    @Test
    void customerResponseCoversEveryClientVisibleCustomerField() {
        Customer c = new Customer();
        c.setCustomerName("Demo Shop"); c.setPhone("9999999999"); c.setEmail("a@b.test");
        c.setAddress("Street"); c.setGstNumber("GST1"); c.setCreditLimit(500.0);
        c.setCreditBalance(120.5); c.setStatus(Status.ACTIVE);

        var expected = new TreeSet<>(Set.of("organizationId", "id", "customerName", "phone", "email",
                "address", "gstNumber", "creditLimit", "creditBalance", "status", "createdAt"));
        assertEquals(expected, fields(CustomerResponse.from(c)));
        // version is intentionally not exposed: it is a JPA @Version lock column,
        // not part of the business record.
        assertFalse(fields(CustomerResponse.from(c)).contains("version"));
    }

    @Test
    void paymentResponseCoversEveryClientVisiblePaymentField() {
        Payment p = new Payment();
        p.setAmount(250.75); p.setReferenceType(ReferenceType.SALE); p.setReferenceId(7L);
        p.setPaymentMode(PaymentMode.CASH); p.setOpeningBalanceAmount(10.0);
        p.setClientReferenceId("ref-1");
        Map<Long, BigDecimal> allocations = new LinkedHashMap<>();
        allocations.put(7L, new BigDecimal("250.75"));
        // Payment exposes the live allocation map; there is no setter.
        p.getSaleAllocations().putAll(allocations);

        var expected = new TreeSet<>(Set.of("id", "organizationId", "clientReferenceId", "referenceType",
                "referenceId", "amount", "paymentMode", "paymentDate", "openingBalanceAmount", "saleAllocations"));
        assertEquals(expected, fields(PaymentResponse.from(p)));
        assertEquals(allocations, PaymentResponse.from(p).getSaleAllocations());
    }

    @Test
    void saleResponseCoversEveryClientVisibleSaleFieldAndKeepsNestedCustomer() {
        Customer c = new Customer();
        c.setCustomerName("Snapshot Shop"); c.setPhone("9000000000");
        Sale s = new Sale();
        s.setBillNumber("B-1"); s.setCustomer(c);
        s.setCustomerName("Snapshot Shop"); s.setCustomerPhone("9000000000");
        s.setCustomerAddress("Addr"); s.setShopName("Main Shop");
        s.setPaymentMode(PaymentMode.UPI); s.setGrandTotal(999.99);

        SaleResponse r = SaleResponse.from(s);
        var expected = new TreeSet<>(Set.of("id", "organizationId", "billNumber", "customer",
                "customerName", "customerPhone", "customerAddress", "shopName", "saleDate",
                "paymentMode", "taxType", "total", "discount", "cgst", "sgst", "igst", "grandTotal",
                "paidAmount", "balanceDue", "clientReferenceId", "salesOrderId", "deliveryId", "createdAt"));
        assertEquals(expected, fields(r));

        // The app reads sale.customer.customerName for search, so the nesting must survive.
        assertNotNull(r.getCustomer());
        assertEquals("Snapshot Shop", r.getCustomer().getCustomerName());
    }

    @Test
    void saleResponseNeverSerializesTheCustomerEntityItself() {
        Customer c = new Customer();
        c.setCustomerName("Leak Check"); c.setPhone("9111111111");
        Sale s = new Sale();
        s.setCustomer(c);
        // A nested CustomerResponse must not itself nest another customer.
        SaleResponse r = SaleResponse.from(s);
        assertEquals(CustomerResponse.class, r.getCustomer().getClass());
        assertFalse(fields(r).contains("passwordHash"));
    }

    @Test
    void paymentDecimalsRemainExactJsonNumbers() throws Exception {
        var amount = new BigDecimal("123456789012345.6789");
        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setOpeningBalanceAmount(new BigDecimal("0.0001"));
        payment.getSaleAllocations().put(7L, amount);
        var reader = mapper.copy().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        var json = reader.readTree(mapper.writeValueAsString(PaymentResponse.from(payment)));
        assertTrue(json.get("amount").isNumber());
        assertEquals(0, amount.compareTo(json.get("amount").decimalValue()));
        assertEquals(0, amount.compareTo(json.get("saleAllocations").get("7").decimalValue()));
        assertEquals(0, new BigDecimal("0.0001").compareTo(json.get("openingBalanceAmount").decimalValue()));
    }

    @Test
    void saleDecimalsStayExactInResponsesAndSummary() throws Exception {
        var total = new BigDecimal("123456789012345.68");
        var paid = new BigDecimal("123456789012345.67");
        var due = new BigDecimal("0.01");
        Sale sale = new Sale();
        sale.setGrandTotal(total);
        sale.setTotal(total);
        sale.setDiscount(due);
        sale.setCgst(due);
        sale.setSgst(due);
        sale.setIgst(due);
        sale.setPaidAmount(paid);
        sale.setBalanceDue(due);
        var reader = mapper.copy().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        var json = reader.readTree(mapper.writeValueAsString(SaleResponse.from(sale)));
        assertTrue(json.get("grandTotal").isNumber());
        for (var field : java.util.List.of("grandTotal", "total"))
            assertEquals(0, total.compareTo(json.get(field).decimalValue()));
        assertEquals(0, paid.compareTo(json.get("paidAmount").decimalValue()));
        for (var field : java.util.List.of("balanceDue", "discount", "cgst", "sgst", "igst"))
            assertEquals(0, due.compareTo(json.get(field).decimalValue()));
        var report = com.riceerp.backend.dto.SalesSummary.fromSales(java.util.List.of(sale), java.time.LocalDateTime.now());
        assertEquals(total, report.invoicedTotal());
        assertEquals(paid, report.paymentsApplied());
        assertEquals(due, report.outstanding());
    }

    @Test
    void nullEntityMapsToNullResponse() {
        assertNull(CustomerResponse.from(null));
        assertNull(PaymentResponse.from(null));
        assertNull(SaleResponse.from(null));
    }
}
