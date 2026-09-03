package com.riceerp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyOtpRequest {
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit Indian mobile number")
    private String phoneNumber;

    @NotBlank(message = "OTP is required")
    private String otp;

    private String name;

    public String getPhoneNumber() {
        if (phoneNumber != null && phoneNumber.startsWith("+91")) {
            return phoneNumber.substring(3);
        }
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && phoneNumber.startsWith("+91")) {
            this.phoneNumber = phoneNumber.substring(3);
        } else {
            this.phoneNumber = phoneNumber;
        }
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
