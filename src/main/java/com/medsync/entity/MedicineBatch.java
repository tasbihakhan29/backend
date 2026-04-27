package com.medsync.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "medicine_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    private LocalDate manufactureDate;

    private BigDecimal purchasePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_status")
    private ExpiryStatus expiryStatus;

    private boolean shareAlert = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        updateExpiryStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateExpiryStatus();
    }

    private void updateExpiryStatus() {
        if (expiryDate == null) return;

        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

        if (expiryDate.isBefore(LocalDate.now())) {
            this.expiryStatus = ExpiryStatus.EXPIRED;
        } else if (daysLeft <= 7) {
            this.expiryStatus = ExpiryStatus.CRITICAL;
        } else if (daysLeft <= 30) {
            this.expiryStatus = ExpiryStatus.NEAR_EXPIRY;
        } else {
            this.expiryStatus = ExpiryStatus.GOOD;
        }
    }

    public enum ExpiryStatus {
        GOOD, NEAR_EXPIRY, CRITICAL, EXPIRED
    }
}