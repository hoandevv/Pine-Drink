package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Topping;
import com.hoandev.pinedrink.entity.dto.request.Topping.CreateToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.Topping.UpdateToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.Topping.UpdateToppingStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Topping.ToppingResponse;
import com.hoandev.pinedrink.entity.enums.FileVisibility;
import com.hoandev.pinedrink.entity.enums.ToppingStatus;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.ToppingMapper;
import com.hoandev.pinedrink.repository.ToppingRepository;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.FileStorageService;
import com.hoandev.pinedrink.service.ToppingService;
import com.hoandev.pinedrink.utils.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToppingServiceImpl implements ToppingService {
    private final ToppingRepository toppingRepository;
    private final ToppingMapper toppingMapper;
    private final AccessScopeService accessScopeService;
    private final CodeGenerator codeGenerator;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ToppingResponse create(CreateToppingRequest request) {
        return create(request, null);
    }

    @Override
    @Transactional
    public ToppingResponse create(CreateToppingRequest request, MultipartFile imageFile) {
        accessScopeService.assertSystemAccess();
        String toppingCode = resolveCreateCode();
        Topping topping = toppingMapper.toEntity(request);
        String uploadedImageUrl = uploadToppingImage(imageFile);
        if (uploadedImageUrl != null) {
            topping.setImageUrl(uploadedImageUrl);
        }
        topping.setCode(toppingCode);
        topping.setStatus(ToppingStatus.ACTIVE.getValue());
        try {
            topping = toppingRepository.save(topping);
            log.info("Topping created: id={}, code={}", topping.getId(), topping.getCode());
            return toppingMapper.toResponse(topping);
        } catch (RuntimeException ex) {
            deleteManagedToppingImage(uploadedImageUrl);
            throw ex;
        }
    }

    @Override
    @Transactional
    public ToppingResponse update(String id, UpdateToppingRequest request) {
        return update(id, request, null);
    }

    @Override
    @Transactional
    public ToppingResponse update(String id, UpdateToppingRequest request, MultipartFile imageFile) {
        accessScopeService.assertSystemAccess();
        Topping topping = getToppingOrThrow(id);
        String oldImageUrl = topping.getImageUrl();
        toppingMapper.updateEntity(topping, request);
        String uploadedImageUrl = uploadToppingImage(imageFile);
        if (uploadedImageUrl != null) {
            topping.setImageUrl(uploadedImageUrl);
        }
        String newImageUrl = topping.getImageUrl();
        try {
            topping = toppingRepository.save(topping);
            deleteReplacedToppingImage(oldImageUrl, newImageUrl);
            log.info("Topping updated: id={}, code={}", topping.getId(), topping.getCode());
            return toppingMapper.toResponse(topping);
        } catch (RuntimeException ex) {
            if (uploadedImageUrl != null) {
                deleteManagedToppingImage(uploadedImageUrl);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public ToppingResponse updateStatus(String id, UpdateToppingStatusRequest request) {
        accessScopeService.assertSystemAccess();
        Topping topping = getToppingOrThrow(id);
        topping.setStatus(request.getStatus().getValue());
        topping = toppingRepository.save(topping);
        log.info("Topping status updated: id={}, code={}, status={}", topping.getId(), topping.getCode(), topping.getStatus());
        return toppingMapper.toResponse(topping);
    }

    @Override
    @Transactional
    public void delete(String id) {
        accessScopeService.assertSystemAccess();
        Topping topping = getToppingOrThrow(id);
        if (ToppingStatus.INACTIVE.getValue().equals(topping.getStatus())) {
            throw new BaseException(ErrorCode.TOPPING_003);
        }
        topping.setStatus(ToppingStatus.INACTIVE.getValue());
        toppingRepository.save(topping);
        log.info("Topping deleted (soft): id={}, code={}", topping.getId(), topping.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public ToppingResponse getById(String id) {
        return toppingMapper.toResponse(getToppingOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ToppingResponse> getAll(Pageable pageable) {
        Page<Topping> toppings = toppingRepository.findAll(pageable);
        List<ToppingResponse> content = toppings.getContent().stream().map(toppingMapper::toResponse).toList();
        return PageResponse.from(toppings, content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ToppingResponse> getAllActive() {
        return toppingRepository.findByStatus(ToppingStatus.ACTIVE.getValue())
                .stream()
                .map(toppingMapper::toResponse)
                .toList();
    }

    private Topping getToppingOrThrow(String id) {
        return toppingRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.TOPPING_001));
    }

    private String resolveCreateCode() {
        String generatedCode = codeGenerator.generate("TP", "TOPPING");
        if (toppingRepository.existsByCode(generatedCode)) {
            throw new BaseException(ErrorCode.TOPPING_002);
        }
        return generatedCode;
    }

    private String uploadToppingImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        return fileStorageService.uploadFile(imageFile, "toppings", FileVisibility.PUBLIC);
    }

    private void deleteReplacedToppingImage(String oldImageUrl, String newImageUrl) {
        if (oldImageUrl == null || oldImageUrl.isBlank()) {
            return;
        }
        if (oldImageUrl.equals(newImageUrl)) {
            return;
        }
        deleteManagedToppingImage(oldImageUrl);
    }

    private void deleteManagedToppingImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        if (!isManagedToppingImage(imageUrl)) {
            return;
        }
        fileStorageService.deleteFile(imageUrl);
    }

    private boolean isManagedToppingImage(String imageUrl) {
        return imageUrl.startsWith("toppings/") || imageUrl.contains("/toppings/");
    }
}
