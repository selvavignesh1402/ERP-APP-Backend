package com.riceerp.backend.service;

import com.riceerp.backend.dto.SalesSummary;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.security.TenantContext;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.controller.MasterAdminController;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportingTest {
    @AfterEach void cleanup() { TenantContext.clear(); }
    Sale invoice(long id, double total, String date) {
        Sale sale = new Sale(); sale.setId(id); sale.setGrandTotal(total);
        sale.setSaleDate(LocalDateTime.parse(date)); sale.setPaidAmount(0.0); sale.setBalanceDue(total);
        return sale;
    }
    @Test void dailyTrendUsesSavedValuesAndSevenCalendarDates() {
        var now = LocalDateTime.parse("2026-09-03T12:00:00");
        var report = SalesSummary.fromSales(List.of(invoice(1, 10.10, "2026-08-28T23:59:00"),
                invoice(2, 20.20, "2026-08-28T01:00:00"), invoice(3, 100, "2026-08-27T12:00:00"),
                invoice(4, 200, "2026-09-04T00:00:00")), now);
        assertEquals(4, report.invoiceCount()); assertEquals("330.30", report.invoicedTotal().toPlainString());
        assertEquals(7, report.trend().size()); assertEquals("2026-08-28", report.periodStart().toString());
        assertEquals("30.30", report.trend().get(0).amount().toPlainString());
        assertTrue(report.trend().subList(1, 7).stream().allMatch(d -> d.amount().signum() == 0));
    }
    @Test void emptySummaryHasZeroValuesAndSevenEmptyDays() {
        var report = SalesSummary.fromSales(List.of(), LocalDateTime.now());
        assertEquals(0, report.invoiceCount()); assertEquals(0, report.outstanding().signum());
        assertEquals(7, report.trend().size()); assertTrue(report.trend().stream().allMatch(d -> d.amount().signum() == 0));
    }
    @Test void missingOrInvalidAmountsAreUnavailableRatherThanInvented() {
        Sale sale = invoice(1, 100, "2026-09-03T12:00:00"); sale.setPaidAmount(null);
        assertThrows(BusinessRuleException.class, () -> SalesSummary.fromSales(List.of(sale), LocalDateTime.now()));
        sale.setPaidAmount(0.0); sale.setGrandTotal((BigDecimal) null);
        assertThrows(BusinessRuleException.class, () -> SalesSummary.fromSales(List.of(sale), LocalDateTime.now()));
        sale.setGrandTotal(new BigDecimal("-0.01"));
        assertThrows(BusinessRuleException.class, () -> SalesSummary.fromSales(List.of(sale), LocalDateTime.now()));
    }
    @Test void summaryIncludesLaterAllocationsAndUsesSelectedShop() {
        var sales = mock(SaleRepository.class); var payments = mock(PaymentRepository.class);
        var service = new SaleService(sales, mock(SaleItemRepository.class), mock(ProductRepository.class), payments,
                mock(CustomerRepository.class), mock(StockMovementService.class), mock(PlatformTransactionManager.class));
        Sale sale = invoice(7, 100, "2026-09-03T12:00:00"); sale.setPaymentMode(PaymentMode.CREDIT);
        TenantContext.setCurrentTenant(2L); when(sales.findByOrganizationId(2L)).thenReturn(List.of(sale));
        when(payments.sumByReference(ReferenceType.SALE, 7L)).thenReturn(BigDecimal.valueOf(30.0));
        when(payments.sumAllocatedToSale(7L)).thenReturn(BigDecimal.valueOf(20.0));
        assertEquals("50.00", service.salesSummary().outstanding().toPlainString());
        when(payments.sumAllocatedToSale(7L)).thenReturn(BigDecimal.valueOf(70.0));
        var settled = service.salesSummary(); assertEquals(0, settled.outstanding().signum());
        assertEquals("100.00", settled.paymentsApplied().toPlainString()); verify(sales, never()).findAll();
        TenantContext.clear(); assertThrows(AccessDeniedException.class, service::salesSummary);
    }
    @Test void cursorBoundsResultsAndRejectsInvalidInput() {
        var repo = mock(StockMovementRepository.class); var service = new StockMovementService(repo);
        assertThrows(AccessDeniedException.class, () -> service.movementPage(null, 50));
        TenantContext.setCurrentTenant(2L);
        assertThrows(BusinessRuleException.class, () -> service.movementPage(null, 101));
        assertThrows(BusinessRuleException.class, () -> service.movementPage(0L, 50));
        assertThrows(BusinessRuleException.class, () -> service.movementPage(null, 0));
        List<StockMovement> rows = new ArrayList<>();
        for (long id = 90; id >= 40; id--) { var row = new StockMovement(); row.setId(id); rows.add(row); }
        when(repo.findByOrganizationIdAndIdLessThanOrderByIdDesc(eq(2L), eq(Long.MAX_VALUE), any())).thenReturn(rows);
        var page = service.movementPage(null, 50); assertEquals(50, page.items().size()); assertEquals(41L, page.nextBeforeId());
        verify(repo).findByOrganizationIdAndIdLessThanOrderByIdDesc(eq(2L), eq(Long.MAX_VALUE), argThat(p -> p.getPageSize() == 51));
        when(repo.findByOrganizationIdAndIdLessThanOrderByIdDesc(eq(2L), eq(41L), any())).thenReturn(List.of(rows.get(50)));
        var last = service.movementPage(41L, 50); assertEquals(40L, last.items().get(0).getId()); assertNull(last.nextBeforeId());
    }
    @Test void platformDoesNotClaimHealthOrActivityWithoutMonitoring() {
        var orgs = mock(OrganizationRepository.class); var users = mock(UserRepository.class);
        when(orgs.count()).thenReturn(3L); when(users.count()).thenReturn(0L);
        var controller = new MasterAdminController(orgs, users, mock(OrganizationMembershipRepository.class),
                mock(PasswordEncoder.class), mock(PermissionService.class));
        var summary = controller.getPlatformSummary().getBody(); assertNotNull(summary);
        assertEquals(3L, summary.get("totalOrganizations")); assertEquals(0L, summary.get("totalUsers"));
        assertEquals("NOT_MONITORED", summary.get("platformHealth")); assertNull(summary.get("activeOrganizations"));
        assertNull(summary.get("systemVersion"));
    }
}
