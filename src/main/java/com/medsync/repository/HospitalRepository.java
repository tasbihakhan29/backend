package com.medsync.repository;

import com.medsync.entity.Hospital;
import com.medsync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    Optional<Hospital> findByUser(User user);
    Optional<Hospital> findByLicenseNumber(String licenseNumber);
    List<Hospital> findByType(Hospital.InstitutionType type);

    @Query("SELECT h FROM Hospital h WHERE h.user.status = 'ACTIVE'")
    List<Hospital> findAllActive();

    @Query("SELECT h FROM Hospital h WHERE h.shareExpiryAlerts = true AND h.user.status = 'ACTIVE'")
    List<Hospital> findSharingExpiryAlerts();

    @Query(value = """
        SELECT h.*, 
               (6371 * acos(cos(radians(:lat)) * cos(radians(h.latitude)) *
               cos(radians(h.longitude) - radians(:lng)) +
               sin(radians(:lat)) * sin(radians(h.latitude)))) AS distance
        FROM hospitals h
        JOIN users u ON h.user_id = u.id
        WHERE u.status = 'ACTIVE' AND h.id != :excludeId
        ORDER BY distance ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Hospital> findNearbyHospitals(double lat, double lng, Long excludeId, int limit);
}
