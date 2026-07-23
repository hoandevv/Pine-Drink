package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing {@link RefreshToken} entities.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Finds a refresh token by its SHA-256 hash.
     *
     * @param tokenHash the hashed token value to search for
     * @return an {@link Optional} containing the token if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    /**
     * Deletes all refresh tokens associated with a specific account.
     * Used when revoking all sessions (e.g., after password change).
     *
     * @param accountId the account ID
     */
    void deleteByAccountId(String accountId);
}
