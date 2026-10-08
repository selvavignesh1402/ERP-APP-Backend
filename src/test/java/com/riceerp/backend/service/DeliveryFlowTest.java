package com.riceerp.backend.service;

import com.riceerp.backend.dto.*;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.security.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeliveryFlowTest {
    final DeliveryRepository deliveries = mock(DeliveryRepository.class);
    final DeliveryItemRepository items = mock(DeliveryItemRepository.class);
    final SalesOrderRepository orders = mock(SalesOrderRepository.class);
    final SalesOrderItemRepository orderLines = mock(SalesOrderItemRepository.class);
    final OrganizationRepository organizations = mock(OrganizationRepository.class);
    final SaleService sales = mock(SaleService.class);
    final DeliveryService service = new DeliveryService(deliveries, items, orders, orderLines,
            mock(ProductRepository.class), mock(UserRepository.class), sales, organizations, mock(SaleRepository.class));
    final SalesOrder order = new SalesOrder();
    final Product product = new Product();
    final SalesOrderItem line = new SalesOrderItem();
    final Delivery delivery = new Delivery();
    final DeliveryItem item = new DeliveryItem();
    final List<Delivery> notes = new ArrayList<>();
    @BeforeEach void setup() {
        TenantContext.setCurrentTenant(1L);
        when(organizations.lockForDelivery(1L)).thenReturn(Optional.of(new Organization()));
        product.setId(2L); product.setProductName("Rice");
        order.setId(3L); order.setStatus(SalesOrderStatus.OUT_FOR_DELIVERY); order.setCustomer(new Customer()); order.setSubtotal(1000);
        line.setProduct(product); line.setOrderedQuantity(10); line.setRemainingQuantity(10); line.setPackedQuantity(6);
        line.setUnitPrice(100); order.setItems(List.of(line));
        delivery.setId(4L); delivery.setSalesOrder(order); delivery.setStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        item.setProduct(product); item.setDeliveringQuantity(6); item.setUnitPrice(100); delivery.setItems(List.of(item));
        notes.add(delivery);
        when(deliveries.findByIdAndOrganizationId(4L, 1L)).thenReturn(Optional.of(delivery));
        when(orders.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(order));
        when(deliveries.findBySalesOrderIdAndOrganizationId(3L, 1L)).thenReturn(notes);
        when(deliveries.save(any())).thenAnswer(call -> call.getArgument(0));
        Sale sale = new Sale(); sale.setId(9L);
        when(sales.createSaleFromDelivery(any(), any(), any(), anyList(), any(), any(java.math.BigDecimal.class), any(), anyMap())).thenReturn(sale);
    }
    @AfterEach void clear() { TenantContext.clear(); }
    DeliveryItemConfirmRequest qty(long productId, int quantity) {
        var q = new DeliveryItemConfirmRequest(); q.setProductId(productId); q.setDeliveredQuantity(quantity); return q;
    }
    DeliveryConfirmRequest confirmation(DeliveryItemConfirmRequest... quantities) {
        var request = new DeliveryConfirmRequest(); request.setReceiverName("Customer"); request.setItems(Arrays.asList(quantities)); return request;
    }
    DeliveryCreateRequest dispatch(int quantity) {
        var q = new DeliveryItemCreateRequest(); q.setProductId(2L); q.setDeliveringQuantity(quantity);
        var request = new DeliveryCreateRequest(); request.setSalesOrderId(3L); request.setItems(List.of(q)); return request;
    }
    DeliveryFailRequest failure() {
        var request = new DeliveryFailRequest(); request.setFailureReason(DeliveryFailureReason.SHOP_CLOSED); return request;
    }
    void untouched() {
        verify(deliveries, never()).save(any()); verify(items, never()).save(any());
        verify(orderLines, never()).save(any()); verify(orders, never()).save(any()); verifyNoInteractions(sales);
        assertNull(delivery.getDeliveredAt()); assertEquals(0, line.getDeliveredQuantity());
    }
    @ParameterizedTest @ValueSource(ints = {3, 6})
    void confirmationClosesNoteAndCannotInvoiceTwice(int quantity) {
        service.confirmDelivery(4L, confirmation(qty(2, quantity)), 7L, true);
        assertEquals(quantity, line.getDeliveredQuantity()); assertEquals(10 - quantity, line.getRemainingQuantity());
        assertEquals(quantity, line.getPackedQuantity()); assertEquals(9L, delivery.getGeneratedInvoiceId());
        assertEquals(quantity == 6 ? DeliveryStatus.DELIVERED : DeliveryStatus.PARTIALLY_DELIVERED, delivery.getStatus());
        assertThrows(BusinessRuleException.class, () -> service.confirmDelivery(4L, confirmation(qty(2, quantity)), 7L, true));
        assertThrows(BusinessRuleException.class, () -> service.markDeliveryFailed(4L, failure(), 7L, true));
        verify(sales).createSaleFromDelivery(eq(4L), eq(3L), any(), argThat(lines -> lines.size() == 1 && lines.get(0).getQuantity() == quantity), any(), eq(new java.math.BigDecimal("0.00")), any(), anyMap());
    }
    @ParameterizedTest @ValueSource(ints = {-1, 7, Integer.MAX_VALUE})
    void invalidDeliveredQuantityRejectedBeforeWrites(int quantity) {
        assertThrows(BusinessRuleException.class, () -> service.confirmDelivery(4L, confirmation(qty(2, quantity)), 7L, true)); untouched();
    }
    @Test void remainingOrderAlsoBoundsConfirmation() {
        line.setOrderedQuantity(4);
        assertThrows(BusinessRuleException.class, () -> service.confirmDelivery(4L, confirmation(qty(2, 5)), 7L, true)); untouched();
    }
    @Test void duplicateConfirmationProductsRejected() {
        assertThrows(BusinessRuleException.class, () -> service.confirmDelivery(4L, confirmation(qty(2, 2), qty(2, 2)), 7L, true)); untouched();
    }
    @Test void omittedDeliveryProductMustBeExplicitlyZero() {
        Product other = new Product(); other.setId(5L);
        DeliveryItem otherItem = new DeliveryItem(); otherItem.setProduct(other); otherItem.setDeliveringQuantity(1);
        delivery.setItems(List.of(item, otherItem));
        assertThrows(BusinessRuleException.class, () -> service.confirmDelivery(4L, confirmation(qty(2, 6)), 7L, true)); untouched();
    }
    @Test void unknownConfirmationProductRejected() {
        assertThrows(BusinessRuleException.class, () -> service.confirmDelivery(4L, confirmation(qty(2, 6), qty(5, 1)), 7L, true)); untouched();
    }
    @Test void zeroDeliveryClosesNoteWithoutInvoiceAndReleasesReservation() {
        service.confirmDelivery(4L, confirmation(qty(2, 0)), 7L, true);
        assertEquals(DeliveryStatus.PARTIALLY_DELIVERED, delivery.getStatus()); assertNull(delivery.getGeneratedInvoiceId());
        assertEquals(0, line.getPackedQuantity()); assertEquals(10, line.getRemainingQuantity()); verifyNoInteractions(sales);
        assertThrows(BusinessRuleException.class, () -> service.confirmDelivery(4L, confirmation(qty(2, 1)), 7L, true));
        assertEquals(10, service.createDeliveryNote(dispatch(10)).getItems().get(0).getDeliveringQuantity());
    }
    @ParameterizedTest @EnumSource(value = DeliveryStatus.class, names = {"ASSIGNED", "DELIVERED", "PARTIALLY_DELIVERED", "FAILED", "CANCELLED"})
    void onlyInTransitCanBeConfirmed(DeliveryStatus status) {
        delivery.setStatus(status);
        assertThrows(BusinessRuleException.class, () -> service.confirmDelivery(4L, confirmation(qty(2, 1)), 7L, true)); untouched();
    }
    @ParameterizedTest @EnumSource(value = DeliveryStatus.class, names = {"DELIVERED", "PARTIALLY_DELIVERED", "FAILED", "CANCELLED"})
    void closedNoteCannotStartOrFail(DeliveryStatus status) {
        delivery.setStatus(status);
        assertThrows(BusinessRuleException.class, () -> service.startDelivery(4L, 7L, true));
        assertThrows(BusinessRuleException.class, () -> service.markDeliveryFailed(4L, failure(), 7L, true)); untouched();
    }
    @Test void alreadyInvoicedInconsistentNoteCannotBeConfirmed() {
        delivery.setGeneratedInvoiceId(9L);
        assertThrows(BusinessRuleException.class, () -> service.confirmDelivery(4L, confirmation(qty(2, 1)), 7L, true)); untouched();
    }
    @ParameterizedTest @ValueSource(ints = {0, -1, 5, Integer.MAX_VALUE})
    void dispatchCannotExceedUnallocatedQuantity(int quantity) {
        assertThrows(BusinessRuleException.class, () -> service.createDeliveryNote(dispatch(quantity))); untouched();
    }
    @Test void exactRemainingAllocationCanBeDispatched() {
        Delivery created = service.createDeliveryNote(dispatch(4));
        assertEquals(4, created.getItems().get(0).getDeliveringQuantity()); assertEquals(10, line.getPackedQuantity());
        assertEquals(SalesOrderStatus.OUT_FOR_DELIVERY, order.getStatus());
    }
    @Test void duplicateDispatchProductsRejected() {
        var request = dispatch(2); request.setItems(List.of(request.getItems().get(0), request.getItems().get(0)));
        assertThrows(BusinessRuleException.class, () -> service.createDeliveryNote(request)); untouched();
    }
    @Test void failedNoteReleasesQuantityForNewNote() {
        service.markDeliveryFailed(4L, failure(), 7L, true);
        assertEquals(0, line.getPackedQuantity()); assertEquals(SalesOrderStatus.CONFIRMED, order.getStatus());
        assertEquals(10, service.createDeliveryNote(dispatch(10)).getItems().get(0).getDeliveringQuantity());
        verifyNoInteractions(sales);
    }
    @Test void partialDeliveryCanDispatchOnlyUndeliveredRemainder() {
        service.confirmDelivery(4L, confirmation(qty(2, 3)), 7L, true);
        assertThrows(BusinessRuleException.class, () -> service.createDeliveryNote(dispatch(8)));
        assertEquals(7, service.createDeliveryNote(dispatch(7)).getItems().get(0).getDeliveringQuantity());
        assertEquals(10, line.getPackedQuantity());
    }
    @Test void agreedDiscountReachesGeneratedInvoice() {
        order.setDiscount(100);
        service.confirmDelivery(4L, confirmation(qty(2, 6)), 7L, true);
        verify(sales).createSaleFromDelivery(eq(4L), eq(3L), any(), anyList(), any(), eq(new java.math.BigDecimal("60.00")), any(), anyMap());
    }
    @Test void failureDoesNotResetAnotherInTransitDelivery() {
        Delivery another = new Delivery(); another.setStatus(DeliveryStatus.OUT_FOR_DELIVERY); another.setItems(List.of()); notes.add(another);
        service.markDeliveryFailed(4L, failure(), 7L, true);
        assertEquals(SalesOrderStatus.OUT_FOR_DELIVERY, order.getStatus());
    }
    @Test void wrongShopAndUnassignedUserCannotMutate() {
        assertThrows(AccessDeniedException.class, () -> service.confirmDelivery(4L, confirmation(qty(2, 1)), 7L, false));
        TenantContext.setCurrentTenant(2L);
        when(organizations.lockForDelivery(2L)).thenReturn(Optional.of(new Organization()));
        assertThrows(NotFoundException.class, () -> service.confirmDelivery(4L, confirmation(qty(2, 1)), 7L, true)); untouched();
    }
}
