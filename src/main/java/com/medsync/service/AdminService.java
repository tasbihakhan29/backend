package com.medsync.service;

import com.medsync.entity.*;
import com.medsync.repository.*;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository batchRepository;
    private final MedicineRequestRepository requestRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Hospital> getPendingRegistrations() {
        return hospitalRepository.findAll().stream()
                .filter(h -> h.getUser().getStatus() == User.AccountStatus.PENDING)
                .toList();
    }

    @Transactional
    public Map<String, Object> approveHospital(Long hospitalId, boolean approve, User admin) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("Hospital not found"));

        User.AccountStatus newStatus = approve ? User.AccountStatus.ACTIVE : User.AccountStatus.SUSPENDED;
        hospital.getUser().setStatus(newStatus);
        userRepository.save(hospital.getUser());

        auditService.log(admin, approve ? "APPROVE_HOSPITAL" : "REJECT_HOSPITAL",
                "Hospital", hospitalId, hospital.getHospitalName());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", hospital.getHospitalName() + " has been " + (approve ? "approved" : "rejected"));
        return result;
    }

    public Map<String, Object> getDashboardStats() {
        long totalHospitals = hospitalRepository.findAll().stream()
                .filter(h -> h.getType() == Hospital.InstitutionType.HOSPITAL).count();
        long totalPharmacies = hospitalRepository.findAll().stream()
                .filter(h -> h.getType() == Hospital.InstitutionType.PHARMACY).count();
        long pendingRegistrations = hospitalRepository.findAll().stream()
                .filter(h -> h.getUser().getStatus() == User.AccountStatus.PENDING).count();
        long approvedMedicines = medicineRepository.findByApprovalStatus(Medicine.ApprovalStatus.APPROVED).size();
        long pendingMedicines = medicineRepository.findByApprovalStatus(Medicine.ApprovalStatus.PENDING).size();
        long totalRequests = requestRepository.count();
        long activeRequests = requestRepository.findByStatus(MedicineRequest.RequestStatus.PENDING).size()
                + requestRepository.findByStatus(MedicineRequest.RequestStatus.IN_TRANSIT).size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalHospitals", totalHospitals);
        stats.put("totalPharmacies", totalPharmacies);
        stats.put("pendingRegistrations", pendingRegistrations);
        stats.put("approvedMedicines", approvedMedicines);
        stats.put("pendingMedicines", pendingMedicines);
        stats.put("totalRequests", totalRequests);
        stats.put("activeRequests", activeRequests);
        return stats;
    }

    public List<Hospital> getAllInstitutions() {
        return hospitalRepository.findAll();
    }
}
