package com.medsync.controller;

import com.medsync.entity.User;
import com.medsync.repository.UserRepository;
import com.medsync.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('CITY_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final MedicineService medicineService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/registrations/pending")
    public ResponseEntity<?> getPendingRegistrations() {
        return ResponseEntity.ok(adminService.getPendingRegistrations());
    }

    @PostMapping("/registrations/{hospitalId}/approve")
    public ResponseEntity<?> approveHospital(@PathVariable Long hospitalId,
                                              @RequestParam boolean approve,
                                              Authentication auth) {
        User admin = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(adminService.approveHospital(hospitalId, approve, admin));
    }

    @GetMapping("/medicines/pending")
    public ResponseEntity<?> getPendingMedicines() {
        return ResponseEntity.ok(medicineService.getPendingMedicines());
    }

    @PostMapping("/medicines/{id}/approve")
    public ResponseEntity<?> approveMedicine(@PathVariable Long id,
                                              @RequestParam boolean approve,
                                              Authentication auth) {
        User admin = userRepository.findByUsername(auth.getName()).orElseThrow();
        medicineService.approveMedicine(id, approve, admin);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/institutions")
    public ResponseEntity<?> getAllInstitutions() {
        return ResponseEntity.ok(adminService.getAllInstitutions());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs() {
        return ResponseEntity.ok(auditService.getRecentLogs());
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getAllRequests(
            @org.springframework.beans.factory.annotation.Autowired com.medsync.service.RequestService requestService) {
        return ResponseEntity.ok(requestService.getAllRequests());
    }
}
