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
     * Finds a refresh token by the owning account's ID.
     *
     * @param accountId the account ID to search for
     * @return an {@link Optional} containing the token if found
     */
    Optional<RefreshToken> findByAccountId(String accountId);
}
