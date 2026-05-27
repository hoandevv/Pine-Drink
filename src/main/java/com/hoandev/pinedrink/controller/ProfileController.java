package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Profile.ChangePasswordRequest;
import com.hoandev.pinedrink.entity.dto.request.Profile.UpdateProfileRequest;
import com.hoandev.pinedrink.entity.dto.response.Auth.AccountResponse;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.FileUploadResponse;
import com.hoandev.pinedrink.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for user profile management operations.
 * <p>
 * All endpoints require authentication via Bearer token in the Authorization header.
 * Responses are wrapped in {@link BaseResponse} with HTTP 200 (OK) on success.
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Retrieves the currently authenticated user's profile information.
     *
     * @return {@code 200 OK} with {@link AccountResponse} containing profile details
     */
    @GetMapping
    public ResponseEntity<BaseResponse<AccountResponse>> getProfile() {
        log.debug("Get profile request received");
        AccountResponse response = profileService.getProfile();
        return ResponseEntity.ok(
                BaseResponse.success(response)
        );
    }

    /**
     * Updates the currently authenticated user's profile information.
     * <p>
     * Only non-null fields in the request will be updated.
     * Validates that phone number is not already used by another account.
     *
     * @param request containing fields to update (fullName, phone, avatarUrl)
     * @return {@code 200 OK} with {@link AccountResponse} containing updated profile
     */
    @PutMapping
    public ResponseEntity<BaseResponse<AccountResponse>> updateProfile(
            @RequestBody @Valid UpdateProfileRequest request
    ) {
        log.debug("Update profile request received");
        AccountResponse response = profileService.updateProfile(request);
        return ResponseEntity.ok(
                BaseResponse.success(response, "Profile updated successfully")
        );
    }

    /**
     * Changes the currently authenticated user's password.
     * <p>
     * Validates:
     * - Current password is correct
     * - New password matches confirm password
     * - New password is different from current password
     * - New password meets strength requirements
     *
     * @param request containing currentPassword, newPassword, and confirmPassword
     * @return {@code 200 OK} with success message
     */
    @PutMapping("/password")
    public ResponseEntity<BaseResponse<Void>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        log.debug("Change password request received");
        profileService.changePassword(request);
        return ResponseEntity.ok(
                BaseResponse.success(null, "Password changed successfully")
        );
    }

    /**
     * Uploads a new avatar for the currently authenticated user.
     * <p>
     * Validates file type (jpg, jpeg, png, gif, webp) and size (max 5MB).
     * Automatically deletes the old avatar if exists.
     *
     * @param file the avatar image file to upload
     * @return {@code 200 OK} with {@link FileUploadResponse} containing uploaded file info
     */
    @PostMapping("/avatar")
    public ResponseEntity<BaseResponse<FileUploadResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file
    ) {
        log.debug("Upload avatar request received");
        FileUploadResponse response = profileService.uploadAvatar(file);
        return ResponseEntity.ok(
                BaseResponse.success(response, "Avatar uploaded successfully")
        );
    }

}
