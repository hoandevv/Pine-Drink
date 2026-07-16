package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.BranchHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchHoursRepository extends JpaRepository<BranchHours, String> {

    Optional<BranchHours> findByBranchIdAndDayOfWeek(String branchId, int dayOfWeek);

    List<BranchHours> findByBranchId(String branchId);

    Optional<BranchHours> findByIdAndBranchId(String id, String branchId);

    boolean existsByBranchIdAndDayOfWeek(String branchId, int dayOfWeek);

    boolean existsByBranchIdAndDayOfWeekAndIdNot(String branchId, int dayOfWeek, String id);
}
