package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.BranchHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchHoursRepository extends JpaRepository<BranchHours, String> {
    List<BranchHours> findByBranchId(String branchId);
}
