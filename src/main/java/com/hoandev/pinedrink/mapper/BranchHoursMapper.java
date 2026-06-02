package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Branch;
import com.hoandev.pinedrink.entity.BranchHours;
import com.hoandev.pinedrink.entity.dto.request.Branch.CreateBranchHoursRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchHoursRequest;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchHoursResponse;
import org.springframework.stereotype.Component;

@Component
public class BranchHoursMapper {

    public BranchHoursResponse toResponse(BranchHours branchHours) {
        if (branchHours == null) {
            return null;
        }

        return BranchHoursResponse.builder()
                .id(branchHours.getId())
                .branchId(branchHours.getBranch().getId())
                .dayOfWeek(branchHours.getDayOfWeek())
                .openTime(branchHours.getOpenTime())
                .closeTime(branchHours.getCloseTime())
                .closed(branchHours.isClosed())
                .status(branchHours.getStatus())
                .createdAt(branchHours.getCreatedAt())
                .updatedAt(branchHours.getUpdatedAt())
                .build();
    }

    public BranchHours toEntity(CreateBranchHoursRequest request, Branch branch) {
        if (request == null) {
            return null;
        }

        BranchHours branchHours = new BranchHours();
        branchHours.setBranch(branch);
        branchHours.setDayOfWeek(request.getDayOfWeek());
        branchHours.setOpenTime(request.getOpenTime());
        branchHours.setCloseTime(request.getCloseTime());
        branchHours.setClosed(request.isClosed());
        return branchHours;
    }

    public void updateEntity(BranchHours branchHours, UpdateBranchHoursRequest request) {
        if (branchHours == null || request == null) {
            return;
        }

        if (request.getDayOfWeek() != null) {
            branchHours.setDayOfWeek(request.getDayOfWeek());
        }
        if (request.getOpenTime() != null) {
            branchHours.setOpenTime(request.getOpenTime());
        }
        if (request.getCloseTime() != null) {
            branchHours.setCloseTime(request.getCloseTime());
        }
        if (request.getClosed() != null) {
            branchHours.setClosed(request.getClosed());
        }
    }
}
