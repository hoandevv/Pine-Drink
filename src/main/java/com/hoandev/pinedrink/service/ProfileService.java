package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Profile.ChangePasswordRequest;
import com.hoandev.pinedrink.entity.dto.request.Profile.UpdateProfileRequest;
import com.hoandev.pinedrink.entity.dto.response.Auth.AccountResponse;
import com.hoandev.pinedrink.entity.dto.response.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for user profile management operations.
 */
public interface ProfileService {

    /**
     * Retrieves the currently authenticated user's profile information.
     *
     * @return {@link AccountResponse} containing the current user's profile details
     */
    AccountResponse getProfile();

    /**
     * Updates the currently authenticated user's profile information.
     * Only non-null fields in the request will be updated.
     *
     * @param request containing the fields to update (fullName, phone, avatarUrl, dateOfBirth, gender)
     * @return {@link AccountResponse} containing the updated profile details
     */
    AccountResponse updateProfile(UpdateProfileRequest request);

    /**
     * Changes the currently authenticated user's password.
     * Validates the current password before updating to the new password.
     *
     * @param request containing currentPassword, newPassword, and confirmPassword
     */
    void changePassword(ChangePasswordRequest request);

    /**
     * Uploads a new avatar for the currently authenticated user.
     * Deletes the old avatar if exists.
     *
     * @param file the avatar image file to upload
     * @return {@link FileUploadResponse} containing the uploaded file information
     */
    FileUploadResponse uploadAvatar(MultipartFile file);

}
