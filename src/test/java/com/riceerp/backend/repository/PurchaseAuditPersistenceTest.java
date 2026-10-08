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
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchaseAuditPersistenceTest {
    Configuration config(String name) {
        return new Configuration().addAnnotatedClass(SalesOrder.class).addAnnotatedClass(SalesOrderItem.class)
                .addAnnotatedClass(Customer.class).addAnnotatedClass(User.class).addAnnotatedClass(Organization.class).addAnnotatedClass(Supplier.class)
                .addAnnotatedClass(Product.class).addAnnotatedClass(Purchase.class).addAnnotatedClass(PurchaseItem.class)
                .addAnnotatedClass(GoodsReceipt.class).addAnnotatedClass(GoodsReceiptItem.class)
                .addAnnotatedClass(PurchaseReturn.class).addAnnotatedClass(StockMovement.class).addAnnotatedClass(PriceHistory.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
                .setProperty("hibernate.connection.isolation", "2")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class.getName());
    }
    long[] seed(Session session, boolean received) {
        var tx = session.beginTransaction();
        Organization org = new Organization(); org.setName("Shop"); session.persist(org);
        Supplier supplier = new Supplier(); supplier.setSupplierName("Supplier"); supplier.setPhone("9876543210"); session.persist(supplier);
        Product product = new Product(); product.setProductName("Rice"); product.setStock(100); session.persist(product);
        Purchase purchase = new Purchase(); purchase.setSupplier(supplier); purchase.setInvoiceNumber("PO-1");
        purchase.setStatus(received ? PurchaseStatus.RECEIVED : PurchaseStatus.ORDERED); session.persist(purchase);
        PurchaseItem line = new PurchaseItem(); line.setPurchase(purchase); line.setProduct(product); line.setQuantity(10); line.setPrice(100); session.persist(line);
        if (received) {
            GoodsReceipt receipt = new GoodsReceipt(); receipt.setPurchase(purchase); receipt.setReceiptNumber("GR-1"); session.persist(receipt);
            GoodsReceiptItem item = new GoodsReceiptItem(); item.setReceipt(receipt); item.setProduct(product);
            item.setOrderedQty(10); item.setReceivedQty(10); item.setUnitPrice(100); session.persist(item);
        }
        tx.commit(); return new long[]{org.getId(), purchase.getId(), product.getId()};
    }
    GoodsReceiptService receiving(JpaRepositoryFactory repos, StockMovementService movements, ProcurementLock lock) {
        return new GoodsReceiptService(repos.getRepository(GoodsReceiptRepository.class), repos.getRepository(GoodsReceiptItemRepository.class),
                repos.getRepository(PurchaseRepository.class), repos.getRepository(PurchaseItemRepository.class),
                repos.getRepository(ProductRepository.class), movements, lock);
    }
    PurchaseService purchasing(JpaRepositoryFactory repos, StockMovementService movements, GoodsReceiptService receipts, ProcurementLock lock) {
        return new PurchaseService(repos.getRepository(PurchaseRepository.class), repos.getRepository(PurchaseItemRepository.class),
                repos.getRepository(PurchaseReturnRepository.class), repos.getRepository(SupplierRepository.class),
                repos.getRepository(ProductRepository.class), movements, receipts, lock);
    }
    @ParameterizedTest @ValueSource(strings = {"returns", "receipts", "cancelReceipt"})
    void competingRequestsPreserveStockAndPurchaseHistory(String mode) throws Exception {
        try (var factory = config("purchase_race_" + mode).buildSessionFactory()) {
            long[] ids;
            try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) { ids = seed(session, mode.equals("returns")); }
            var executor = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
            try {
                List<Future<Boolean>> outcomes = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    boolean cancel = mode.equals("cancelReceipt") && i == 0;
                    outcomes.add(executor.submit(() -> {
                        TenantContext.setCurrentTenant(ids[0]);
                        try (var session = factory.withOptions().tenantIdentifier(ids[0]).openSession()) {
                            var tx = session.beginTransaction(); var repos = new JpaRepositoryFactory(session);
                            var movements = new StockMovementService(repos.getRepository(StockMovementRepository.class));
                            var lock = new ProcurementLock(repos.getRepository(OrganizationRepository.class));
                            var receiving = receiving(repos, movements, lock);
                            var purchasing = purchasing(repos, movements, receiving, lock);
                            start.await();
                            try {
                                if (mode.equals("returns")) {
                                    var request = new PurchaseReturnRequest(); request.setProductId(ids[2]); request.setQuantityReturned(6);
                                    purchasing.createPurchaseReturn(ids[1], request);
                                } else if (cancel) purchasing.cancel(ids[1]);
                                else {
                                    var line = new GoodsReceiptItemRequest(); line.setProductId(ids[2]); line.setReceivedQty(6); line.setUnitPrice(100);
                                    var request = new GoodsReceiptRequest(); request.setItems(List.of(line)); receiving.createReceipt(ids[1], request);
                                }
                                tx.commit(); return true;
                            } catch (BusinessRuleException expected) { tx.rollback(); return false; }
                        } finally { TenantContext.clear(); }
                    }));
                }
                start.countDown(); int successful = 0;
                for (var result : outcomes) if (result.get(30, TimeUnit.SECONDS)) successful++;
                assertEquals(1, successful);
                try (var session = factory.withOptions().tenantIdentifier(ids[0]).openSession()) {
                    var purchase = session.find(Purchase.class, ids[1]);
                    var product = session.find(Product.class, ids[2]);
                    long movementCount = session.createQuery("select count(m) from StockMovement m", Long.class).getSingleResult();
                    if (mode.equals("returns")) {
                        assertEquals(94, product.getStock()); assertEquals(1, movementCount);
                        assertEquals(1L, session.createQuery("select count(r) from PurchaseReturn r", Long.class).getSingleResult());
                    } else if (purchase.getStatus() == PurchaseStatus.CANCELLED) {
                        assertEquals(100, product.getStock()); assertEquals(0, movementCount);
                        assertEquals(0L, session.createQuery("select count(r) from GoodsReceipt r", Long.class).getSingleResult());
                    } else {
                        assertEquals(PurchaseStatus.PARTIALLY_RECEIVED, purchase.getStatus());
                        assertEquals(106, product.getStock()); assertEquals(1, movementCount);
                        assertEquals(1L, session.createQuery("select count(r) from GoodsReceipt r", Long.class).getSingleResult());
                    }
                }
            } finally { executor.shutdownNow(); }
        }
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void openingStockAndHistoryCommitTogetherOrRollbackOnLedgerFailure(boolean fail) {
        try (var factory = config("opening_audit_" + fail).buildSessionFactory()) {
            TenantContext.setCurrentTenant(1L);
            try {
                var repos = new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory));
                StockMovementService movements = fail ? mock(StockMovementService.class)
                        : new StockMovementService(repos.getRepository(StockMovementRepository.class));
                if (fail) doThrow(new IllegalStateException("Ledger unavailable")).when(movements).record(any(), any(), anyDouble(), anyLong());
                var service = new ProductService(repos.getRepository(ProductRepository.class), repos.getRepository(PriceHistoryRepository.class), movements);
                var proxy = new ProxyFactory(service);
                proxy.addAdvice(new TransactionInterceptor(new JpaTransactionManager(factory), new AnnotationTransactionAttributeSource()));
                var request = new ProductRequest(); request.setProductName("Rice"); request.setStock(25); request.setSellingPrice(100);
                ProductService transactional = (ProductService) proxy.getProxy();
                if (fail) assertThrows(IllegalStateException.class, () -> transactional.createProduct(request));
                else transactional.createProduct(request);
                try (var session = factory.withOptions().tenantIdentifier(1L).openSession()) {
                    assertEquals(fail ? 0L : 1L, session.createQuery("select count(p) from Product p", Long.class).getSingleResult());
                    assertEquals(fail ? 0L : 2L, session.createQuery("select count(p) from PriceHistory p", Long.class).getSingleResult());
                    var movementsSaved = session.createQuery("from StockMovement", StockMovement.class).list();
                    assertEquals(fail ? 0 : 1, movementsSaved.size());
                    if (!fail) {
                        assertEquals(MovementType.OPENING_STOCK, movementsSaved.get(0).getMovementType());
                        assertEquals(25, movementsSaved.get(0).getQuantity());
                        assertEquals(movementsSaved.get(0).getProduct().getId(), movementsSaved.get(0).getReferenceId());
                    }
                }
            } finally { TenantContext.clear(); }
        }
    }
}
