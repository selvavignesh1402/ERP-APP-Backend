package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Payment;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.security.TenantIdentifierResolver;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentAllocationPersistenceTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void simultaneousSubmissionsCannotCollectTwice(boolean sameRequestId) throws Exception {
        var config = new Configuration().addAnnotatedClass(Payment.class)
                .addAnnotatedClass(com.riceerp.backend.entity.Organization.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:concurrent_" + sameRequestId + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
                .setProperty("hibernate.connection.isolation", "2")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        try (var factory = config.buildSessionFactory()) {
            Long orgId;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var tx = session.beginTransaction();
                var org = new com.riceerp.backend.entity.Organization(); org.setName("Test shop");
                session.persist(org); tx.commit(); orgId = org.getId();
            }
            var start = new java.util.concurrent.CountDownLatch(1);
            var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                java.util.List<java.util.concurrent.Future<Boolean>> results = new java.util.ArrayList<>();
                for (int index = 0; index < 2; index++) {
                    String key = sameRequestId ? "same-request" : "request-" + index;
                    results.add(executor.submit(() -> {
                        start.await();
                        com.riceerp.backend.security.TenantContext.setCurrentTenant(orgId);
                        try (var session = factory.withOptions().tenantIdentifier(orgId).openSession()) {
                            var tx = session.beginTransaction();
                            var repositoryFactory = new JpaRepositoryFactory(session);
                            var paymentRepo = repositoryFactory.getRepository(PaymentRepository.class);
                            var orgRepo = repositoryFactory.getRepository(OrganizationRepository.class);
                            var sales = mock(SaleRepository.class);
                            var customers = mock(CustomerRepository.class);
                            var customer = new com.riceerp.backend.entity.Customer();
                            org.springframework.test.util.ReflectionTestUtils.setField(customer, "id", 20L);
                            customer.setCreditBalance(100);
                            var sale = new com.riceerp.backend.entity.Sale(); sale.setId(10L);
                            sale.setPaymentMode(PaymentMode.CREDIT); sale.setGrandTotal(100); sale.setCustomer(customer);
                            when(sales.findByIdAndOrganizationId(10L, orgId)).thenReturn(java.util.Optional.of(sale));
                            when(customers.findByIdAndOrganizationId(20L, orgId)).thenReturn(java.util.Optional.of(customer));
                            when(sales.findByCustomerIdAndOrganizationIdAndPaymentModeOrderBySaleDateAscIdAsc(20L, orgId, PaymentMode.CREDIT))
                                    .thenReturn(java.util.List.of(sale));
                            var service = new com.riceerp.backend.service.PaymentService(paymentRepo, sales, customers, orgRepo, mock(PurchaseRepository.class));
                            var request = new com.riceerp.backend.dto.PaymentRequest();
                            request.setReferenceType("SALE"); request.setReferenceId(10L); request.setAmount(new BigDecimal("100"));
                            request.setPaymentMode("CASH"); request.setClientReferenceId(key);
                            try { service.createPayment(request); tx.commit(); return true; }
                            catch (com.riceerp.backend.exception.BusinessRuleException ex) { tx.rollback(); return false; }
                        } finally { com.riceerp.backend.security.TenantContext.clear(); }
                    }));
                }
                start.countDown();
                int successfulResponses = 0;
                for (var result : results) if (result.get(30, java.util.concurrent.TimeUnit.SECONDS)) successfulResponses++;
                assertEquals(sameRequestId ? 2 : 1, successfulResponses);
                try (var session = factory.withOptions().tenantIdentifier(orgId).openSession()) {
                    var repo = new JpaRepositoryFactory(session).getRepository(PaymentRepository.class);
                    assertEquals(1, repo.count()); assertEquals(0, new BigDecimal("100").compareTo(repo.sumByReference(ReferenceType.SALE, 10L)));
                }
            } finally { executor.shutdownNow(); }
        }
    }
    @Test void allocationsPersistAndQueriesIncludeOnlySelectedTenant() {
        var config = new Configuration().addAnnotatedClass(Payment.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:allocations;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        try (var factory = config.buildSessionFactory()) {
            Long collectionId;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var tx = session.beginTransaction();
                Payment direct = payment(ReferenceType.SALE, 10L, 40); session.persist(direct);
                Payment collected = payment(ReferenceType.CUSTOMER, 20L, 70);
                collected.getSaleAllocations().put(10L, new BigDecimal("60")); collected.setOpeningBalanceAmount(10.0);
                session.persist(collected); tx.commit(); collectionId = collected.getId();
            }
            try (var session = factory.withOptions().tenantIdentifier(2L).openSession()) {
                var tx = session.beginTransaction();
                Payment otherShop = payment(ReferenceType.CUSTOMER, 20L, 500);
                otherShop.getSaleAllocations().put(10L, new BigDecimal("500")); session.persist(otherShop); tx.commit();
            }
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var repository = new JpaRepositoryFactory(session).getRepository(PaymentRepository.class);
                assertEquals(0, new BigDecimal("40").compareTo(repository.sumByReference(ReferenceType.SALE, 10L)));
                assertEquals(0, new BigDecimal("60").compareTo(repository.sumAllocatedToSale(10L)));
                assertEquals(2, repository.findPaymentsForSale(10L).size());
                assertEquals(1, repository.findByReferenceTypeAndReferenceId(ReferenceType.CUSTOMER, 20L).size());
                Payment persisted = repository.findByIdAndOrganizationId(collectionId, 1L).orElseThrow();
                assertEquals(0, new BigDecimal("60").compareTo(persisted.getSaleAllocations().get(10L))); assertEquals(0, new BigDecimal("10").compareTo(persisted.getOpeningBalanceAmount()));
            }
        }
    }
    @Test void decimalAmountsSurviveReloadAndAllAggregateQueriesExactly() {
        var config = new Configuration().addAnnotatedClass(Payment.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:decimal_payments;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        // Within DECIMAL(19,4), but beyond the precision of a double.
        var amount = new BigDecimal("123456789012345.6789");
        var increment = new BigDecimal("0.0001");
        var expected = amount.add(increment);
        try (var factory = config.buildSessionFactory()) {
            var ids = new java.util.ArrayList<Long>();
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var tx = session.beginTransaction();
                for (var value : java.util.List.of(amount, increment)) {
                    var payment = payment(ReferenceType.SALE, 10L, 0);
                    payment.setAmount(value);
                    payment.setOpeningBalanceAmount(value);
                    payment.getSaleAllocations().put(20L, value);
                    session.persist(payment);
                    ids.add(payment.getId());
                }
                tx.commit();
            }
            try (var session = factory.withOptions().tenantIdentifier(2L).openSession()) {
                var tx = session.beginTransaction();
                var foreign = payment(ReferenceType.SALE, 10L, 500);
                foreign.getSaleAllocations().put(20L, new BigDecimal("500"));
                session.persist(foreign);
                ids.add(foreign.getId());
                tx.commit();
            }
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var repo = new JpaRepositoryFactory(session).getRepository(PaymentRepository.class);
                var loaded = repo.findByIdAndOrganizationId(ids.get(0), 1L).orElseThrow();
                assertEquals(0, amount.compareTo(loaded.getAmount()));
                assertEquals(0, amount.compareTo(loaded.getOpeningBalanceAmount()));
                assertEquals(0, amount.compareTo(loaded.getSaleAllocations().get(20L)));
                assertEquals(0, expected.compareTo(repo.sumByReference(ReferenceType.SALE, 10L)));
                assertEquals(0, expected.compareTo(repo.sumAllocatedToSale(20L)));
                assertEquals(0, expected.compareTo(repo.sumAmountByIdIn(ids)));
                assertEquals(0, repo.sumByReference(ReferenceType.SALE, 999L).signum());
                assertEquals(0, repo.sumAllocatedToSale(999L).signum());
                assertEquals(0, repo.sumAmountByIdIn(java.util.List.of(-1L)).signum());
            }
        }
    }

    private Payment payment(ReferenceType type, Long id, double amount) {
        Payment payment = new Payment(); payment.setReferenceType(type); payment.setReferenceId(id);
        payment.setAmount(amount); payment.setPaymentMode(PaymentMode.CASH); return payment;
    }
}
