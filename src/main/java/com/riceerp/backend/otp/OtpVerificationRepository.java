package com.riceerp.backend.otp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);
}

