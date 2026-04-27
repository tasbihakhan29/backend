package com.medsync.controller;

import com.medsync.entity.Hospital;
import com.medsync.entity.Medicine;
import com.medsync.entity.MedicineBatch;
import com.medsync.entity.MedicineRequest;
import com.medsync.repository.HospitalRepository;
import com.medsync.repository.MedicineRepository;
import com.medsync.repository.MedicineBatchRepository;
import com.medsync.repository.MedicineRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final HospitalRepository hospitalRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final MedicineRequestRepository medicineRequestRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPublicStats() {
        // Count ACTIVE hospitals and pharmacies
        long totalInstitutions = hospitalRepository.findAll()
                .stream()
                .filter(h -> h.getUser() != null && h.getUser().getStatus().toString().equals("ACTIVE"))
                .count();

        // Count APPROVED medicines
        long totalMedicines = medicineRepository.findAll()
                .stream()
                .filter(m -> m.getApprovalStatus().toString().equals("APPROVED"))
                .count();

        // Count batches with SAFE or NEAR_EXPIRY status
        long activeBatches = medicineBatchRepository.findAll()
                .stream()
                .filter(b -> {
                    String status = b.getExpiryStatus() != null ? b.getExpiryStatus().toString() : "UNKNOWN";
                    return status.equals("SAFE") || status.equals("NEAR_EXPIRY");
                })
                .count();

        // Count PENDING transfer requests
        long pendingTransfers = medicineRequestRepository.findAll()
                .stream()
                .filter(r -> r.getStatus().toString().equals("PENDING"))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInstitutions", totalInstitutions);
        stats.put("totalMedicines", totalMedicines);
        stats.put("activeBatches", activeBatches);
        stats.put("pendingTransfers", pendingTransfers);

        return ResponseEntity.ok(stats);
    }
}
