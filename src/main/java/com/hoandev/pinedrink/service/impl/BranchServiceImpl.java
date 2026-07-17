package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Branch;
import com.hoandev.pinedrink.entity.BranchHours;
import com.hoandev.pinedrink.entity.dto.request.Branch.CreateBranchRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchOptionResponse;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchResponse;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchSummaryResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.enums.BranchStatus;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.BranchMapper;
import com.hoandev.pinedrink.mapper.BranchHoursMapper;
import com.hoandev.pinedrink.repository.BranchRepository;
import com.hoandev.pinedrink.repository.BranchHoursRepository;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.BranchService;
import com.hoandev.pinedrink.utils.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoandev.pinedrink.security.scope.AccessScopeContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchServiceImpl implements BranchService {
    private final BranchRepository branchRepository;
    private final BranchHoursRepository branchHoursRepository;
    private final BranchMapper branchMapper;
    private final BranchHoursMapper branchHoursMapper;
    private final CodeGenerator codeGenerator;
    private final AccessScopeService accessScopeService;

    @Override
    @Transactional
    public BranchResponse create(CreateBranchRequest request) {
        accessScopeService.assertSystemAccess();
        String branchCode = codeGenerator.generate("BR", "BRANCH");
        if (branchRepository.existsByCode(branchCode)) {
            throw new BaseException(ErrorCode.BRANCH_002);
        }
        Branch branch = branchMapper.toEntity(request);
        branch.setCode(branchCode);
        branch = branchRepository.save(branch);
        log.info("Branch created: id={}, code={}", branch.getId(), branch.getCode());
        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional
    public BranchResponse update(String id, UpdateBranchRequest request) {
        accessScopeService.assertCanManageBranch(id);
        Branch branch = getBranchOrThrow(id);
        branchMapper.updateEntity(branch, request);
        branch = branchRepository.save(branch);
        log.info("Branch updated: id={}, code={}", branch.getId(), branch.getCode());
        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional
    public BranchResponse updateStatus(String id, UpdateBranchStatusRequest request) {
        accessScopeService.assertCanManageBranch(id);
        Branch branch = getBranchOrThrow(id);
        branch.setStatus(request.getStatus());
        branch = branchRepository.save(branch);
        log.info("Branch status updated: id={}, code={}, status={}", branch.getId(), branch.getCode(), branch.getStatus());
        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional
    public void delete(String id) {
        accessScopeService.assertCanDeleteBranch(id);
        Branch branch = getBranchOrThrow(id);
        if (BranchStatus.INACTIVE.getValue().equals(branch.getStatus())) {
            throw new BaseException(ErrorCode.BRANCH_004);
        }
        branch.setStatus(BranchStatus.INACTIVE.getValue());
        branchRepository.save(branch);
        log.info("Branch deleted (soft): id={}, code={}", branch.getId(), branch.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getById(String id) {
        accessScopeService.assertCanAccessBranch(id);
        return branchMapper.toResponse(getBranchOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BranchSummaryResponse> getSummaries(Pageable pageable) {
        return getBranchSummaries(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BranchOptionResponse> getActiveOptions(Pageable pageable) {
        return getActiveBranchOptions(pageable);
    }

    private PageResponse<BranchSummaryResponse> getBranchSummaries(Pageable pageable) {
        AccessScopeContext scope = accessScopeService.resolveCurrentScope();
        Page<Branch> branches = scope.fullAccess()
                ? branchRepository.findAll(pageable)
                : findScopedBranches(scope, pageable);
        List<BranchSummaryResponse> content = branches.getContent().stream().map(branchMapper::toSummaryResponse).toList();
        return PageResponse.from(branches, content);
    }

    private PageResponse<BranchOptionResponse> getActiveBranchOptions(Pageable pageable) {
        AccessScopeContext scope = accessScopeService.resolveCurrentScope();
        Page<Branch> branches = scope.fullAccess()
                ? branchRepository.findByStatus(BranchStatus.ACTIVE.getValue(), pageable)
                : findScopedActiveBranches(scope, pageable);
        List<BranchOptionResponse> content = mapOptionsWithHours(branches.getContent());
        return PageResponse.from(branches, content);
    }

    private List<BranchOptionResponse> mapOptionsWithHours(List<Branch> branches) {
        if (branches.isEmpty()) {
            return List.of();
        }

        Map<String, List<BranchHours>> hoursByBranchId = branchHoursRepository
                .findByBranchIdIn(branches.stream().map(Branch::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(branchHours -> branchHours.getBranch().getId()));

        return branches.stream()
                .map(branch -> {
                    BranchOptionResponse response = branchMapper.toOptionResponse(branch);
                    response.setHours(hoursByBranchId.getOrDefault(branch.getId(), List.of())
                            .stream()
                            .map(branchHoursMapper::toResponse)
                            .toList());
                    return response;
                })
                .toList();
    }

    private Page<Branch> findScopedBranches(AccessScopeContext scope, Pageable pageable) {
        if (scope.branchIds().isEmpty()) {
            return Page.empty(pageable);
        }
        return branchRepository.findByIdIn(scope.branchIds(), pageable);
    }

    private Page<Branch> findScopedActiveBranches(AccessScopeContext scope, Pageable pageable) {
        if (scope.branchIds().isEmpty()) {
            return Page.empty(pageable);
        }
        return branchRepository.findByIdInAndStatus(scope.branchIds(), BranchStatus.ACTIVE.getValue(), pageable);
    }

    private Branch getBranchOrThrow(String id) {
        return branchRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
    }
}
