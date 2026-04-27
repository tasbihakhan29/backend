package com.medsync.service;

import com.medsync.dto.MedicineDto;
import com.medsync.entity.*;
import com.medsync.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository batchRepository;
    private final HospitalRepository hospitalRepository;
    private final AuditService auditService;

    @Value("${app.expiry.alert-days:30}")
    private int alertDays;

    public Medicine createMedicine(MedicineDto.CreateMedicineRequest req, User requestedBy) {
        if (req.getBarcode() != null && medicineRepository.existsByBarcode(req.getBarcode())) {
            throw new RuntimeException("Barcode already exists");
        }

        Medicine.ApprovalStatus status = requestedBy.getRole() == User.Role.CITY_ADMIN
                ? Medicine.ApprovalStatus.APPROVED
                : Medicine.ApprovalStatus.PENDING;

        Medicine medicine = Medicine.builder()
                .name(req.getName())
                .genericName(req.getGenericName())
                .manufacturer(req.getManufacturer())
                .category(req.getCategory())
                .description(req.getDescription())
                .barcode(req.getBarcode())
                .unit(req.getUnit())
                .approvalStatus(status)
                .addedBy(requestedBy)
                .build();

        medicine = medicineRepository.save(medicine);
        auditService.log(requestedBy, "CREATE_MEDICINE", "Medicine", medicine.getId(), medicine.getName());
        return medicine;
    }

    public List<MedicineDto.MedicineResponse> searchMedicines(String query) {
        return medicineRepository
                .findByNameContainingIgnoreCaseAndApprovalStatus(query, Medicine.ApprovalStatus.APPROVED)
                .stream()
                .map(this::toMedicineResponse)
                .collect(Collectors.toList());
    }

    public MedicineDto.MedicineResponse getMedicineByBarcode(String barcode) {
        Medicine m = medicineRepository.findByBarcode(barcode)
                .orElseThrow(() -> new RuntimeException("Medicine not found for barcode: " + barcode));
        return toMedicineResponse(m);
    }

    @Transactional
    public MedicineBatch addBatch(MedicineDto.AddBatchRequest req, Hospital hospital, User user) {
        Medicine medicine = medicineRepository.findById(req.getMedicineId())
                .orElseThrow(() -> new RuntimeException("Medicine not found"));

        if (medicine.getApprovalStatus() != Medicine.ApprovalStatus.APPROVED) {
            throw new RuntimeException("Medicine not approved yet");
        }

        MedicineBatch batch = MedicineBatch.builder()
                .medicine(medicine)
                .hospital(hospital)
                .batchNumber(req.getBatchNumber())
                .quantity(req.getQuantity())
                .expiryDate(req.getExpiryDate())
                .manufactureDate(req.getManufactureDate())
                .purchasePrice(req.getPurchasePrice() != null
                        ? BigDecimal.valueOf(req.getPurchasePrice()) : null)
                .shareAlert(req.getShareAlert())
                .build();

        batch = batchRepository.save(batch);
        auditService.log(user, "ADD_BATCH", "MedicineBatch", batch.getId(),
                "Added batch " + req.getBatchNumber() + " qty:" + req.getQuantity());
        return batch;
    }

    @Transactional
    public MedicineBatch updateBatch(Long batchId, MedicineDto.UpdateBatchRequest req, User user) {
        MedicineBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        if (req.getQuantity() != null) batch.setQuantity(req.getQuantity());
        if (req.getShareAlert() != null) batch.setShareAlert(req.getShareAlert());

        batch = batchRepository.save(batch);
        auditService.log(user, "UPDATE_BATCH", "MedicineBatch", batchId, "Updated batch " + batchId);
        return batch;
    }

    public List<MedicineDto.BatchResponse> getInventory(Hospital hospital) {
        return batchRepository.findByHospital(hospital)
                .stream()
                .map(this::toBatchResponse)
                .collect(Collectors.toList());
    }

    public List<MedicineDto.BatchResponse> getNearExpiryBatches(Hospital hospital) {
        LocalDate alertDate = LocalDate.now().plusDays(alertDays);
        return batchRepository.findNearExpiry(hospital, alertDate)
                .stream()
                .map(this::toBatchResponse)
                .collect(Collectors.toList());
    }

    public List<MedicineDto.BatchResponse> getSharedNearExpiryAlerts() {
        LocalDate alertDate = LocalDate.now().plusDays(alertDays);
        return batchRepository.findSharedNearExpiry(alertDate)
                .stream()
                .map(this::toBatchResponse)
                .collect(Collectors.toList());
    }

    public List<MedicineDto.AvailabilityResponse> findAvailability(Long medicineId, Hospital requester) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));

        return batchRepository.findAvailableByMedicine(medicine, requester)
                .stream()
                .collect(Collectors.groupingBy(b -> b.getHospital().getId()))
                .values().stream()
                .map(batches -> {
                    Hospital supplier = batches.get(0).getHospital();
                    int total = batches.stream().mapToInt(MedicineBatch::getQuantity).sum();
                    double dist = calculateDistance(
                            requester.getLatitude(), requester.getLongitude(),
                            supplier.getLatitude(), supplier.getLongitude());
                    return MedicineDto.AvailabilityResponse.builder()
                            .medicineId(medicineId)
                            .medicineName(medicine.getName())
                            .hospitalId(supplier.getId())
                            .hospitalCode("HOSP-" + supplier.getId())
                            .totalQuantity(total)
                            .nearestBatchExpiry(batches.stream()
                                    .map(b -> b.getExpiryDate().toString())
                                    .min(String::compareTo).orElse("N/A"))
                            .distanceKm(Math.round(dist * 10.0) / 10.0)
                            .build();
                })
                .sorted((a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm()))
                .collect(Collectors.toList());
    }

    public List<MedicineDto.MedicineResponse> getAllApproved() {
        return medicineRepository.findByApprovalStatus(Medicine.ApprovalStatus.APPROVED)
                .stream().map(this::toMedicineResponse).collect(Collectors.toList());
    }

    public List<MedicineDto.MedicineResponse> getPendingMedicines() {
        return medicineRepository.findByApprovalStatus(Medicine.ApprovalStatus.PENDING)
                .stream().map(this::toMedicineResponse).collect(Collectors.toList());
    }

    @Transactional
    public void approveMedicine(Long id, boolean approve, User admin) {
        Medicine m = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));
        m.setApprovalStatus(approve ? Medicine.ApprovalStatus.APPROVED : Medicine.ApprovalStatus.REJECTED);
        medicineRepository.save(m);
        auditService.log(admin, approve ? "APPROVE_MEDICINE" : "REJECT_MEDICINE", "Medicine", id, m.getName());
    }

    private double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return 999.0;
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private MedicineDto.MedicineResponse toMedicineResponse(Medicine m) {
        return MedicineDto.MedicineResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .genericName(m.getGenericName())
                .manufacturer(m.getManufacturer())
                .category(m.getCategory())
                .barcode(m.getBarcode())
                .unit(m.getUnit())
                .approvalStatus(m.getApprovalStatus().name())
                .build();
    }

    public MedicineDto.BatchResponse toBatchResponse(MedicineBatch b) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), b.getExpiryDate());
        return MedicineDto.BatchResponse.builder()
                .id(b.getId())
                .medicineId(b.getMedicine().getId())
                .medicineName(b.getMedicine().getName())
                .genericName(b.getMedicine().getGenericName())
                .batchNumber(b.getBatchNumber())
                .quantity(b.getQuantity())
                .expiryDate(b.getExpiryDate())
                .manufactureDate(b.getManufactureDate())
                .expiryStatus(b.getExpiryStatus() != null ? b.getExpiryStatus().name() : "GOOD")
                .purchasePrice(b.getPurchasePrice() != null ? b.getPurchasePrice().doubleValue() : null)
                .shareAlert(b.isShareAlert())
                .hospitalId(b.getHospital().getId())
                .hospitalName(b.getHospital().getHospitalName())
                .daysUntilExpiry(days)
                .build();
    }
}
