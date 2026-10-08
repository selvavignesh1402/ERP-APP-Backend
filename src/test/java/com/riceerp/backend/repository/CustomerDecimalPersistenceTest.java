package com.riceerp.backend.repository;

import com.riceerp.backend.entity.Customer;
import com.riceerp.backend.enums.Status;
import com.riceerp.backend.security.TenantIdentifierResolver;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class CustomerDecimalPersistenceTest {
    @Test void balancesAndLimitsReloadExactlyAndTotalsRespectTenantAndStatus() {
        var config = new Configuration().addAnnotatedClass(Customer.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:customer_decimals;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        var amount = new BigDecimal("123456789012345.6789");
        try (var factory = config.buildSessionFactory()) {
            Long id;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var tx = session.beginTransaction();
                Customer first = customer(amount, Status.ACTIVE);
                session.persist(first);
                session.persist(customer(new BigDecimal("0.0001"), Status.ACTIVE));
                session.persist(customer(new BigDecimal("50"), Status.INACTIVE));
                tx.commit();
                id = first.getId();
            }
            try (var session = factory.withOptions().tenantIdentifier(2L).openSession()) {
                var tx = session.beginTransaction();
                session.persist(customer(new BigDecimal("100"), Status.ACTIVE));
                tx.commit();
            }
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var repo = new JpaRepositoryFactory(session).getRepository(CustomerRepository.class);
                var loaded = repo.findByIdAndOrganizationId(id, 1L).orElseThrow();
                assertEquals(0, amount.compareTo(loaded.getCreditBalance()));
                assertEquals(0, amount.compareTo(loaded.getCreditLimit()));
                var expected = new BigDecimal("123456789012345.6790");
                assertEquals(0, expected.compareTo(repo.sumActiveCreditBalance()));
                assertEquals(0, expected.compareTo(repo.sumActiveCreditBalanceByOrganizationId(1L)));
                assertEquals(0, expected.compareTo(repo.sumActiveCreditBalanceByOrganizationId(null)));
                assertEquals(0, repo.sumActiveCreditBalanceByOrganizationId(2L).signum());
            }
            try (var session = factory.withOptions().tenantIdentifier(3L).openSession()) {
                var repo = new JpaRepositoryFactory(session).getRepository(CustomerRepository.class);
                assertEquals(0, repo.sumActiveCreditBalance().signum());
            }
        }
    }

    private Customer customer(BigDecimal amount, Status status) {
        var customer = new Customer();
        customer.setCustomerName("Decimal test");
        customer.setCreditBalance(amount);
        customer.setCreditLimit(amount);
        customer.setStatus(status);
        return customer;
    }
}
