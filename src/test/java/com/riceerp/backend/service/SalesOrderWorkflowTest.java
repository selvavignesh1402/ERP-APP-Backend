package com.riceerp.backend.service;

import com.riceerp.backend.controller.SalesOrderController;
import com.riceerp.backend.dto.*;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.exception.*;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.security.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SalesOrderWorkflowTest {
    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
    final SalesOrderRepository orders = mock(SalesOrderRepository.class);
    final CustomerRepository customers = mock(CustomerRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final OrganizationRepository organizations = mock(OrganizationRepository.class);
    final DeliveryRepository deliveries = mock(DeliveryRepository.class);
    final SalesOrderService service = new SalesOrderService(orders, mock(SalesOrderItemRepository.class), customers,
            products, mock(UserRepository.class), organizations, deliveries);
    final SalesOrder order = new SalesOrder();
    final SalesOrderItem line = new SalesOrderItem();
    @BeforeEach void setup() {
        TenantContext.setCurrentTenant(1L);
        when(organizations.lockForDelivery(1L)).thenReturn(Optional.of(new Organization()));
        order.setId(3L); order.setStatus(SalesOrderStatus.CONFIRMED); order.setItems(List.of(line));
        when(orders.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(order));
        when(orders.save(any())).thenAnswer(call -> call.getArgument(0));
        when(customers.findByIdAndOrganizationId(2L, 1L)).thenReturn(Optional.of(new Customer()));
        Product product = new Product(); product.setGstRate(5.0); product.setId(4L); product.setSellingPrice(100); product.setStock(100);
        when(products.findForStockUpdate(4L)).thenReturn(Optional.of(product));
    }
    @AfterEach void clear() { TenantContext.clear(); SecurityContextHolder.clearContext(); }
    @Test void interstateOrderSnapshotsProductRate() {
        var req = request(100);
        req.setTaxType(TaxType.INTER_STATE);
        Product product = products.findForStockUpdate(4L).orElseThrow();
        product.setGstRate(18.0);
        var saved = service.createSalesOrder(req);
        assertMoney("162", saved.getTaxAmount());
        assertMoney("1062", saved.getGrandTotal());
        assertEquals(TaxType.INTER_STATE, saved.getTaxType());
        product.setGstRate(5.0);
        assertEquals(18.0, saved.getItems().get(0).getGstRate());
    }
    @Test void productWithoutGstCreatesUntaxedOrder() {
        products.findForStockUpdate(4L).orElseThrow().setGstRate(null);
        var saved = service.createSalesOrder(request(100));
        assertMoney("0", saved.getTaxAmount());
        assertMoney("900", saved.getGrandTotal());
        assertNull(saved.getItems().get(0).getGstRate());
    }
    @Test void staleProductGstRejectsOrderBeforeSave() {
        var request = request(0);
        request.getItems().get(0).setGstRate(null);
        assertThrows(BusinessRuleException.class, () -> service.createSalesOrder(request));
        verify(orders, never()).save(any());
    }
    SalesOrderRequest request(double discount) {
        var item = new SalesOrderItemRequest(); item.setProductId(4L); item.setQuantity(10); item.setUnitPrice(100);
        var request = new SalesOrderRequest(); request.setCustomerId(2L); request.setItems(List.of(item)); request.setDiscount(BigDecimal.valueOf(discount)); return request;
    }
    @Test void validOrderPreservesAgreedPricesAndDiscount() {
        SalesOrder result = service.createSalesOrder(request(100));
        assertMoney("1000", result.getSubtotal()); assertMoney("100", result.getDiscount());
        assertMoney("945", result.getGrandTotal()); assertMoney("100", result.getItems().get(0).getUnitPrice());
        verify(products, never()).save(any());
    }
    @Test void largeOrderKeepsTheLastPaisaInCalculatedTotals() {
        products.findForStockUpdate(4L).orElseThrow().setGstRate(null);
        var request = request(.02);
        request.getItems().get(0).setUnitPrice(999999999999.99);
        request.getItems().get(0).setQuantity(99);
        var result = service.createSalesOrder(request);
        assertMoney("98999999999999.01", result.getSubtotal());
        assertMoney("0.02", result.getDiscount());
        assertMoney("98999999999998.99", result.getGrandTotal());
    }
    @Test void orderExceedingDatabaseCapacityIsRejectedBeforeSave() {
        var request = request(0);
        request.getItems().get(0).setUnitPrice(100000000000000.0);
        assertThrows(BusinessRuleException.class, () -> service.createSalesOrder(request));
        verify(orders, never()).save(any());
    }
    @Test void exactRequestDiscountReachesTheSavedOrder() {
        products.findForStockUpdate(4L).orElseThrow().setGstRate(null);
        var request = request(0);
        request.getItems().get(0).setQuantity(3);
        request.getItems().get(0).setUnitPrice(300000000000000.0);
        request.setDiscount(new BigDecimal("823456789012345.67"));
        var result = service.createSalesOrder(request);
        assertMoney("823456789012345.67", result.getDiscount());
        assertMoney("76543210987654.33", result.getGrandTotal());
    }
    @Test void exactRequestPriceAndMultipliedLineTotalReachTheSavedOrder() {
        products.findForStockUpdate(4L).orElseThrow().setGstRate(null);
        var request = request(.01);
        request.getItems().get(0).setQuantity(3);
        request.getItems().get(0).setUnitPrice(new BigDecimal("123456789012345.67"));
        var result = service.createSalesOrder(request);
        assertMoney("123456789012345.67", result.getItems().get(0).getUnitPrice());
        assertMoney("370370367037037.01", result.getItems().get(0).getTotalPrice());
        assertMoney("370370367037037.00", result.getGrandTotal());
    }
    @Test void gstCannotPushTheOrderBeyondDatabaseCapacity() {
        var request = request(0);
        request.getItems().get(0).setUnitPrice(99999999999999.0);
        assertThrows(BusinessRuleException.class, () -> service.createSalesOrder(request));
        verify(orders, never()).save(any());
    }
    @ParameterizedTest @NullSource @ValueSource(strings = {"-1", "1001", "1.001", "1000000000000000"})
    void invalidDiscountRejectedBeforeSave(String discount) {
        var request = request(0);
        request.setDiscount(discount == null ? null : new BigDecimal(discount));
        assertThrows(BusinessRuleException.class, () -> service.createSalesOrder(request)); verify(orders, never()).save(any());
    }
    @Test void ambiguousDuplicateProductsRejected() {
        var request = request(0); request.setItems(List.of(request.getItems().get(0), request.getItems().get(0)));
        assertThrows(BusinessRuleException.class, () -> service.createSalesOrder(request)); verify(orders, never()).save(any());
    }
    @Test void preparationStepsWorkWithoutFabricatingDelivery() {
        assertEquals(SalesOrderStatus.PROCESSING, service.updateStatus(3L, SalesOrderStatus.PROCESSING).getStatus());
        assertEquals(SalesOrderStatus.READY_FOR_DELIVERY, service.updateStatus(3L, SalesOrderStatus.READY_FOR_DELIVERY).getStatus());
        assertThrows(BusinessRuleException.class, () -> service.updateStatus(3L, SalesOrderStatus.DELIVERED));
    }
    @ParameterizedTest @EnumSource(value = SalesOrderStatus.class, names = {"DRAFT", "CONFIRMED", "OUT_FOR_DELIVERY", "PARTIALLY_DELIVERED", "DELIVERED", "CANCELLED"})
    void arbitraryStatusTargetsRejected(SalesOrderStatus target) {
        assertThrows(BusinessRuleException.class, () -> service.updateStatus(3L, target)); verify(orders, never()).save(any());
    }
    @ParameterizedTest @EnumSource(value = SalesOrderStatus.class, names = {"DELIVERED", "CANCELLED", "OUT_FOR_DELIVERY"})
    void closedOrInTransitOrdersCannotBeReopened(SalesOrderStatus status) {
        order.setStatus(status);
        assertThrows(BusinessRuleException.class, () -> service.updateStatus(3L, SalesOrderStatus.READY_FOR_DELIVERY));
        verify(orders, never()).save(any());
    }
    @Test void activeDispatchPreventsPreparationAndCancellation() {
        Delivery note = new Delivery(); note.setStatus(DeliveryStatus.ASSIGNED);
        when(deliveries.findBySalesOrderIdAndOrganizationId(3L, 1L)).thenReturn(List.of(note));
        assertThrows(BusinessRuleException.class, () -> service.updateStatus(3L, SalesOrderStatus.READY_FOR_DELIVERY));
        assertThrows(BusinessRuleException.class, () -> service.cancelSalesOrder(3L, "cancel")); verify(orders, never()).save(any());
    }
    @Test void partialFulfillmentCannotBeCancelledButCanPrepareRemainder() {
        line.setDeliveredQuantity(1); order.setStatus(SalesOrderStatus.PARTIALLY_DELIVERED);
        assertThrows(BusinessRuleException.class, () -> service.cancelSalesOrder(3L, "cancel"));
        assertEquals(SalesOrderStatus.READY_FOR_DELIVERY, service.updateStatus(3L, SalesOrderStatus.READY_FOR_DELIVERY).getStatus());
    }
    @Test void cleanUnfulfilledOrderCanBeCancelledOnce() {
        assertEquals(SalesOrderStatus.CANCELLED, service.cancelSalesOrder(3L, "Customer request").getStatus());
        assertTrue(order.getNotes().contains("Customer request"));
        assertThrows(BusinessRuleException.class, () -> service.cancelSalesOrder(3L, null));
    }
    @Test void cancelPermissionDoesNotGrantPreparationPermission() throws Exception {
        ProxyFactory proxy = new ProxyFactory(new SalesOrderController(service));
        proxy.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());
        var mvc = MockMvcBuilders.standaloneSetup(proxy.getProxy()).setControllerAdvice(new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(7L, null,
                List.of(new SimpleGrantedAuthority("sales-order:cancel"))));
        mvc.perform(put("/api/sales-orders/3/status").param("status", "READY_FOR_DELIVERY")).andExpect(status().isForbidden());
        verify(orders, never()).save(any());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(7L, null,
                List.of(new SimpleGrantedAuthority("delivery:create"))));
        mvc.perform(put("/api/sales-orders/3/status").param("status", "READY_FOR_DELIVERY")).andExpect(status().isOk());
        mvc.perform(put("/api/sales-orders/3/status").param("status", "DELIVERED")).andExpect(status().isConflict());
        mvc.perform(put("/api/sales-orders/3/cancel")).andExpect(status().isForbidden());
    }
}
