package com.agrishield.repository;

import com.agrishield.entity.VetShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface VetShiftRepository extends JpaRepository<VetShift, String> {

    List<VetShift> findByVetId(String vetId);

    List<VetShift> findByVetIdAndShiftDate(String vetId, LocalDate shiftDate);

    @Query("SELECT s FROM VetShift s WHERE s.vetId = :vetId AND s.shiftDate = :shiftDate AND :time BETWEEN s.startTime AND s.endTime")
    List<VetShift> findActiveShift(
        @Param("vetId") String vetId,
        @Param("shiftDate") LocalDate shiftDate,
        @Param("time") LocalTime time
    );
}
