package com.riceerp.backend.service;

import com.riceerp.backend.controller.VisitController;
import com.riceerp.backend.dto.*;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.exception.*;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VisitServiceTest {
    final VisitScheduleRepository schedules = mock(VisitScheduleRepository.class);
    final VisitCheckInRepository visits = mock(VisitCheckInRepository.class);
    final SaleRepository sales = mock(SaleRepository.class);
    final PaymentRepository payments = mock(PaymentRepository.class);
    final VisitService service = new VisitService(schedules, visits, sales, payments);
    final VisitSchedule schedule = new VisitSchedule();
    final VisitCheckIn visit = new VisitCheckIn();
    final Customer customer = new Customer();
    final CheckOutRequestDto checkout = new CheckOutRequestDto();

    @BeforeEach void setup() {
        TenantContext.setCurrentTenant(2L);
        User owner = new User(); ReflectionTestUtils.setField(owner, "id", 7L);
        ReflectionTestUtils.setField(customer, "id", 3L);
        schedule.setId(10L); schedule.setOrganizationId(2L); schedule.setSalesperson(owner); schedule.setCustomer(customer);
        schedule.setScheduledDate(LocalDate.now());
        visit.setId(20L); visit.setOrganizationId(2L); visit.setVisitSchedule(schedule);
        visit.setSalesperson(owner); visit.setCustomer(customer); visit.setCheckInTime(LocalDateTime.now());
        checkout.setOutcome(VisitOutcome.NO_ORDER);
        when(schedules.findForUpdate(10L, 2L)).thenReturn(Optional.of(schedule));
        when(visits.findScheduleIdForCheckOut(20L, 2L)).thenReturn(Optional.of(10L));
        when(visits.findForUpdateBySchedule(10L, 2L)).thenReturn(Optional.of(visit));
    }
    @AfterEach void cleanup() { TenantContext.clear(); SecurityContextHolder.clearContext(); }

    void untouched() {
        assertNull(visit.getCheckOutTime());
        verify(visits, never()).save(any()); verify(schedules, never()).save(any());
    }
    @Test void otherSalespersonCannotCheckIn() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.checkIn(10L, 8L, new CheckInRequestDto()));
        untouched();
    }
    @Test void checkInRejectedWhenScheduledDateIsNotToday() {
        schedule.setScheduledDate(LocalDate.now().plusDays(1));
        assertThrows(BusinessRuleException.class,
                () -> service.checkIn(10L, 7L, new CheckInRequestDto()));
        untouched();
    }
    @Test void checkInRejectedWhenScheduledDateIsNull() {
        schedule.setScheduledDate(null);
        assertThrows(BusinessRuleException.class,
                () -> service.checkIn(10L, 7L, new CheckInRequestDto()));
        untouched();
    }
    @Test void otherSalespersonCannotCheckOut() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.checkOut(20L, 8L, checkout));
        untouched();
    }
    @Test void ownerCanCheckInOnce() {
        when(visits.findForUpdateBySchedule(10L, 2L)).thenReturn(Optional.empty());
        VisitCheckIn result = service.checkIn(10L, 7L, new CheckInRequestDto());
        assertSame(schedule.getSalesperson(), result.getSalesperson()); assertNotNull(result.getCheckInTime());
        verify(visits).save(result);
        when(visits.findForUpdateBySchedule(10L, 2L)).thenReturn(Optional.of(result));
        assertThrows(BusinessRuleException.class, () -> service.checkIn(10L, 7L, new CheckInRequestDto()));
        verify(visits, times(1)).save(any());
    }
    @ParameterizedTest @EnumSource(value = VisitStatus.class, names = {"COMPLETED", "MISSED"})
    void closedScheduleRejectsBothMutations(VisitStatus status) {
        schedule.setStatus(status);
        assertThrows(BusinessRuleException.class, () -> service.checkIn(10L, 7L, new CheckInRequestDto()));
        assertThrows(BusinessRuleException.class, () -> service.checkOut(20L, 7L, checkout));
        untouched();
    }
    @Test void completedCheckoutCannotBeOverwritten() {
        service.checkOut(20L, 7L, checkout);
        LocalDateTime completedAt = visit.getCheckOutTime();
        checkout.setNotes("replacement");
        assertThrows(BusinessRuleException.class, () -> service.checkOut(20L, 7L, checkout));
        assertEquals(completedAt, visit.getCheckOutTime()); assertNull(visit.getNotes());
        assertEquals(VisitStatus.COMPLETED, schedule.getStatus());
        verify(visits, times(1)).save(any());
    }
    @Test void alreadyClosedCheckInRejectedEvenIfSchedulePending() {
        LocalDateTime time = LocalDateTime.now(); visit.setCheckOutTime(time);
        assertThrows(BusinessRuleException.class, () -> service.checkOut(20L, 7L, checkout));
        assertEquals(time, visit.getCheckOutTime()); verify(visits, never()).save(any());
    }
    @Test void checkoutWithoutCheckInTimeRejected() {
        visit.setCheckInTime(null);
        assertThrows(BusinessRuleException.class, () -> service.checkOut(20L, 7L, checkout)); untouched();
    }
    @Test void selectedOrganizationRequired() {
        TenantContext.clear();
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.checkIn(10L, 7L, new CheckInRequestDto()));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.checkOut(20L, 7L, checkout));
        verifyNoInteractions(schedules, visits);
    }
    @Test void anotherOrganizationCannotAddressVisitIds() {
        TenantContext.setCurrentTenant(9L);
        assertThrows(NotFoundException.class, () -> service.checkIn(10L, 7L, new CheckInRequestDto()));
        assertThrows(NotFoundException.class, () -> service.checkOut(20L, 7L, checkout)); untouched();
    }
    @Test void linkedSaleMustExistInCurrentOrganization() {
        checkout.setSaleId(30L);
        assertThrows(IllegalArgumentException.class, () -> service.checkOut(20L, 7L, checkout)); untouched();
        verify(sales).findByIdAndOrganizationId(30L, 2L);
    }
    @Test void linkedSaleMustHaveSameCustomer() {
        Sale sale = new Sale(); sale.setCustomer(new Customer());
        when(sales.findByIdAndOrganizationId(30L, 2L)).thenReturn(Optional.of(sale)); checkout.setSaleId(30L);
        assertThrows(IllegalArgumentException.class, () -> service.checkOut(20L, 7L, checkout)); untouched();
    }
    @Test void linkedPaymentMustExistInCurrentOrganization() {
        checkout.setPaymentId(40L);
        assertThrows(IllegalArgumentException.class, () -> service.checkOut(20L, 7L, checkout)); untouched();
    }
    @ParameterizedTest @EnumSource(value = ReferenceType.class, names = {"PURCHASE", "SUPPLIER"})
    void unrelatedPaymentTypesRejected(ReferenceType type) {
        Payment payment = new Payment(); payment.setReferenceType(type); payment.setReferenceId(3L);
        when(payments.findByIdAndOrganizationId(40L, 2L)).thenReturn(Optional.of(payment)); checkout.setPaymentId(40L);
        assertThrows(IllegalArgumentException.class, () -> service.checkOut(20L, 7L, checkout)); untouched();
    }
    @Test void customerPaymentForAnotherCustomerRejected() {
        Payment payment = new Payment(); payment.setReferenceType(ReferenceType.CUSTOMER); payment.setReferenceId(99L);
        when(payments.findByIdAndOrganizationId(40L, 2L)).thenReturn(Optional.of(payment)); checkout.setPaymentId(40L);
        assertThrows(IllegalArgumentException.class, () -> service.checkOut(20L, 7L, checkout)); untouched();
    }
    @Test void matchingSaleAndPaymentAccepted() {
        Sale sale = new Sale(); sale.setCustomer(customer);
        Payment payment = new Payment(); payment.setReferenceType(ReferenceType.SALE); payment.setReferenceId(30L);
        when(sales.findByIdAndOrganizationId(30L, 2L)).thenReturn(Optional.of(sale));
        when(payments.findByIdAndOrganizationId(40L, 2L)).thenReturn(Optional.of(payment));
        checkout.setSaleId(30L); checkout.setPaymentId(40L);
        service.checkOut(20L, 7L, checkout);
        assertEquals(30L, visit.getSaleId()); assertEquals(40L, visit.getPaymentId());
        assertEquals(VisitStatus.COMPLETED, schedule.getStatus());
    }
    @Test void salePaymentMustReferenceSelectedSale() {
        Sale sale = new Sale(); sale.setCustomer(customer);
        Payment payment = new Payment(); payment.setReferenceType(ReferenceType.SALE); payment.setReferenceId(31L);
        when(sales.findByIdAndOrganizationId(30L, 2L)).thenReturn(Optional.of(sale));
        when(sales.findByIdAndOrganizationId(31L, 2L)).thenReturn(Optional.of(sale));
        when(payments.findByIdAndOrganizationId(40L, 2L)).thenReturn(Optional.of(payment));
        checkout.setSaleId(30L); checkout.setPaymentId(40L);
        assertThrows(IllegalArgumentException.class, () -> service.checkOut(20L, 7L, checkout)); untouched();
    }
    @Test void viewingPermissionCannotMutateThroughHttp() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(7L, null, List.of(new SimpleGrantedAuthority("beat-plan:view")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        ProxyFactory proxy = new ProxyFactory(service);
        proxy.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());
        var mvc = MockMvcBuilders.standaloneSetup(new VisitController((VisitService) proxy.getProxy()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/visits/10/check-in").principal(authentication).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/visits/20/check-out").principal(authentication).contentType("application/json").content("{\"outcome\":\"NO_ORDER\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(visits, schedules);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(7L, null,
                List.of(new SimpleGrantedAuthority("visit:execute"))));
        mvc.perform(put("/visits/20/check-out").principal(authentication).contentType("application/json").content("{\"outcome\":\"NO_ORDER\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("NO_ORDER"));
    }
    @Test void defaultViewOnlyRolesCannotExecuteVisits() {
        assertTrue(DefaultPermissionMatrix.isDefaultAllowed(OrgRole.SALES, "visit:execute"));
        assertTrue(DefaultPermissionMatrix.isDefaultAllowed(OrgRole.ADMIN, "visit:execute"));
        assertTrue(DefaultPermissionMatrix.isDefaultAllowed(OrgRole.MANAGER, "visit:execute"));
        assertFalse(DefaultPermissionMatrix.isDefaultAllowed(OrgRole.ACCOUNTANT, "visit:execute"));
        assertFalse(DefaultPermissionMatrix.isDefaultAllowed(OrgRole.WAREHOUSE, "visit:execute"));
    }

    @Test void newPermissionSeedingPreservesExistingVisitRestriction() {
        RolePermissionRepository repository = mock(RolePermissionRepository.class);
        when(repository.lockOrganization(2L)).thenReturn(Optional.of(new Organization()));
        when(repository.findByOrganizationIdAndOrgRoleAndPermission(2L, OrgRole.SALES, "beat-plan:view"))
                .thenReturn(Optional.of(new RolePermission(2L, OrgRole.SALES, "beat-plan:view", false)));
        new PermissionService(repository).seedPermissionsForOrg(2L);
        verify(repository).save(argThat(row -> row.getOrgRole() == OrgRole.SALES &&
                row.getPermission().equals("visit:execute") && !row.isAllowed()));
    }
}
