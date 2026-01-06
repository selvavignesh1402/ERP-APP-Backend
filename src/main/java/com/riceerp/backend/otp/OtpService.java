package com.riceerp.backend.otp;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    private final OtpVerificationRepository otpRepository;

    public OtpService(OtpVerificationRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    public String generateAndSaveOtp(String phoneNumber) {
        String otp = generateOtp();

        OtpVerification otpEntity = new OtpVerification();
        otpEntity.setPhoneNumber(phoneNumber);
        otpEntity.setOtpCode(otp);
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpEntity.setVerified(false);
        otpEntity.setAttempts(0);

        otpRepository.save(otpEntity);

        // TEMP: print OTP in console (SMS later)
        System.out.println("OTP for " + phoneNumber + " is: " + otp);

        return otp;
    }

    public boolean verifyOtp(String phoneNumber, String otp) {

        OtpVerification otpEntity = otpRepository
                .findTopByPhoneNumberOrderByCreatedAtDesc(phoneNumber)
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (otpEntity.isVerified()) {
            throw new RuntimeException("OTP already used");
        }

        if (otpEntity.getAttempts() >= MAX_ATTEMPTS) {
            throw new RuntimeException("Maximum OTP attempts exceeded");
        }

        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        otpEntity.setAttempts(otpEntity.getAttempts() + 1);

        if (!otpEntity.getOtpCode().equals(otp)) {
            otpRepository.save(otpEntity);
            throw new RuntimeException("Invalid OTP");
        }

        otpEntity.setVerified(true);
        otpRepository.save(otpEntity);

        return true;
    }

    private String generateOtp() {
        Random random = new Random();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }
}
