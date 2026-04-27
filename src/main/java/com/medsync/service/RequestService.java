package com.medsync.service;

import com.medsync.dto.RequestDto;
import com.medsync.entity.*;
import com.medsync.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {

    private final MedicineRequestRepository requestRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository batchRepository;
    private final HospitalRepository hospitalRepository;
    private final AuditService auditService;

    @Transactional
    public RequestDto.RequestResponse createRequest(RequestDto.CreateRequestRequest req,
                                                    Hospital requester, User user) {
        Medicine medicine = medicineRepository.findById(req.getMedicineId())
                .orElseThrow(() -> new RuntimeException("Medicine not found"));
        Hospital supplier = hospitalRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        if (requester.getId().equals(supplier.getId())) {
            throw new RuntimeException("Cannot request from yourself");
        }

        String trackingCode = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        MedicineRequest request = MedicineRequest.builder()
                .requester(requester)
                .supplier(supplier)
                .medicine(medicine)
                .quantityRequested(req.getQuantityRequested())
                .urgency(req.getUrgency())
                .status(MedicineRequest.RequestStatus.PENDING)
                .requestNote(req.getRequestNote())
                .trackingCode(trackingCode)
                .build();

        request = requestRepository.save(request);
        auditService.log(user, "CREATE_REQUEST", "MedicineRequest", request.getId(),
                "Request for " + medicine.getName() + " qty:" + req.getQuantityRequested());

        return toResponse(request, false);
    }

    @Transactional
    public RequestDto.RequestResponse respondToRequest(Long requestId,
                                                       RequestDto.RespondToRequestRequest req,
                                                       Hospital supplier, User user) {
        MedicineRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getSupplier().getId().equals(supplier.getId())) {
            throw new RuntimeException("Not authorized");
        }
        if (request.getStatus() != MedicineRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Request already processed");
        }

        request.setStatus(req.getStatus());
        request.setResponseNote(req.getResponseNote());

        if (req.getStatus() == MedicineRequest.RequestStatus.ACCEPTED) {
            int approvedQty = req.getQuantityApproved() != null
                    ? req.getQuantityApproved()
                    : request.getQuantityRequested();
            request.setQuantityApproved(approvedQty);

            // FIFO stock deduction — oldest expiry first
            List<MedicineBatch> batches = batchRepository
                    .findByMedicineAndHospital(request.getMedicine(), supplier)
                    .stream()
                    .filter(b -> b.getQuantity() > 0
                            && b.getExpiryDate().isAfter(java.time.LocalDate.now()))
                    .sorted((a, b2) -> a.getExpiryDate().compareTo(b2.getExpiryDate()))
                    .collect(Collectors.toList());

            int remaining = approvedQty;
            for (MedicineBatch batch : batches) {
                if (remaining <= 0) break;
                int deduct = Math.min(batch.getQuantity(), remaining);
                batch.setQuantity(batch.getQuantity() - deduct);
                batchRepository.save(batch);
                remaining -= deduct;
            }

            request.setStatus(MedicineRequest.RequestStatus.IN_TRANSIT);
        }

        // ✅ Request saved successfully
        request = requestRepository.save(request);
        auditService.log(user, "RESPOND_REQUEST", "MedicineRequest", requestId,
                "Status: " + req.getStatus().name());

        return toResponse(request, true);
    }

    @Transactional
    public RequestDto.RequestResponse markCompleted(Long requestId, Hospital requester, User user) {
        MedicineRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getRequester().getId().equals(requester.getId())) {
            throw new RuntimeException("Not authorized");
        }
        if (request.getStatus() != MedicineRequest.RequestStatus.IN_TRANSIT) {
            throw new RuntimeException("Request not in transit");
        }

        // Add stock to requester
        MedicineBatch newBatch = MedicineBatch.builder()
                .medicine(request.getMedicine())
                .hospital(requester)
                .batchNumber("TRANSFER-" + request.getTrackingCode())
                .quantity(request.getQuantityApproved())
                .expiryDate(java.time.LocalDate.now().plusYears(1))
                .shareAlert(false)
                .build();
        batchRepository.save(newBatch);

        request.setStatus(MedicineRequest.RequestStatus.COMPLETED);
        request = requestRepository.save(request);

        auditService.log(user, "COMPLETE_REQUEST", "MedicineRequest", requestId,
                "Transfer completed for " + request.getMedicine().getName());

        return toResponse(request, true);
    }

    public List<RequestDto.RequestResponse> getMyRequests(Hospital hospital, boolean isAdmin) {
        return requestRepository.findByRequesterOrSupplierOrderByCreatedAtDesc(hospital, hospital)
                .stream()
                .map(r -> toResponse(r, isAdmin))
                .collect(Collectors.toList());
    }

    public List<RequestDto.RequestResponse> getAllRequests() {
        return requestRepository.findAll().stream()
                .map(r -> toResponse(r, true))
                .collect(Collectors.toList());
    }

    public List<RequestDto.RequestResponse> getIncomingRequests(Hospital supplier) {
        return requestRepository.findBySupplier(supplier)
                .stream()
                .filter(r -> r.getStatus() == MedicineRequest.RequestStatus.PENDING)
                .map(r -> toResponse(r, false))
                .collect(Collectors.toList());
    }

    private RequestDto.RequestResponse toResponse(MedicineRequest r, boolean showFullDetails) {
        return RequestDto.RequestResponse.builder()
                .id(r.getId())
                .trackingCode(r.getTrackingCode())
                .requesterCode(showFullDetails
                        ? r.getRequester().getHospitalName()
                        : "HOSP-" + r.getRequester().getId())
                .supplierCode(showFullDetails
                        ? r.getSupplier().getHospitalName()
                        : "HOSP-" + r.getSupplier().getId())
                .requesterName(showFullDetails ? r.getRequester().getHospitalName() : null)
                .supplierName(showFullDetails ? r.getSupplier().getHospitalName() : null)
                .medicineName(r.getMedicine().getName())
                .quantityRequested(r.getQuantityRequested())
                .quantityApproved(r.getQuantityApproved())
                .urgency(r.getUrgency().name())
                .status(r.getStatus().name())
                .requestNote(r.getRequestNote())
                .responseNote(r.getResponseNote())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}