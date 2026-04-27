package com.medsync.dto;

import com.medsync.entity.MedicineBatch;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

public class MedicineDto {

    @Data
    public static class CreateMedicineRequest {
        @NotBlank private String name;
        private String genericName;
        private String manufacturer;
        private String category;
        private String description;
        private String barcode;
        @NotBlank private String unit;
    }

    @Data
    public static class AddBatchRequest {
        @NotNull private Long medicineId;
        @NotBlank private String batchNumber;
        @NotNull @Min(1) private Integer quantity;
        @NotNull private LocalDate expiryDate;
        private LocalDate manufactureDate;
        private Double purchasePrice;
        private Boolean shareAlert = false;
    }

    @Data
    public static class UpdateBatchRequest {
        @Min(0) private Integer quantity;
        private Boolean shareAlert;
    }

    @Data
    @Builder
    public static class MedicineResponse {
        private Long id;
        private String name;
        private String genericName;
        private String manufacturer;
        private String category;
        private String barcode;
        private String unit;
        private String approvalStatus;
    }

    @Data
    @Builder
    public static class BatchResponse {
        private Long id;
        private Long medicineId;
        private String medicineName;
        private String genericName;
        private String batchNumber;
        private Integer quantity;
        private LocalDate expiryDate;
        private LocalDate manufactureDate;
        private Double purchasePrice;
        private String expiryStatus;
        private Boolean shareAlert;
        private Long hospitalId;
        private String hospitalName;
        private long daysUntilExpiry;
    }

    @Data
    @Builder
    public static class AvailabilityResponse {
        private Long medicineId;
        private String medicineName;
        private Long hospitalId;
        private String hospitalCode;
        private Integer totalQuantity;
        private String nearestBatchExpiry;
        private double distanceKm;
    }
}
