package com.riceerp.backend.service;

import com.riceerp.backend.controller.PurchaseController;
import com.riceerp.backend.dto.*;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.exception.*;
import com.riceerp.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PurchaseApprovalTest {
    @Test void exactRequestPriceFlowsIntoLineAndTotal() {
        var request = request(PurchaseStatus.DRAFT);
        var price = new java.math.BigDecimal("123456789012345.6789");
        request.getItems().get(0).setPrice(price);
        var result = service.createPurchase(request);
        assertEquals(price.multiply(java.math.BigDecimal.valueOf(2)), result.getTotalAmount());
        verify(items).save(argThat(item -> price.equals(item.getPrice())));
    }
    @Test void calculatedTotalRetainsEveryPaisaBeforeSaving() {
        var request = request(PurchaseStatus.DRAFT);
        request.getItems().get(0).setQuantity(99);
        request.getItems().get(0).setPrice(999999999999.99);
        var result = service.createPurchase(request);
        assertEquals(new java.math.BigDecimal("98999999999999.0100"), result.getTotalAmount());
        verify(purchases, times(1)).save(any());
    }
    @Test void fractionalTotalUsesTheExistingFourDecimalStoragePrecision() {
        var request = request(PurchaseStatus.DRAFT);
        request.getItems().get(0).setQuantity(.123456);
        request.getItems().get(0).setPrice(1);
        assertEquals(new java.math.BigDecimal("0.1235"), service.createPurchase(request).getTotalAmount());
    }
    @Test void invalidLaterLineDoesNotWriteAnEarlierLineOrPurchase() {
        var request = request(PurchaseStatus.DRAFT);
        var invalid = new PurchaseItemRequest(); invalid.setProductId(2L); invalid.setQuantity(1); invalid.setPrice((java.math.BigDecimal) null);
        request.setItems(List.of(request.getItems().get(0), invalid));
        assertThrows(BusinessRuleException.class, () -> service.createPurchase(request));
        verify(purchases, never()).save(any()); verify(items, never()).save(any()); verifyNoInteractions(movements);
    }
    @Test void oversizedTotalIsRejectedBeforeWrites() {
        var request = request(PurchaseStatus.DRAFT);
        request.getItems().get(0).setPrice(99999999999999.0); request.getItems().get(0).setQuantity(11);
        assertThrows(BusinessRuleException.class, () -> service.createPurchase(request));
        verify(purchases, never()).save(any()); verify(items, never()).save(any());
    }
    @Test void invalidQuantityOrPriceCannotReachStorage() {
        for (double quantity : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY, 1e13, .0000001}) {
            var request = request(PurchaseStatus.DRAFT); request.getItems().get(0).setQuantity(quantity);
            assertThrows(BusinessRuleException.class, () -> service.createPurchase(request));
        }
        for (double price : new double[]{0, -1, 1e15, .00001}) {
            var request = request(PurchaseStatus.DRAFT); request.getItems().get(0).setPrice(price);
            assertThrows(BusinessRuleException.class, () -> service.createPurchase(request));
        }
        verify(purchases, never()).save(any()); verify(items, never()).save(any());
    }
    final PurchaseRepository purchases = mock(PurchaseRepository.class);
    final PurchaseItemRepository items = mock(PurchaseItemRepository.class);
    final SupplierRepository suppliers = mock(SupplierRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final StockMovementService movements = mock(StockMovementService.class);
    final GoodsReceiptService receiptHistory = mock(GoodsReceiptService.class);
    final ProcurementLock procurementLock = mock(ProcurementLock.class);
    final PurchaseService service = new PurchaseService(purchases, items, mock(PurchaseReturnRepository.class), suppliers, products, movements, receiptHistory, procurementLock);
    final Product product = new Product();

    @BeforeEach void setup() {
        when(procurementLock.acquire()).thenReturn(1L);
        product.setId(2L); product.setStock(10);
        when(suppliers.findById(1L)).thenReturn(Optional.of(new Supplier()));
        when(products.findById(2L)).thenReturn(Optional.of(product));
        when(purchases.save(any())).thenAnswer(call -> {
            Purchase p = call.getArgument(0); p.setId(3L); return p;
        });
    }
    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }
    PurchaseRequest request(PurchaseStatus status) {
        PurchaseItemRequest line = new PurchaseItemRequest(); line.setProductId(2L); line.setQuantity(2); line.setPrice(100);
        PurchaseRequest request = new PurchaseRequest(); request.setSupplierId(1L); request.setInvoiceNumber("INV-1");
        request.setStatus(status); request.setItems(List.of(line)); return request;
    }
    void assertDraft(PurchaseStatus status) {
        Purchase created = service.createPurchase(request(status));
        assertEquals(PurchaseStatus.DRAFT, created.getStatus()); assertEquals(0, new java.math.BigDecimal("200").compareTo(created.getTotalAmount()));
        assertEquals("INV-1", created.getInvoiceNumber()); assertEquals(10, product.getStock());
        verify(items).save(argThat(item -> item.getQuantity() == 2 && item.getPrice().compareTo(java.math.BigDecimal.valueOf(100)) == 0));
        verify(products, never()).save(any()); verifyNoInteractions(movements);
    }
    @ParameterizedTest @EnumSource(PurchaseStatus.class)
    void everyCallerSuppliedInitialStatusBecomesDraft(PurchaseStatus status) { assertDraft(status); }
    @Test void omittedStatusAlsoCreatesDraft() { assertDraft(null); }
    @Test void approvalMustFollowSubmission() {
        Purchase p = service.createPurchase(request(PurchaseStatus.APPROVED));
        when(purchases.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(p));
        assertThrows(BusinessRuleException.class, () -> service.approve(3L));
        assertThrows(BusinessRuleException.class, () -> service.order(3L));
        assertEquals(PurchaseStatus.PENDING_APPROVAL, service.submit(3L).getStatus());
        assertEquals(PurchaseStatus.APPROVED, service.approve(3L).getStatus());
        assertEquals(PurchaseStatus.ORDERED, service.order(3L).getStatus());
        verifyNoInteractions(movements);
    }
    @ParameterizedTest @EnumSource(value = PurchaseStatus.class, names = {"PARTIALLY_RECEIVED", "RECEIVED"})
    void manualStatusCannotPretendGoodsArrived(PurchaseStatus target) {
        Purchase p = new Purchase(); p.setStatus(PurchaseStatus.ORDERED);
        when(purchases.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(p));
        assertThrows(BusinessRuleException.class, () -> service.updateStatus(3L, target));
        assertEquals(PurchaseStatus.ORDERED, p.getStatus()); verify(purchases, never()).save(any());
        verifyNoInteractions(movements);
    }
    @Test void missingTargetGivesBusinessError() {
        assertThrows(BusinessRuleException.class, () -> service.updateStatus(3L, null));
        verify(purchases, never()).save(any());
    }
    @Test void receivedPurchaseCanStillBeCompleted() {
        Purchase p = new Purchase(); p.setStatus(PurchaseStatus.RECEIVED);
        when(purchases.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(p));
        PurchaseItem line = new PurchaseItem(); line.setProduct(product); line.setQuantity(2);
        when(items.findByPurchaseId(3L)).thenReturn(List.of(line));
        when(receiptHistory.getReceivedQuantities(3L)).thenReturn(Map.of(2L, 2.0));
        assertEquals(PurchaseStatus.COMPLETED, service.updateStatus(3L, PurchaseStatus.COMPLETED).getStatus());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(doubles = {1, 2})
    void realReceiptStillUpdatesStockAndDerivesPurchaseStatus(double received) {
        Purchase p = new Purchase(); p.setId(3L); p.setStatus(PurchaseStatus.ORDERED);
        when(purchases.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(p));
        PurchaseItem line = new PurchaseItem(); line.setProduct(product); line.setQuantity(2); line.setPrice(100);
        when(items.findByPurchaseId(3L)).thenReturn(List.of(line));
        GoodsReceiptRepository receipts = mock(GoodsReceiptRepository.class);
        when(receipts.save(any())).thenAnswer(call -> {
            GoodsReceipt receipt = call.getArgument(0); receipt.setId(4L); return receipt;
        });
        ProcurementLock lock = mock(ProcurementLock.class); when(lock.acquire()).thenReturn(1L);
        GoodsReceiptService receiving = new GoodsReceiptService(receipts, mock(GoodsReceiptItemRepository.class),
                purchases, items, products, movements, lock);
        GoodsReceiptItemRequest receivedItem = new GoodsReceiptItemRequest();
        receivedItem.setProductId(2L); receivedItem.setReceivedQty(received); receivedItem.setUnitPrice(100);
        GoodsReceiptRequest request = new GoodsReceiptRequest(); request.setItems(List.of(receivedItem));
        receiving.createReceipt(3L, request);
        assertEquals(received == 2 ? PurchaseStatus.RECEIVED : PurchaseStatus.PARTIALLY_RECEIVED, p.getStatus());
        assertEquals(10 + received, product.getStock());
        verify(movements).record(product, MovementType.PURCHASE_RECEIPT, received, 4L);
    }
    @Test void creatorCannotApproveThroughEitherHttpRoute() throws Exception {
        ProxyFactory proxy = new ProxyFactory(new PurchaseController(service));
        proxy.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());
        var mvc = MockMvcBuilders.standaloneSetup(proxy.getProxy()).setControllerAdvice(new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(7L, null,
                List.of(new SimpleGrantedAuthority("purchase:create"))));
        mvc.perform(post("/purchases").contentType("application/json").content(
                "{\"supplierId\":1,\"status\":\"APPROVED\",\"items\":[{\"productId\":2,\"quantity\":2,\"price\":100}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));
        Purchase p = new Purchase(); p.setStatus(PurchaseStatus.DRAFT);
        when(purchases.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(p));
        mvc.perform(put("/purchases/3/submit")).andExpect(status().isOk());
        clearInvocations(purchases);
        for (String route : List.of("approve", "order", "cancel", "status")) {
            mvc.perform(put("/purchases/3/" + route).contentType("application/json").content("{\"status\":\"APPROVED\"}"))
                    .andExpect(status().isForbidden());
        }
        verify(purchases, never()).save(any());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(8L, null,
                List.of(new SimpleGrantedAuthority("purchase:approve"))));
        mvc.perform(put("/purchases/3/approve")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(put("/purchases/3/order")).andExpect(status().isOk());
        mvc.perform(put("/purchases/3/status").contentType("application/json").content("{\"status\":\"RECEIVED\"}"))
                .andExpect(status().isConflict());
        assertEquals(PurchaseStatus.ORDERED, p.getStatus());
    }
}
