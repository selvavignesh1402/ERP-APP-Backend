package com.riceerp.backend.repository;

import com.riceerp.backend.entity.User;
import com.riceerp.backend.enums.PlatformRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query(value = "select u.* from users u join organization_memberships m on m.user_id=u.id " +
            "where u.id=:id and m.organization_id=:orgId and m.is_active=true and u.is_active=true", nativeQuery = true)
    Optional<User> findActiveOrganizationUser(@Param("id") Long id, @Param("orgId") Long orgId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.phoneNumber = :phone")
    Optional<User> findByPhoneNumberForUpdate(@Param("phone") String phone);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findForUpdate(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.platformRole = com.riceerp.backend.enums.PlatformRole.MASTER_ADMIN and u.isActive = true order by u.id")
    List<User> lockActivePlatformAdmins();

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    long countByPlatformRole(PlatformRole platformRole);

    @Query("SELECT COUNT(u) FROM User u WHERE u.platformRole = :role AND u.isActive = true")
    long countActiveAdmins(@Param("role") PlatformRole role);
}

