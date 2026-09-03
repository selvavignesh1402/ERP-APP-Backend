package com.riceerp.backend.repository;

import com.riceerp.backend.entity.User;
import com.riceerp.backend.enums.PlatformRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    long countByPlatformRole(PlatformRole platformRole);

    @Query("SELECT COUNT(u) FROM User u WHERE u.platformRole = :role AND u.isActive = true")
    long countActiveAdmins(@Param("role") PlatformRole role);
}

