package com.medsync.dto;

import com.medsync.entity.Hospital;
import com.medsync.entity.User;
import jakarta.validation.constraints.*;
import lombok.*;

public class AuthDto {

    @Data
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }

    @Data
    public static class RegisterHospitalRequest {
        @NotBlank private String username;
        @NotBlank @Email private String email;
        @NotBlank @Size(min = 8) private String password;
        @NotBlank private String hospitalName;
        @NotBlank private String address;
        @NotBlank private String licenseNumber;
        private String contactPhone;
        private String contactEmail;
        private Double latitude;
        private Double longitude;
        @NotNull private Hospital.InstitutionType type;
    }

    @Data
    @Builder
    public static class AuthResponse {
        private String token;
        private String username;
        private String role;
        private Long institutionId;
        private String institutionName;
        private String status;
    }

    @Data
    @Builder
    public static class MessageResponse {
        private String message;
        private boolean success;
    }
}
