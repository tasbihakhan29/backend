package com.medsync.controller;

import com.medsync.dto.*;
import com.medsync.entity.*;
import com.medsync.repository.*;
import com.medsync.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hospital")
@RequiredArgsConstructor
public class HospitalController {

    private final MedicineService medicineService;
    private final RequestService requestService;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;

    private Hospital getCurrentHospital(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return hospitalRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Hospital not found"));
    }

    // --- Inventory ---
    @GetMapping("/inventory")
    public ResponseEntity<List<MedicineDto.BatchResponse>> getInventory(Authentication auth) {
        return ResponseEntity.ok(medicineService.getInventory(getCurrentHospital(auth)));
    }

    @PostMapping("/inventory/batch")
    public ResponseEntity<?> addBatch(@Valid @RequestBody MedicineDto.AddBatchRequest req,
                                       Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Hospital hospital = getCurrentHospital(auth);
        MedicineBatch batch = medicineService.addBatch(req, hospital, user);
        return ResponseEntity.ok(Map.of("success", true, "batchId", batch.getId()));
    }

    @PutMapping("/inventory/batch/{batchId}")
    public ResponseEntity<?> updateBatch(@PathVariable Long batchId,
                                          @RequestBody MedicineDto.UpdateBatchRequest req,
                                          Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        MedicineBatch batch = medicineService.updateBatch(batchId, req, user);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // --- Expiry ---
    @GetMapping("/expiry/near")
    public ResponseEntity<List<MedicineDto.BatchResponse>> getNearExpiry(Authentication auth) {
        return ResponseEntity.ok(medicineService.getNearExpiryBatches(getCurrentHospital(auth)));
    }

    @GetMapping("/expiry/shared-alerts")
    public ResponseEntity<List<MedicineDto.BatchResponse>> getSharedAlerts() {
        return ResponseEntity.ok(medicineService.getSharedNearExpiryAlerts());
    }

    @PatchMapping("/settings/share-expiry")
    public ResponseEntity<?> toggleExpirySharing(@RequestParam boolean share, Authentication auth) {
        Hospital hospital = getCurrentHospital(auth);
        hospital.setShareExpiryAlerts(share);
        hospitalRepository.save(hospital);
        return ResponseEntity.ok(Map.of("success", true, "shareExpiryAlerts", share));
    }

    // --- Medicine Search & Availability ---
    @GetMapping("/medicines/search")
    public ResponseEntity<List<MedicineDto.MedicineResponse>> searchMedicines(@RequestParam String query) {
        return ResponseEntity.ok(medicineService.searchMedicines(query));
    }

    @GetMapping("/medicines/barcode/{barcode}")
    public ResponseEntity<MedicineDto.MedicineResponse> getByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(medicineService.getMedicineByBarcode(barcode));
    }

    @GetMapping("/medicines/all")
    public ResponseEntity<List<MedicineDto.MedicineResponse>> getAllMedicines() {
        return ResponseEntity.ok(medicineService.getAllApproved());
    }

    @GetMapping("/medicines/{id}/availability")
    public ResponseEntity<List<MedicineDto.AvailabilityResponse>> getAvailability(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(medicineService.findAvailability(id, getCurrentHospital(auth)));
    }

    @PostMapping("/medicines")
    public ResponseEntity<?> addMedicine(@Valid @RequestBody MedicineDto.CreateMedicineRequest req,
                                          Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Medicine m = medicineService.createMedicine(req, user);
        return ResponseEntity.ok(Map.of("success", true, "medicineId", m.getId(),
                "status", m.getApprovalStatus().name()));
    }

    // --- Requests ---
    @PostMapping("/requests")
    public ResponseEntity<RequestDto.RequestResponse> createRequest(
            @Valid @RequestBody RequestDto.CreateRequestRequest req, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Hospital hospital = getCurrentHospital(auth);
        return ResponseEntity.ok(requestService.createRequest(req, hospital, user));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<RequestDto.RequestResponse>> getMyRequests(Authentication auth) {
        Hospital hospital = getCurrentHospital(auth);
        return ResponseEntity.ok(requestService.getMyRequests(hospital, false));
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<List<RequestDto.RequestResponse>> getIncomingRequests(Authentication auth) {
        return ResponseEntity.ok(requestService.getIncomingRequests(getCurrentHospital(auth)));
    }

    @PostMapping("/requests/{id}/respond")
    public ResponseEntity<RequestDto.RequestResponse> respondToRequest(
            @PathVariable Long id,
            @RequestBody RequestDto.RespondToRequestRequest req,
            Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Hospital hospital = getCurrentHospital(auth);
        return ResponseEntity.ok(requestService.respondToRequest(id, req, hospital, user));
    }

    @PostMapping("/requests/{id}/complete")
    public ResponseEntity<RequestDto.RequestResponse> completeRequest(
            @PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Hospital hospital = getCurrentHospital(auth);
        return ResponseEntity.ok(requestService.markCompleted(id, hospital, user));
    }
}
