package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Sale;
import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.enums.PaymentMode;
import com.riceerp.backend.security.TenantIdentifierResolver;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class SaleReferencePersistenceTest {
    private Sale sale(String bill, String reference) {
        Sale sale = new Sale(); sale.setBillNumber(bill); sale.setClientReferenceId(reference);
        sale.setPaymentMode(PaymentMode.CASH); return sale;
    }

    @Test void concurrentReferenceIsUniquePerShopButNormalSalesAndOtherShopsStillWork() throws Exception {
        var config = new Configuration().addAnnotatedClass(Sale.class).addAnnotatedClass(Customer.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:sale_refs;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        try (var factory = config.buildSessionFactory()) {
            var start = new CountDownLatch(1);
            var executor = Executors.newFixedThreadPool(2);
            try {
                var results = new java.util.ArrayList<Future<Boolean>>();
                for (int i = 0; i < 2; i++) {
                    final String bill = "bill-" + i;
                    results.add(executor.submit(() -> {
                        start.await();
                        try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                            var tx = session.beginTransaction();
                            try { session.persist(sale(bill, "offline-ref")); tx.commit(); return true; }
                            catch (org.hibernate.exception.ConstraintViolationException ex) { tx.rollback(); return false; }
                        }
                    }));
                }
                start.countDown();
                int commits = 0;
                for (var result : results) if (result.get(30, TimeUnit.SECONDS)) commits++;
                assertEquals(1, commits);
                try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                    assertEquals(1L, session.createQuery("select count(s) from Sale s", Long.class).getSingleResult());
                    var tx = session.beginTransaction();
                    session.persist(sale("normal-1", null)); session.persist(sale("normal-2", null)); tx.commit();
                }
                try (var session = factory.withOptions().tenantIdentifier(2L).openSession()) {
                    var tx = session.beginTransaction(); session.persist(sale("other-shop", "offline-ref")); tx.commit();
                    assertEquals(1L, session.createQuery("select count(s) from Sale s", Long.class).getSingleResult());
                }
            } finally { executor.shutdownNow(); }
        }
    }
}
