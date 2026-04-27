package com.medsync.dto;

import com.medsync.entity.MedicineRequest;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

public class RequestDto {

    @Data
    public static class CreateRequestRequest {
        @NotNull private Long medicineId;
        @NotNull private Long supplierId;
        @NotNull @Min(1) private Integer quantityRequested;
        @NotNull private MedicineRequest.UrgencyLevel urgency;
        private String requestNote;
    }

    @Data
    public static class RespondToRequestRequest {
        @NotNull private MedicineRequest.RequestStatus status;
        private Integer quantityApproved;
        private String responseNote;
    }

    @Data
    @Builder
    public static class RequestResponse {
        private Long id;
        private String trackingCode;
        private String requesterCode;
        private String supplierCode;
        private String medicineName;
        private Integer quantityRequested;
        private Integer quantityApproved;
        private String urgency;
        private String status;
        private String requestNote;
        private String responseNote;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        // Full details shown only to admin or own party
        private String requesterName;
        private String supplierName;
    }
}
