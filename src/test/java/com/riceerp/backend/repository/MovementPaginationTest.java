package com.riceerp.backend.repository;

import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.MovementType;
import com.riceerp.backend.security.*;
import com.riceerp.backend.service.StockMovementService;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MovementPaginationTest {
    @Test void olderPagesRemainCompleteWhenAnotherMovementIsInserted() {
        var config = new Configuration().addAnnotatedClass(Organization.class).addAnnotatedClass(Product.class)
                .addAnnotatedClass(StockMovement.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:movement_pages;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        try (var factory = config.buildSessionFactory()) {
            for (long tenant = 1; tenant <= 2; tenant++) {
                try (var session = factory.withOptions().tenantIdentifier(tenant).openSession()) {
                    var tx = session.beginTransaction(); Product product = new Product(); product.setProductName("Rice"); session.persist(product);
                    for (int i = 0; i < 55; i++) {
                        var movement = new StockMovement(); movement.setProduct(product); movement.setQuantity(1);
                        movement.setMovementType(MovementType.OPENING_STOCK); session.persist(movement);
                    }
                    tx.commit();
                }
            }
            TenantContext.setCurrentTenant(1L);
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var service = new StockMovementService(new JpaRepositoryFactory(session).getRepository(StockMovementRepository.class));
                var first = service.movementPage(null, 50); assertEquals(50, first.items().size()); assertNotNull(first.nextBeforeId());
                var tx = session.beginTransaction(); service.record(first.items().get(0).getProduct(), MovementType.OPENING_STOCK, 1, null); tx.commit();
                var second = service.movementPage(first.nextBeforeId(), 50); assertEquals(5, second.items().size()); assertNull(second.nextBeforeId());
                Set<Long> ids = new HashSet<>();
                for (var page : List.of(first, second)) for (var row : page.items()) {
                    assertEquals(1L, row.getOrganizationId()); assertTrue(ids.add(row.getId()));
                }
                assertEquals(55, ids.size());
                assertFalse(ids.contains(service.movementPage(null, 50).items().get(0).getId()));
            } finally { TenantContext.clear(); }
        }
    }
}
