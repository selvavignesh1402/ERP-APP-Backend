package com.riceerp.backend.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceerp.backend.entity.*;
import com.riceerp.backend.enums.*;
import com.riceerp.backend.security.*;
import com.riceerp.backend.service.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReconciliationConcurrencyTest {
    @Test void concurrentInvoicesCannotBothMatchTheSameReceiptCapacity() throws Exception {
        var config = new Configuration().addAnnotatedClass(Organization.class).addAnnotatedClass(Supplier.class)
                .addAnnotatedClass(Product.class).addAnnotatedClass(Purchase.class).addAnnotatedClass(PurchaseItem.class)
                .addAnnotatedClass(SupplierInvoice.class).addAnnotatedClass(SupplierInvoiceItem.class).addAnnotatedClass(ReconciliationResult.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:reconcile_concurrent;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
                .setProperty("hibernate.connection.isolation", "2")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
        try (var factory = config.buildSessionFactory()) {
            Long orgId, purchaseId, productId; List<Long> invoiceIds = new ArrayList<>();
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                var tx = session.beginTransaction();
                Organization org = new Organization(); org.setName("Shop"); session.persist(org); orgId = org.getId();
                Supplier supplier = new Supplier(); supplier.setSupplierName("Supplier"); supplier.setPhone("9876543210"); session.persist(supplier);
                Product product = new Product(); product.setProductName("Rice"); session.persist(product); productId = product.getId();
                Purchase purchase = new Purchase(); purchase.setSupplier(supplier); purchase.setInvoiceNumber("PO-1");
                purchase.setStatus(PurchaseStatus.RECEIVED); purchase.setTotalAmount(1000); session.persist(purchase); purchaseId = purchase.getId();
                PurchaseItem po = new PurchaseItem(); po.setPurchase(purchase); po.setProduct(product); po.setQuantity(10); po.setPrice(100); session.persist(po);
                for (int i = 0; i < 2; i++) {
                    SupplierInvoice invoice = new SupplierInvoice(); invoice.setInvoiceNumber("INV-" + i);
                    invoice.setSupplier(supplier); invoice.setTotalAmount(600); session.persist(invoice); invoiceIds.add(invoice.getId());
                    SupplierInvoiceItem item = new SupplierInvoiceItem(); item.setInvoice(invoice); item.setProduct(product);
                    item.setQuantity(6); item.setUnitPrice(100); item.setTotalAmount(600); session.persist(item);
                }
                tx.commit();
            }
            var executor = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
            try {
                List<Future<ReconciliationStatus>> results = new ArrayList<>();
                for (Long invoiceId : invoiceIds) results.add(executor.submit(() -> {
                    TenantContext.setCurrentTenant(orgId);
                    try (var session = factory.withOptions().tenantIdentifier(orgId).openSession()) {
                        var tx = session.beginTransaction(); var repos = new JpaRepositoryFactory(session);
                        var lock = new ProcurementLock(repos.getRepository(OrganizationRepository.class));
                        var invoices = new SupplierInvoiceService(repos.getRepository(SupplierInvoiceRepository.class),
                                repos.getRepository(SupplierInvoiceItemRepository.class), repos.getRepository(SupplierRepository.class),
                                repos.getRepository(PurchaseRepository.class), repos.getRepository(ProductRepository.class), lock);
                        var receipts = mock(GoodsReceiptService.class);
                        when(receipts.getReceivedQuantities(purchaseId)).thenReturn(Map.of(productId, 10.0));
                        var service = new ReconciliationService(repos.getRepository(ReconciliationResultRepository.class),
                                repos.getRepository(PurchaseRepository.class), repos.getRepository(PurchaseItemRepository.class), invoices,
                                receipts, new ObjectMapper(), lock);
                        start.await(); var result = service.reconcile(purchaseId, invoiceId); tx.commit(); return result.getStatus();
                    } finally { TenantContext.clear(); }
                }));
                start.countDown(); int matched = 0;
                for (var result : results) if (result.get(30, TimeUnit.SECONDS) == ReconciliationStatus.MATCHED) matched++;
                assertEquals(1, matched);
                try (var session = factory.withOptions().tenantIdentifier(orgId).openSession()) {
                    assertEquals(2L, session.createQuery("select count(r) from ReconciliationResult r", Long.class).getSingleResult());
                    assertEquals(2, new JpaRepositoryFactory(session).getRepository(SupplierInvoiceRepository.class)
                            .findByPurchaseIdAndOrganizationId(purchaseId, orgId).size());
                }
            } finally { executor.shutdownNow(); }
        }
    }
}
