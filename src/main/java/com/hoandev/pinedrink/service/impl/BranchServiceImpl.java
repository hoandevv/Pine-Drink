package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Branch;
import com.hoandev.pinedrink.entity.Brand;
import com.hoandev.pinedrink.entity.dto.request.Branch.CreateBranchRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchRequest;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchResponse;
import com.hoandev.pinedrink.entity.enums.BranchStatus;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.BranchMapper;
import com.hoandev.pinedrink.repository.BranchRepository;
import com.hoandev.pinedrink.repository.BrandRepository;
import com.hoandev.pinedrink.service.BranchService;
import com.hoandev.pinedrink.utils.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchServiceImpl implements BranchService {
    private final BranchRepository branchRepository;
    private final BrandRepository brandRepository;
    private final BranchMapper branchMapper;
    private final CodeGenerator codeGenerator;

    @Override
    @Transactional
    public BranchResponse create(CreateBranchRequest request) {
        // Verify brand exists
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_003));

        // Generate branch code
        String branchCode = codeGenerator.generate("BR", request.getBrandId());
        
        // Check if generated code already exists (race condition protection)
        if (branchRepository.existsByCode(branchCode)) {
            throw new BaseException(ErrorCode.BRANCH_002);
        }

        Branch branch = branchMapper.toEntity(request, brand);
        branch.setCode(branchCode);
        branch = branchRepository.save(branch);

        log.info("Branch created: id={}, code={}, brandId={}", branch.getId(), branch.getCode(), brand.getId());
        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional
    public BranchResponse update(String id, UpdateBranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));

        branchMapper.updateEntity(branch, request);
        branch = branchRepository.save(branch);

        log.info("Branch updated: id={}, code={}", branch.getId(), branch.getCode());
        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));

        if (BranchStatus.INACTIVE.getValue().equals(branch.getStatus())) {
            throw new BaseException(ErrorCode.BRANCH_004);
        }

        // Soft delete
        branch.setStatus(BranchStatus.INACTIVE.getValue());
        branchRepository.save(branch);

        log.info("Branch deleted (soft): id={}, code={}", branch.getId(), branch.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getById(String id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));

        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getAllByBrandId(String brandId) {
        // Verify brand exists
        brandRepository.findById(brandId)
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_003));

        List<Branch> branches = branchRepository.findByBrandId(brandId);

        return branches.stream()
                .map(branchMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getAllActiveByBrandId(String brandId) {
        // Verify brand exists
        brandRepository.findById(brandId)
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_003));

        List<Branch> branches = branchRepository.findByBrandIdAndStatus(brandId, BranchStatus.ACTIVE.getValue());

        return branches.stream()
                .map(branchMapper::toResponse)
                .collect(Collectors.toList());
    }
}
