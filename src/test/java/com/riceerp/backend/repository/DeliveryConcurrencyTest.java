package com.riceerp.backend.repository;

import com.riceerp.backend.dto.*;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.exception.BusinessRuleException;
import com.riceerp.backend.security.*;
import com.riceerp.backend.service.*;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeliveryConcurrencyTest {
    private Configuration config(String name) {
        return new Configuration().addAnnotatedClass(Organization.class).addAnnotatedClass(User.class)
                .addAnnotatedClass(Customer.class).addAnnotatedClass(Product.class).addAnnotatedClass(SalesOrder.class).addAnnotatedClass(Sale.class)
                .addAnnotatedClass(SalesOrderItem.class).addAnnotatedClass(Delivery.class).addAnnotatedClass(DeliveryItem.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
                .setProperty("hibernate.connection.isolation", "2")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
    }
    private DeliveryService service(Session session, SaleService sales) {
        var repos = new JpaRepositoryFactory(session);
        return new DeliveryService(repos.getRepository(DeliveryRepository.class), repos.getRepository(DeliveryItemRepository.class),
                repos.getRepository(SalesOrderRepository.class), repos.getRepository(SalesOrderItemRepository.class),
                repos.getRepository(ProductRepository.class), repos.getRepository(UserRepository.class), sales,
                repos.getRepository(OrganizationRepository.class), repos.getRepository(SaleRepository.class));
    }
    private long[] seed(Session session, boolean withDelivery) {
        var tx = session.beginTransaction();
        Organization org = new Organization(); org.setName("Shop"); session.persist(org);
        Customer customer = new Customer(); customer.setCustomerName("Customer"); session.persist(customer);
        Product product = new Product(); product.setProductName("Rice"); product.setStock(10); session.persist(product);
        SalesOrder order = new SalesOrder(); order.setOrderNumber("SO-1"); order.setCustomer(customer);
        order.setStatus(SalesOrderStatus.CONFIRMED); order.setSubtotal(1000);
        SalesOrderItem line = new SalesOrderItem(); line.setProduct(product); line.setSalesOrder(order);
        line.setOrderedQuantity(10); line.setRemainingQuantity(10); line.setUnitPrice(100);
        order.setItems(List.of(line)); session.persist(order);
        Delivery note = new Delivery();
        if (withDelivery) {
            note.setDeliveryNumber("DN-1"); note.setSalesOrder(order); note.setStatus(DeliveryStatus.OUT_FOR_DELIVERY);
            DeliveryItem item = new DeliveryItem(); item.setDelivery(note); item.setProduct(product);
            item.setOrderedQuantity(10); item.setDeliveringQuantity(6); item.setUnitPrice(100);
            note.setItems(List.of(item)); session.persist(note); line.setPackedQuantity(6);
        }
        tx.commit(); return new long[]{org.getId(), order.getId(), product.getId(), withDelivery ? note.getId() : 0};
    }
    private DeliveryConfirmRequest confirm(long productId) {
        var line = new DeliveryItemConfirmRequest(); line.setProductId(productId); line.setDeliveredQuantity(3);
        var request = new DeliveryConfirmRequest(); request.setReceiverName("Customer"); request.setItems(List.of(line)); return request;
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void concurrentConfirmationOrPackingIsSerialized(boolean confirming) throws Exception {
        try (var factory = config("delivery_concurrent_" + confirming).buildSessionFactory()) {
            long[] ids;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) { ids = seed(session, confirming); }
            SaleService sales = mock(SaleService.class); Sale invoice = new Sale(); invoice.setId(9L);
            when(sales.createSaleFromDelivery(any(), any(), any(), anyList(), any(), any(java.math.BigDecimal.class), any(), anyMap())).thenReturn(invoice);
            var executor = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
            try {
                List<Future<Boolean>> results = new ArrayList<>();
                for (int i = 0; i < 2; i++) results.add(executor.submit(() -> {
                    TenantContext.setCurrentTenant(ids[0]);
                    try (var session = factory.withOptions().tenantIdentifier(ids[0]).openSession()) {
                        var tx = session.beginTransaction(); var service = service(session, sales); start.await();
                        try {
                            if (confirming) service.confirmDelivery(ids[3], confirm(ids[2]), 7L, true);
                            else {
                                var line = new DeliveryItemCreateRequest(); line.setProductId(ids[2]); line.setDeliveringQuantity(6);
                                var request = new DeliveryCreateRequest(); request.setSalesOrderId(ids[1]); request.setItems(List.of(line));
                                service.createDeliveryNote(request);
                            }
                            tx.commit(); return true;
                        } catch (BusinessRuleException ex) { tx.rollback(); return false; }
                    } finally { TenantContext.clear(); }
                }));
                start.countDown(); int succeeded = 0;
                for (var result : results) if (result.get(30, TimeUnit.SECONDS)) succeeded++;
                assertEquals(1, succeeded);
                try (var session = factory.withOptions().tenantIdentifier(ids[0]).openSession()) {
                    SalesOrder order = session.find(SalesOrder.class, ids[1]);
                    assertEquals(confirming ? 3 : 0, order.getItems().get(0).getDeliveredQuantity());
                    assertEquals(confirming ? 3 : 6, order.getItems().get(0).getPackedQuantity());
                    assertEquals(1L, session.createQuery("select count(d) from Delivery d", Long.class).getSingleResult());
                    if (confirming) assertEquals(9L, session.find(Delivery.class, ids[3]).getGeneratedInvoiceId());
                }
                if (confirming) verify(sales).createSaleFromDelivery(eq(ids[3]), eq(ids[1]), any(), anyList(), any(), any(java.math.BigDecimal.class), any(), anyMap());
                else verifyNoInteractions(sales);
            } finally { executor.shutdownNow(); }
        }
    }
    @Test void invoiceFailureRollsBackDeliveryAndOrderQuantities() {
        try (var factory = config("delivery_rollback").buildSessionFactory()) {
            long[] ids;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) { ids = seed(session, true); }
            SaleService sales = mock(SaleService.class);
            when(sales.createSaleFromDelivery(any(), any(), any(), anyList(), any(), any(java.math.BigDecimal.class), any(), anyMap()))
                    .thenThrow(new BusinessRuleException("Insufficient stock"));
            TenantContext.setCurrentTenant(ids[0]);
            try (var session = factory.withOptions().tenantIdentifier(ids[0]).openSession()) {
                var tx = session.beginTransaction();
                assertThrows(BusinessRuleException.class, () -> service(session, sales).confirmDelivery(ids[3], confirm(ids[2]), 7L, true));
                tx.rollback();
            } finally { TenantContext.clear(); }
            try (var session = factory.withOptions().tenantIdentifier(ids[0]).openSession()) {
                Delivery note = session.find(Delivery.class, ids[3]);
                assertEquals(DeliveryStatus.OUT_FOR_DELIVERY, note.getStatus()); assertNull(note.getDeliveredAt());
                assertNull(note.getGeneratedInvoiceId()); assertEquals(0, note.getItems().get(0).getDeliveredQuantity());
                var line = note.getSalesOrder().getItems().get(0);
                assertEquals(0, line.getDeliveredQuantity()); assertEquals(10, line.getRemainingQuantity()); assertEquals(6, line.getPackedQuantity());
            }
        }
    }
    @Test void cancellationAndDispatchCannotBothCommit() throws Exception {
        try (var factory = config("cancel_dispatch").buildSessionFactory()) {
            long[] ids;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) { ids = seed(session, false); }
            var executor = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
            try {
                List<Future<Boolean>> results = new ArrayList<>();
                for (boolean cancelling : List.of(true, false)) results.add(executor.submit(() -> {
                    TenantContext.setCurrentTenant(ids[0]);
                    try (var session = factory.withOptions().tenantIdentifier(ids[0]).openSession()) {
                        var tx = session.beginTransaction(); var repos = new JpaRepositoryFactory(session); start.await();
                        try {
                            if (cancelling) {
                                new SalesOrderService(repos.getRepository(SalesOrderRepository.class), repos.getRepository(SalesOrderItemRepository.class),
                                        repos.getRepository(CustomerRepository.class), repos.getRepository(ProductRepository.class), repos.getRepository(UserRepository.class),
                                        repos.getRepository(OrganizationRepository.class), repos.getRepository(DeliveryRepository.class)).cancelSalesOrder(ids[1], "Cancel");
                            } else {
                                var line = new DeliveryItemCreateRequest(); line.setProductId(ids[2]); line.setDeliveringQuantity(6);
                                var request = new DeliveryCreateRequest(); request.setSalesOrderId(ids[1]); request.setItems(List.of(line));
                                service(session, mock(SaleService.class)).createDeliveryNote(request);
                            }
                            tx.commit(); return true;
                        } catch (BusinessRuleException ex) { tx.rollback(); return false; }
                    } finally { TenantContext.clear(); }
                }));
                start.countDown(); int commits = 0;
                for (var result : results) if (result.get(30, TimeUnit.SECONDS)) commits++;
                assertEquals(1, commits);
                try (var session = factory.withOptions().tenantIdentifier(ids[0]).openSession()) {
                    var order = session.find(SalesOrder.class, ids[1]);
                    long notes = session.createQuery("select count(d) from Delivery d", Long.class).getSingleResult();
                    assertEquals(order.getStatus() == SalesOrderStatus.CANCELLED ? 0 : 1, notes);
                }
            } finally { executor.shutdownNow(); }
        }
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void staleOrderOrDeliveryUpdatesCannotOverwriteCommittedState(boolean updatingOrder) {
        try (var factory = config("fulfillment_versions_" + updatingOrder).buildSessionFactory()) {
            long[] ids;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) { ids = seed(session, true); }
            try (var first = factory.withOptions().tenantIdentifier(ids[0]).openSession();
                 var stale = factory.withOptions().tenantIdentifier(ids[0]).openSession()) {
                var firstTx = first.beginTransaction(); var staleTx = stale.beginTransaction();
                if (updatingOrder) {
                    var current = first.find(SalesOrder.class, ids[1]); var old = stale.find(SalesOrder.class, ids[1]);
                    current.setNotes("Current"); firstTx.commit(); old.setNotes("Stale");
                } else {
                    var current = first.find(Delivery.class, ids[3]); var old = stale.find(Delivery.class, ids[3]);
                    current.setDeliveryNotes("Current"); firstTx.commit(); old.setDeliveryNotes("Stale");
                }
                assertThrows(jakarta.persistence.OptimisticLockException.class, stale::flush);
                staleTx.rollback();
            }
        }
    }
    @Test void sequentialDiscountAllocationsReconcileAgainstPersistedInvoices() {
        try (var factory = config("order_discounts").buildSessionFactory()) {
            long[] ids;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                ids = seed(session, true);
                var tx = session.beginTransaction(); session.find(SalesOrder.class, ids[1]).setDiscount(100); tx.commit();
            }
            TenantContext.setCurrentTenant(ids[0]);
            try {
                for (int quantity : List.of(3, 7)) {
                    try (var session = factory.withOptions().tenantIdentifier(ids[0]).openSession()) {
                        var tx = session.beginTransaction();
                        SaleService billing = mock(SaleService.class);
                        when(billing.createSaleFromDelivery(any(), any(), any(), anyList(), any(), any(java.math.BigDecimal.class), any(), anyMap())).thenAnswer(call -> {
                            Sale sale = new Sale(); sale.setBillNumber("BILL-" + quantity); sale.setSalesOrderId(ids[1]);
                            sale.setDeliveryId(call.getArgument(0)); sale.setTotal(quantity * 100); sale.setDiscount(call.getArgument(5, java.math.BigDecimal.class));
                            sale.setPaymentMode(PaymentMode.CREDIT); session.persist(sale); return sale;
                        });
                        DeliveryService service = service(session, billing); long deliveryId = ids[3];
                        if (quantity == 7) {
                            var line = new DeliveryItemCreateRequest(); line.setProductId(ids[2]); line.setDeliveringQuantity(7);
                            var request = new DeliveryCreateRequest(); request.setSalesOrderId(ids[1]); request.setItems(List.of(line));
                            deliveryId = service.createDeliveryNote(request).getId(); service.startDelivery(deliveryId, 7L, true);
                        }
                        var request = confirm(ids[2]); request.getItems().get(0).setDeliveredQuantity(quantity);
                        service.confirmDelivery(deliveryId, request, 7L, true); tx.commit();
                    }
                }
                try (var session = factory.withOptions().tenantIdentifier(ids[0]).openSession()) {
                    var invoices = new JpaRepositoryFactory(session).getRepository(SaleRepository.class)
                            .findBySalesOrderIdAndOrganizationId(ids[1], ids[0]);
                    assertEquals(2, invoices.size());
                    assertEquals(0, new java.math.BigDecimal("100").compareTo(invoices.stream()
                            .map(Sale::getDiscount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)));
                    assertEquals(0, new java.math.BigDecimal("1000").compareTo(invoices.stream()
                            .map(Sale::getTotal).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)));
                    assertEquals(SalesOrderStatus.DELIVERED, session.find(SalesOrder.class, ids[1]).getStatus());
                }
            } finally { TenantContext.clear(); }
        }
    }
}
