package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Account;
import com.hoandev.pinedrink.entity.CustomerProfile;
import com.hoandev.pinedrink.entity.dto.request.Profile.ChangePasswordRequest;
import com.hoandev.pinedrink.entity.dto.request.Profile.UpdateProfileRequest;
import com.hoandev.pinedrink.entity.dto.response.Auth.AccountResponse;
import com.hoandev.pinedrink.entity.dto.response.FileUploadResponse;
import com.hoandev.pinedrink.enums.FileVisibility;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.repository.AccountRepository;
import com.hoandev.pinedrink.repository.CustomerProfileRepository;
import com.hoandev.pinedrink.security.UserPrincipal;
import com.hoandev.pinedrink.service.FileStorageService;
import com.hoandev.pinedrink.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation of {@link ProfileService}.
 * Handles user profile management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final AccountRepository accountRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AccountResponse getProfile() {
        Account account = getCurrentAccount();
        return mapToAccountResponse(account);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AccountResponse updateProfile(UpdateProfileRequest request) {
        Account account = getCurrentAccount();

        // Update only non-null fields
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            account.setFullName(request.getFullName().trim());
        }

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            String phone = request.getPhone().trim();
            // Check if phone is already used by another account
            if (accountRepository.existsByPhoneAndIdNot(phone, account.getId())) {
                throw new BaseException(ErrorCode.AUTH_015);
            }
            account.setPhone(phone);
        }

        if (request.getAvatarUrl() != null) {
            account.setAvatarUrl(request.getAvatarUrl().trim());
        }

        Account updatedAccount = accountRepository.save(account);

        // Update CustomerProfile fields
        CustomerProfile customerProfile = customerProfileRepository.findByAccountId(account.getId())
                .orElse(null);
        if (customerProfile != null) {
            if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
                customerProfile.setFullName(request.getFullName().trim());
            }
            if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                customerProfile.setPhone(request.getPhone().trim());
            }
            if (request.getDateOfBirth() != null) {
                customerProfile.setDateOfBirth(request.getDateOfBirth());
            }
            if (request.getGender() != null) {
                customerProfile.setGender(request.getGender());
            }
            customerProfileRepository.save(customerProfile);
        }

        log.info("Profile updated for account: {}", updatedAccount.getUsername());

        return mapToAccountResponse(updatedAccount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Account account = getCurrentAccount();

        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
            throw new BaseException(ErrorCode.AUTH_001);
        }

        // Validate new password and confirm password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BaseException(ErrorCode.AUTH_010);
        }

        // Validate new password is different from current password
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BaseException(ErrorCode.AUTH_010);
        }

        // Update password
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        log.info("Password changed successfully for account: {}", account.getUsername());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public FileUploadResponse uploadAvatar(MultipartFile file) {
        Account account = getCurrentAccount();

        // Get old avatar URL before uploading new one
        String oldAvatarUrl = account.getAvatarUrl();

        // Upload new avatar to MinIO (PUBLIC bucket)
        String newAvatarUrl = fileStorageService.uploadFile(file, "avatars", FileVisibility.PUBLIC);

        // Update account with new avatar URL
        account.setAvatarUrl(newAvatarUrl);
        accountRepository.save(account);

        // Delete old avatar if exists
        if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
            fileStorageService.deleteFile(oldAvatarUrl);
        }

        log.info("Avatar uploaded successfully for account: {}", account.getUsername());

        return FileUploadResponse.builder()
                .fileUrl(newAvatarUrl)
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();
    }

    /**
     * Retrieves the currently authenticated account from the security context.
     *
     * @return the authenticated {@link Account}
     * @throws BaseException if no authentication is found or account does not exist
     */
    private Account getCurrentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BaseException(ErrorCode.AUTH_012);
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return accountRepository.findById(principal.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_012));
    }

    /**
     * Maps an {@link Account} entity to an {@link AccountResponse} DTO.
     *
     * @param account the account entity
     * @return the account response DTO
     */
    private AccountResponse mapToAccountResponse(Account account) {
        CustomerProfile customerProfile = customerProfileRepository.findByAccountId(account.getId())
                .orElse(null);

        return AccountResponse.builder()
                .id(account.getId())
                .brandId(account.getBrand() != null ? account.getBrand().getId() : null)
                .username(account.getUsername())
                .fullName(account.getFullName())
                .email(account.getEmail())
                .phone(account.getPhone())
                .avatarUrl(account.getAvatarUrl())
                .status(account.getStatus())
                .lastLoginAt(account.getLastLoginAt())
                .dateOfBirth(customerProfile != null ? customerProfile.getDateOfBirth() : null)
                .gender(customerProfile != null ? customerProfile.getGender() : null)
                .build();
    }

}
