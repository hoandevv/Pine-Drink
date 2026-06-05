package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing {@link Account} entities.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, String>, JpaSpecificationExecutor<Account> {

    /**
     * Finds an account by its unique username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the account if found
     */
    Optional<Account> findByUsername(String username);

    /**
     * Finds an account by its unique email address.
     *
     * @param email the email to search for
     * @return an {@link Optional} containing the account if found
     */
    Optional<Account> findByEmail(String email);

    Optional<Account> findByAuthProviderAndProviderId(String authProvider, String providerId);

    /**
     * Checks whether an account with the given username already exists.
     *
     * @param username the username to check
     * @return true if an account with the username exists
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether an account with the given email already exists.
     *
     * @param email the email to check
     * @return true if an account with the email exists
     */
    boolean existsByEmail(String email);

    /**
     * Checks whether an account with the given phone number already exists.
     *
     * @param phone the phone number to check
     * @return true if an account with the phone number exists
     */
    boolean existsByPhone(String phone);

    /**
     * Checks whether an account with the given phone number exists,
     * excluding the account with the specified ID.
     *
     * @param phone the phone number to check
     * @param id the account ID to exclude from the check
     * @return true if another account with the phone number exists
     */
    boolean existsByPhoneAndIdNot(String phone, String id);

    /**
     * Checks whether an account with the given email exists,
     * excluding the account with the specified ID.
     *
     * @param email the email to check
     * @param id the account ID to exclude from the check
     * @return true if another account with the email exists
     */
    boolean existsByEmailAndIdNot(String email, String id);
}
