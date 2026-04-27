package com.medsync.repository;

import com.medsync.entity.Hospital;
import com.medsync.entity.Medicine;
import com.medsync.entity.MedicineBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, Long> {

    List<MedicineBatch> findByHospital(Hospital hospital);

    List<MedicineBatch> findByMedicineAndHospital(Medicine medicine, Hospital hospital);

    @Query("SELECT b FROM MedicineBatch b WHERE b.hospital = :hospital AND b.expiryDate <= :date AND b.quantity > 0")
    List<MedicineBatch> findNearExpiry(Hospital hospital, LocalDate date);

    @Query("SELECT b FROM MedicineBatch b WHERE b.expiryDate <= :date AND b.quantity > 0 AND b.shareAlert = true")
    List<MedicineBatch> findSharedNearExpiry(LocalDate date);

    @Query("SELECT b FROM MedicineBatch b WHERE b.medicine = :medicine AND b.hospital != :exclude AND b.quantity > 0 AND b.expiryDate > CURRENT_DATE")
    List<MedicineBatch> findAvailableByMedicine(Medicine medicine, Hospital exclude);

    @Query("SELECT SUM(b.quantity) FROM MedicineBatch b WHERE b.hospital = :hospital AND b.medicine = :medicine AND b.expiryDate > CURRENT_DATE")
    Integer getTotalQuantity(Hospital hospital, Medicine medicine);

    @Query("SELECT b FROM MedicineBatch b WHERE b.hospital = :hospital AND b.expiryDate < CURRENT_DATE")
    List<MedicineBatch> findExpiredByHospital(Hospital hospital);
}
