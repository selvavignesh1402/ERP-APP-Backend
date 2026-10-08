package com.riceerp.backend;

import com.riceerp.backend.controller.*;
import com.riceerp.backend.dto.*;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.repository.*;
import com.riceerp.backend.security.*;
import com.riceerp.backend.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class AccountAndReservationIntegrationTest {
    @Autowired AuthController auth;
    @Autowired OrganizationController team;
    @Autowired InviteService invitations;
    @Autowired PermissionService permissions;
    @Autowired RolePermissionRepository permissionRows;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @SpyBean OrganizationMembershipRepository memberships;
    @Autowired OrganizationInviteRepository invites;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired SalesOrderService orders;
    @Autowired DeliveryService delivery;
    @Autowired SaleService sales;
    @Autowired SalesOrderRepository orderRows;
    @Autowired BeatPlanService plans;
    @Autowired VisitScheduleRepository schedules;
    @Autowired VisitCheckInRepository checkIns;
    @Autowired SaleRepository saleRows;
    @Autowired SaleItemRepository saleItems;
    @Autowired ProductService productService;
    @Autowired UserController platformUsers;
    Organization org;
    User user;
    Customer customer;
    Product product;

    @BeforeEach void setup() {
        org = new Organization(); org.setName("Integration shop"); org = organizations.save(org);
        TenantContext.setCurrentTenant(org.getId());
        user = new User(); user.setName("Test admin"); user.setPhoneNumber(UUID.randomUUID().toString());
        user.setPlatformRole(PlatformRole.USER); user = users.save(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getId(), null,
                List.of(new SimpleGrantedAuthority("member:manage"))));
        customer = new Customer(); customer.setCustomerName("Customer"); customer = customers.save(customer);
        product = new Product(); product.setProductName("Rice"); product.setStock(10); product.setSellingPrice(100);
        product = products.save(product);
    }
    @AfterEach void cleanup() { TenantContext.clear(); SecurityContextHolder.clearContext(); }
    OrganizationMembership member(User person) {
        var row = new OrganizationMembership(); row.setOrganization(org); row.setUser(person); row.setRole(OrgRole.ADMIN); row.setActive(true);
        return memberships.save(row);
    }
    SalesOrderRequest order(int qty) {
        var item = new SalesOrderItemRequest(); item.setProductId(product.getId()); item.setQuantity(qty); item.setUnitPrice(100);
        var request = new SalesOrderRequest(); request.setCustomerId(customer.getId()); request.setItems(List.of(item)); return request;
    }
    @Test void failedSignupDoesNotLeaveUserOrShop() {
        long before = organizations.count(); String phone = "9" + System.nanoTime();
        var request = new SignupRequest(); request.setName("Rollback owner"); request.setPhoneNumber(phone); request.setPassword("Explicit test password!");
        doThrow(new IllegalStateException("membership write failed")).when(memberships).save(any());
        assertThrows(org.springframework.dao.InvalidDataAccessApiUsageException.class, () -> auth.signupWithPassword(request));
        assertFalse(users.existsByPhoneNumber(phone)); assertEquals(before, organizations.count());
    }
    @Test void failedInviteMembershipDoesNotConsumeInvite() {
        var invite = invitations.createInvite(org, user, user.getPhoneNumber(), OrgRole.SALES);
        doThrow(new IllegalStateException("membership write failed")).when(memberships).saveAndFlush(any());
        assertThrows(org.springframework.dao.InvalidDataAccessApiUsageException.class, () -> invitations.acceptInvite(invite.getToken(), user));
        assertEquals("PENDING", invites.findByToken(invite.getToken()).orElseThrow().getStatus());
        assertTrue(memberships.findByUserIdAndOrganizationId(user.getId(), org.getId()).isEmpty());
    }
    @Test void permissionBatchRollsBackAndSeparateServiceReadsChangesImmediately() {
        permissions.seedPermissionsForOrg(org.getId());
        var secondServer = new PermissionService(permissionRows);
        assertTrue(secondServer.getEffectivePermissions(org.getId(), OrgRole.SALES).contains("sale:create"));
        List<Map<String,Object>> invalid = List.of(Map.of("role","SALES","permission","sale:create","allowed",false),
                Map.of("role","ADMIN","permission","member:manage","allowed",false));
        assertThrows(IllegalArgumentException.class, () -> permissions.updateMatrix(org.getId(), invalid));
        assertTrue(secondServer.getEffectivePermissions(org.getId(), OrgRole.SALES).contains("sale:create"));
        permissions.updateMatrix(org.getId(), List.of(invalid.get(0)));
        assertFalse(secondServer.getEffectivePermissions(org.getId(), OrgRole.SALES).contains("sale:create"));
    }
    @Test void concurrentDemotionsLeaveOneActiveOrganizationAdmin() throws Exception {
        var first = member(user); User other = new User(); other.setName("Other admin"); other.setPhoneNumber(UUID.randomUUID().toString());
        other.setPlatformRole(PlatformRole.USER); other = users.save(other); var second = member(other);
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (Long id : List.of(first.getId(), second.getId())) results.add(pool.submit(() -> {
                TenantContext.setCurrentTenant(org.getId());
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getId(), null,
                        List.of(new SimpleGrantedAuthority("member:manage"))));
                try { start.await(); team.updateMemberRole(id, Map.of("role","SALES")); return true; }
                catch (BusinessRuleException expected) { return false; }
                finally { TenantContext.clear(); SecurityContextHolder.clearContext(); }
            }));
            start.countDown(); int successes = 0; for (var result : results) if (result.get(20, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
            assertEquals(1, memberships.findByOrganizationId(org.getId()).stream().filter(m -> m.isActive() && m.getRole() == OrgRole.ADMIN).count());
        } finally { pool.shutdownNow(); }
    }
    @Test void confirmationReservesWithoutReducingStockAndCancellationReleasesIt() {
        var confirmed = orders.createSalesOrder(order(8));
        assertEquals(10, products.findById(product.getId()).orElseThrow().getStock()); assertEquals(8, products.reservedQuantity(product.getId()));
        var line = new SaleItemRequest(); line.setProductId(product.getId()); line.setQuantity(3); line.setPrice(100);
        var request = new SaleRequest(); request.setItems(List.of(line)); request.setPaymentMode("CASH");
        assertThrows(BusinessRuleException.class, () -> sales.createSale(request));
        orders.cancelSalesOrder(confirmed.getId(), "Customer cancelled"); assertEquals(0, products.reservedQuantity(product.getId()));
        sales.createSale(request); assertEquals(7, products.findById(product.getId()).orElseThrow().getStock());
    }
    @Test void partialDeliveryReleasesOnlyDeliveredReservationAndDeductsPhysicalStock() {
        var confirmed = orders.createSalesOrder(order(8));
        var dispatchLine = new DeliveryItemCreateRequest(); dispatchLine.setProductId(product.getId()); dispatchLine.setDeliveringQuantity(8);
        var dispatch = new DeliveryCreateRequest(); dispatch.setSalesOrderId(confirmed.getId()); dispatch.setItems(List.of(dispatchLine));
        var note = delivery.createDeliveryNote(dispatch); delivery.startDelivery(note.getId(), user.getId(), true);
        var actual = new DeliveryItemConfirmRequest(); actual.setProductId(product.getId()); actual.setDeliveredQuantity(3);
        var confirmation = new DeliveryConfirmRequest(); confirmation.setReceiverName("Receiver"); confirmation.setItems(List.of(actual));
        confirmation.setPaymentMode(PaymentMode.CASH); delivery.confirmDelivery(note.getId(), confirmation, user.getId(), true);
        assertEquals(7, products.findById(product.getId()).orElseThrow().getStock()); assertEquals(5, products.reservedQuantity(product.getId()));
    }
    @Test void invoiceIdentitySurvivesMasterDataEdits() {
        customer.setPhone("111"); customer.setAddress("Original address"); customers.save(customer);
        product.setUnit("kg"); products.save(product);
        var line = new SaleItemRequest(); line.setProductId(product.getId()); line.setQuantity(1); line.setPrice(100);
        var request = new SaleRequest(); request.setCustomerId(customer.getId()); request.setItems(List.of(line)); request.setPaymentMode("CASH");
        var invoice = sales.createSale(request);
        customer = customers.findById(customer.getId()).orElseThrow();
        customer.setCustomerName("Renamed"); customer.setPhone("222"); customers.save(customer);
        var currentProduct = products.findById(product.getId()).orElseThrow(); currentProduct.setProductName("Different rice"); currentProduct.setUnit("bag"); products.save(currentProduct);
        org.setName("Different shop"); organizations.save(org);
        var saved = saleRows.findById(invoice.getId()).orElseThrow(); var item = saleItems.findBySaleId(invoice.getId()).get(0);
        assertEquals("Customer", saved.getCustomerName()); assertEquals("111", saved.getCustomerPhone());
        assertEquals("Original address", saved.getCustomerAddress()); assertEquals("Integration shop", saved.getShopName());
        assertEquals("Rice", item.getProductName()); assertEquals("kg", item.getUnit());
        assertEquals(0, new java.math.BigDecimal("100").compareTo(item.getPrice()));
    }
    @Test void catalogSeparatelyExposesReservedAndAvailablePhysicalStock() {
        orders.createSalesOrder(order(8));
        var loaded = productService.listProducts(null, null).stream().filter(p -> p.getId().equals(product.getId())).findFirst().orElseThrow();
        assertEquals(10, loaded.getStock()); assertEquals(8, loaded.getReservedStock()); assertEquals(2, loaded.getAvailableStock());
    }
    @Test void routeEditsReplaceUnstartedSchedulesButPreserveCheckedInVisits() {
        member(user);
        var original = plan(customer.getId(), 1);
        var created = plans.createBeatPlan(original);
        var today = java.time.LocalDate.now();
        var first = schedules.findBySalespersonIdAndScheduledDateOrderByVisitOrderAsc(user.getId(), today).get(0);
        var checkIn = new VisitCheckIn(); checkIn.setVisitSchedule(first); checkIn.setCustomer(customer);
        checkIn.setSalesperson(user); checkIn.setCheckInTime(java.time.LocalDateTime.now()); checkIns.save(checkIn);
        var nextCustomer = new Customer(); nextCustomer.setCustomerName("Next customer"); nextCustomer = customers.save(nextCustomer);
        plans.updateBeatPlan(created.getId(), plan(nextCustomer.getId(), 3));
        var after = schedules.findBySalespersonIdAndScheduledDateOrderByVisitOrderAsc(user.getId(), today);
        assertEquals(2, after.size()); assertTrue(schedules.findById(first.getId()).isPresent());
        plans.updateBeatPlan(created.getId(), plan(nextCustomer.getId(), 7));
        after = schedules.findBySalespersonIdAndScheduledDateOrderByVisitOrderAsc(user.getId(), today);
        assertEquals(2, after.size());
        Long nextId = nextCustomer.getId();
        assertEquals(7, after.stream().filter(v -> v.getCustomer().getId().equals(nextId)).findFirst().orElseThrow().getVisitOrder());
        var inactive = plan(nextCustomer.getId(), 7); inactive.setActive(false); plans.updateBeatPlan(created.getId(), inactive);
        assertEquals(1, schedules.findBySalespersonIdAndScheduledDateOrderByVisitOrderAsc(user.getId(), today).size());
    }
    BeatPlanDto plan(Long customerId, int order) {
        var entry = new BeatPlanDto.EntryDto(); entry.setCustomerId(customerId); entry.setDayOfWeek(java.time.LocalDate.now().getDayOfWeek()); entry.setVisitOrder(order);
        var dto = new BeatPlanDto(); dto.setName("Test route"); dto.setSalespersonId(user.getId()); dto.setActive(true); dto.setEntries(List.of(entry)); return dto;
    }
    @Test void overdueUnstartedVisitsBecomeMissedAndHistoricalRouteUsesRequestedDate() {
        member(user); plans.createBeatPlan(plan(customer.getId(), 1));
        var today = java.time.LocalDate.now();
        var schedule = schedules.findBySalespersonIdAndScheduledDateOrderByVisitOrderAsc(user.getId(), today).get(0);
        schedule.setScheduledDate(today.minusDays(2)); schedules.save(schedule);
        var route = plans.getRoute(user.getId(), today.minusDays(2));
        assertEquals(1, route.size()); assertEquals(VisitStatus.MISSED, route.get(0).getStatus());
        assertEquals(1, schedules.findBySalespersonIdAndScheduledDateOrderByVisitOrderAsc(user.getId(), today.minusDays(2)).size());
    }
    @Test void routeUsesCustomersLastSaleEvenWhenOtherCustomersHaveMoreThanFiveNewerSales() {
        member(user); plans.createBeatPlan(plan(customer.getId(), 1));
        var old = new Sale(); old.setBillNumber(UUID.randomUUID().toString()); old.setCustomer(customer); old.setPaymentMode(PaymentMode.CASH);
        old.setSaleDate(java.time.LocalDateTime.now().minusDays(4)); old.setGrandTotal(123); saleRows.save(old);
        for (int i=0;i<6;i++) { var sale = new Sale(); sale.setBillNumber(UUID.randomUUID().toString()); sale.setPaymentMode(PaymentMode.CASH); saleRows.save(sale); }
        assertEquals(0, new java.math.BigDecimal("123").compareTo(plans.getTodayRoute(user.getId()).get(0).getLastOrderAmount()));
    }
    @Test void assignmentRejectsUserWithoutActiveMembershipInSelectedShop() {
        var request = order(2); request.setSalespersonId(user.getId());
        assertThrows(BusinessRuleException.class, () -> orders.createSalesOrder(request));
        assertThrows(BusinessRuleException.class, () -> plans.createBeatPlan(plan(customer.getId(), 1)));
        member(user); assertDoesNotThrow(() -> orders.createSalesOrder(request));
    }
    @Test void simultaneousOrdersCannotReserveMoreThanPhysicalStock() throws Exception {
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i=0;i<2;i++) results.add(pool.submit(() -> { TenantContext.setCurrentTenant(org.getId());
                try { start.await(); orders.createSalesOrder(order(6)); return true; }
                catch (BusinessRuleException expected) { return false; } finally { TenantContext.clear(); }
            }));
            start.countDown(); int successes=0; for(var result:results) if(result.get(20,TimeUnit.SECONDS)) successes++;
            assertEquals(1,successes); assertEquals(6, products.reservedQuantity(product.getId()));
        } finally { pool.shutdownNow(); }
    }
}
