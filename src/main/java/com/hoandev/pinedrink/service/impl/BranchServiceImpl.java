package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Branch;
import com.hoandev.pinedrink.entity.dto.request.Branch.CreateBranchRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.enums.BranchStatus;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.BranchMapper;
import com.hoandev.pinedrink.repository.BranchRepository;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.BranchService;
import com.hoandev.pinedrink.utils.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchServiceImpl implements BranchService {
    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;
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
    public PageResponse<BranchResponse> getAll(Pageable pageable) {
        accessScopeService.assertSystemAccess();
        Page<Branch> branches = branchRepository.findAll(pageable);
        List<BranchResponse> content = branches.getContent().stream().map(branchMapper::toResponse).toList();
        return PageResponse.from(branches, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BranchResponse> getAllActive(Pageable pageable) {
        accessScopeService.assertSystemAccess();
        Page<Branch> branches = branchRepository.findByStatus(BranchStatus.ACTIVE.getValue(), pageable);
        List<BranchResponse> content = branches.getContent().stream().map(branchMapper::toResponse).toList();
        return PageResponse.from(branches, content);
    }

    private Branch getBranchOrThrow(String id) {
        return branchRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
    }
}
