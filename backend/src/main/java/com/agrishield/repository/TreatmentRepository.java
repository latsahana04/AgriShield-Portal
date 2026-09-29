package com.agrishield.repository;

import com.agrishield.entity.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, String> {
    Optional<Treatment> findByCompositeKey(String compositeKey);
    boolean existsByCompositeKey(String compositeKey);
    List<Treatment> findByAnimalTagIdOrderByAdministeredAtDesc(String animalTagId);
}
