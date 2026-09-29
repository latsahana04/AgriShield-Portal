package com.agrishield.repository;

import com.agrishield.entity.Animal;
import com.agrishield.entity.AnimalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, String> {
    Optional<Animal> findByTagId(String tagId);
    List<Animal> findByTagIdIn(List<String> tagIds);
    List<Animal> findByStatus(AnimalStatus status);
}
