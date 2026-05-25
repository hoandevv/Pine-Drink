package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.PickupTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PickupTimeSlotRepository extends JpaRepository<PickupTimeSlot, String> {
    List<PickupTimeSlot> findByBranchIdAndStatus(String branchId, String status);
}
