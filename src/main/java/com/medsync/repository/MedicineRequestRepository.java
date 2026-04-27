package com.medsync.repository;

import com.medsync.entity.Hospital;
import com.medsync.entity.MedicineRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MedicineRequestRepository extends JpaRepository<MedicineRequest, Long> {
    List<MedicineRequest> findByRequester(Hospital requester);
    List<MedicineRequest> findBySupplier(Hospital supplier);
    List<MedicineRequest> findByStatus(MedicineRequest.RequestStatus status);
    List<MedicineRequest> findByRequesterOrSupplierOrderByCreatedAtDesc(Hospital requester, Hospital supplier);
    java.util.Optional<MedicineRequest> findByTrackingCode(String trackingCode);
}
