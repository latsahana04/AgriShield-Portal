package com.agrishield.repository;

import com.agrishield.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, String> {
    Optional<Medication> findByDrugId(String drugId);
    Optional<Medication> findByNameIgnoreCase(String name);
}
