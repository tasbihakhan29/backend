package com.medsync.repository;

import com.medsync.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByApprovalStatus(Medicine.ApprovalStatus status);
    Optional<Medicine> findByBarcode(String barcode);
    List<Medicine> findByNameContainingIgnoreCaseAndApprovalStatus(String name, Medicine.ApprovalStatus status);
    boolean existsByBarcode(String barcode);
}
