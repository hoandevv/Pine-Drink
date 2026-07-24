package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Branch;
import com.hoandev.pinedrink.entity.BranchHours;
import com.hoandev.pinedrink.entity.dto.request.Branch.CreateBranchHoursRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchHoursRequest;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchHoursResponse;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.BranchHoursMapper;
import com.hoandev.pinedrink.repository.BranchHoursRepository;
import com.hoandev.pinedrink.repository.BranchRepository;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.BranchHoursService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class BranchHoursServiceImpl implements BranchHoursService {
    private final BranchHoursRepository branchHoursRepository;
    private final BranchRepository branchRepository;
    private final BranchHoursMapper branchHoursMapper;
    private final AccessScopeService accessScopeService;

    public BranchHoursServiceImpl(BranchHoursRepository branchHoursRepository, BranchRepository branchRepository, BranchHoursMapper branchHoursMapper, AccessScopeService accessScopeService) {
        this.branchHoursRepository = branchHoursRepository;
        this.branchRepository = branchRepository;
        this.branchHoursMapper = branchHoursMapper;
        this.accessScopeService = accessScopeService;
    }

    @Override
    @Transactional
    public BranchHoursResponse create(String branchId, CreateBranchHoursRequest request) {
        accessScopeService.assertCanManageBranch(branchId);
        validateTimeRange(request.getOpenTime(), request.getCloseTime());
        if (branchHoursRepository.existsByBranchIdAndDayOfWeek(branchId, request.getDayOfWeek())) {
            throw new BaseException(ErrorCode.BRANCH_006);
        }
        Branch branch = getBranchOrThrow(branchId);
        BranchHours branchHours = branchHoursMapper.toEntity(request, branch);
        branchHours = branchHoursRepository.save(branchHours);
        log.info("Branch hours created: id={}, branchId={}, dayOfWeek={}", branchHours.getId(), branchId, branchHours.getDayOfWeek());
        return branchHoursMapper.toResponse(branchHours);
    }

    @Override
    @Transactional
    public BranchHoursResponse update(String branchId, String id, UpdateBranchHoursRequest request) {
        accessScopeService.assertCanManageBranch(branchId);
        BranchHours branchHours = getBranchHoursOrThrow(branchId, id);
        int nextDayOfWeek = request.getDayOfWeek() == null ? branchHours.getDayOfWeek() : request.getDayOfWeek();
        if (branchHoursRepository.existsByBranchIdAndDayOfWeekAndIdNot(branchId, nextDayOfWeek, id)) {
            throw new BaseException(ErrorCode.BRANCH_006);
        }
        branchHoursMapper.updateEntity(branchHours, request);
        validateTimeRange(branchHours.getOpenTime(), branchHours.getCloseTime());
        branchHours = branchHoursRepository.save(branchHours);
        log.info("Branch hours updated: id={}, branchId={}, dayOfWeek={}", branchHours.getId(), branchId, branchHours.getDayOfWeek());
        return branchHoursMapper.toResponse(branchHours);
    }

    @Override
    @Transactional
    public void delete(String branchId, String id) {
        accessScopeService.assertCanManageBranch(branchId);
        BranchHours branchHours = getBranchHoursOrThrow(branchId, id);
        branchHoursRepository.delete(branchHours);
        log.info("Branch hours deleted: id={}, branchId={}", id, branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchHoursResponse getById(String branchId, String id) {
        return branchHoursMapper.toResponse(getBranchHoursOrThrow(branchId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchHoursResponse> getByBranch(String branchId) {
        getBranchOrThrow(branchId);
        return branchHoursRepository.findByBranchId(branchId).stream()
                .sorted(Comparator.comparingInt(BranchHours::getDayOfWeek))
                .map(branchHoursMapper::toResponse)
                .toList();
    }

    private Branch getBranchOrThrow(String branchId) {
        return branchRepository.findById(branchId).orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
    }

    private BranchHours getBranchHoursOrThrow(String branchId, String id) {
        return branchHoursRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_005));
    }

    private void validateTimeRange(LocalTime openTime, LocalTime closeTime) {
        if (openTime == null || closeTime == null || !openTime.isBefore(closeTime)) {
            throw new BaseException(ErrorCode.BRANCH_007);
        }
    }
}
