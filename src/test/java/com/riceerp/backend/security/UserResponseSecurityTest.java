package com.riceerp.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceerp.backend.controller.DeliveryController;
import com.riceerp.backend.controller.SalesOrderController;
import com.riceerp.backend.controller.VisitController;
import com.riceerp.backend.dto.LoginRequest;
import com.riceerp.backend.dto.SignupRequest;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.service.DeliveryService;
import com.riceerp.backend.service.SalesOrderService;
import com.riceerp.backend.service.VisitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Response serialization tests; repositories and the security filter chain are not exercised. */
class UserResponseSecurityTest {
    private static final String HASH_SENTINEL = "AUDIT_STORED_HASH_MUST_NEVER_LEAVE_SERVER";
    private final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    private final ObjectMapper mapper = converter.getObjectMapper();

    private static User staff() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setName("Test staff");
        user.setPhoneNumber("9000000000");
        user.setPasswordHash(HASH_SENTINEL);
        return user;
    }

    private static SalesOrder order() {
        SalesOrder order = new SalesOrder();
        order.setSalesperson(staff());
        return order;
    }

    private static Delivery delivery() {
        Delivery delivery = new Delivery();
        delivery.setDeliveryPerson(staff());
        delivery.setSalesOrder(order());
        return delivery;
    }

    private static VisitCheckIn visit() {
        BeatPlan plan = new BeatPlan();
        plan.setSalesperson(staff());
        VisitSchedule schedule = new VisitSchedule();
        schedule.setSalesperson(staff());
        schedule.setBeatPlan(plan);
        VisitCheckIn visit = new VisitCheckIn();
        visit.setSalesperson(staff());
        visit.setVisitSchedule(schedule);
        return visit;
    }

    static Stream<Arguments> userBearingEntities() {
        OrganizationMembership membership = new OrganizationMembership();
        membership.setUser(staff());
        OrganizationInvite invite = new OrganizationInvite();
        invite.setInvitedBy(staff());
        VisitCheckIn visit = visit();
        return Stream.of(
                Arguments.of("user", staff(), ""),
                Arguments.of("sales order", order(), "/salesperson"),
                Arguments.of("delivery", delivery(), "/deliveryPerson"),
                Arguments.of("visit check-in", visit, "/salesperson"),
                Arguments.of("visit schedule", visit.getVisitSchedule(), "/salesperson"),
                Arguments.of("beat plan", visit.getVisitSchedule().getBeatPlan(), "/salesperson"),
                Arguments.of("membership", membership, "/user"),
                Arguments.of("invitation", invite, "/invitedBy"));
    }

    @ParameterizedTest(name = "{0} excludes credentials and preserves staff details")
    @MethodSource("userBearingEntities")
    void entityResponsesNeverExposeCredentials(String label, Object entity, String userPointer) throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(entity));
        assertSafeResponse(json);
        assertStaffDetails(json.at(userPointer));
    }

    @Test
    void salesOrderListAndDetailResponsesAreSafe() throws Exception {
        SalesOrderService service = mock(SalesOrderService.class);
        when(service.getSalesOrderById(1L)).thenReturn(order());
        when(service.listSalesOrders(null, null, null)).thenReturn(List.of(order()));
        MockMvc mvc = mvc(new SalesOrderController(service));
        assertStaffDetails(response(mvc, "/api/sales-orders/1").at("/salesperson"));
        assertStaffDetails(response(mvc, "/api/sales-orders").at("/0/salesperson"));
    }

    @Test
    void deliveryListAndDetailResponsesProtectBothDriverAndSalesperson() throws Exception {
        DeliveryService service = mock(DeliveryService.class);
        when(service.getDeliveryById(1L)).thenReturn(delivery());
        when(service.listDeliveries(null)).thenReturn(List.of(delivery()));
        MockMvc mvc = mvc(new DeliveryController(service));
        JsonNode detail = response(mvc, "/api/deliveries/1");
        assertStaffDetails(detail.at("/deliveryPerson"));
        assertStaffDetails(detail.at("/salesOrder/salesperson"));
        assertStaffDetails(response(mvc, "/api/deliveries").at("/0/deliveryPerson"));
    }

    @Test
    void visitHistoriesProtectUsersInScheduleAndBeatPlan() throws Exception {
        VisitService service = mock(VisitService.class);
        when(service.getVisitHistory(1L)).thenReturn(List.of(visit()));
        when(service.getSalespersonHistory(7L)).thenReturn(List.of(visit()));
        MockMvc mvc = mvc(new VisitController(service));
        JsonNode history = response(mvc, "/visits/customer/1/history");
        assertStaffDetails(history.at("/0/salesperson"));
        assertStaffDetails(history.at("/0/visitSchedule/salesperson"));
        assertStaffDetails(history.at("/0/visitSchedule/beatPlan/salesperson"));
        assertStaffDetails(response(mvc, "/visits/salesperson/7/history").at("/0/salesperson"));
    }

    @Test
    void credentialProtectionDoesNotRemoveInternalHashOrPasswordRequestBinding() throws Exception {
        User user = staff();
        mapper.writeValueAsString(user);
        assertEquals(HASH_SENTINEL, user.getPasswordHash());
        assertEquals(HASH_SENTINEL, user.getPassword());
        String request = "{\"phoneNumber\":\"9000000000\",\"password\":\"Explicit test password!\"}";
        assertEquals("Explicit test password!", mapper.readValue(request, LoginRequest.class).getPassword());
        assertEquals("Explicit test password!", mapper.readValue(request, SignupRequest.class).getPassword());
    }

    private MockMvc mvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller).setMessageConverters(converter).build();
    }

    private JsonNode response(MockMvc mvc, String path) throws Exception {
        String body = mvc.perform(get(path)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(body);
        assertSafeResponse(json);
        return json;
    }

    private void assertSafeResponse(JsonNode json) {
        assertFalse(json.toString().contains(HASH_SENTINEL), "Stored credential value leaked");
        assertTrue(json.findValues("password").isEmpty(), "password field leaked");
        assertTrue(json.findValues("passwordHash").isEmpty(), "passwordHash field leaked");
    }

    private void assertStaffDetails(JsonNode user) {
        assertEquals(7L, user.path("id").asLong());
        assertEquals("Test staff", user.path("name").asText());
        assertEquals("9000000000", user.path("phoneNumber").asText());
        assertTrue(user.path("active").asBoolean());
    }
}
