package com.riceerp.backend.repository;

import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.PaymentMode;
import com.riceerp.backend.security.TenantIdentifierResolver;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class SaleDecimalPersistenceTest {
    @Test void invoiceAmountsAndRevenueSumsStayExactAcrossReloadAndTenantFiltering() {
        var config = new Configuration().addAnnotatedClass(Sale.class).addAnnotatedClass(Customer.class)
                .addAnnotatedClass(Organization.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:sale_decimals;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        var amount = new BigDecimal("123456789012345.6789");
        var date = LocalDateTime.of(2026, 10, 5, 12, 0);
        try (var factory = config.buildSessionFactory()) {
            Long id;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var tx = session.beginTransaction();
                var first = sale("first", amount, date);
                first.setDiscount(new BigDecimal("0.0001"));
                first.setCgst(new BigDecimal("1234567890123.4567"));
                first.setSgst(new BigDecimal("1234567890123.4567"));
                first.setIgst(new BigDecimal("0.0002"));
                session.persist(first);
                session.persist(sale("second", new BigDecimal("0.0001"), date));
                session.persist(sale("older", new BigDecimal("1"), date.minusDays(1)));
                tx.commit();
                id = first.getId();
            }
            try (var session = factory.withOptions().tenantIdentifier(2L).openSession()) {
                var tx = session.beginTransaction();
                session.persist(sale("foreign", new BigDecimal("500"), date));
                tx.commit();
            }
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var repo = new JpaRepositoryFactory(session).getRepository(SaleRepository.class);
                var loaded = repo.findByIdAndOrganizationId(id, 1L).orElseThrow();
                assertEquals(0, amount.compareTo(loaded.getTotal()));
                assertEquals(0, amount.compareTo(loaded.getGrandTotal()));
                assertEquals(0, new BigDecimal("0.0001").compareTo(loaded.getDiscount()));
                assertEquals(0, new BigDecimal("1234567890123.4567").compareTo(loaded.getCgst()));
                assertEquals(loaded.getCgst(), loaded.getSgst());
                assertEquals(0, new BigDecimal("0.0002").compareTo(loaded.getIgst()));
                assertEquals(0, new BigDecimal("123456789012345.6790").compareTo(
                        repo.sumGrandTotalBySaleDateBetween(date.minusHours(1), date.plusHours(1))));
                assertEquals(0, new BigDecimal("123456789012346.6790").compareTo(
                        repo.sumGrandTotalByPaymentMode(PaymentMode.CASH)));
                assertEquals(0, repo.sumGrandTotalByPaymentMode(PaymentMode.CREDIT).signum());
                assertEquals(0, repo.sumGrandTotalBySaleDateBetween(date.plusDays(1), date.plusDays(2)).signum());
            }
        }
    }

    private Sale sale(String bill, BigDecimal amount, LocalDateTime date) {
        var sale = new Sale();
        sale.setBillNumber(bill);
        sale.setPaymentMode(PaymentMode.CASH);
        sale.setSaleDate(date);
        sale.setTotal(amount);
        sale.setGrandTotal(amount);
        return sale;
    }
}
