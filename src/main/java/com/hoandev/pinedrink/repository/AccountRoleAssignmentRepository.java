package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.AccountRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing {@link AccountRoleAssignment} entities.
 */
@Repository
public interface AccountRoleAssignmentRepository extends JpaRepository<AccountRoleAssignment, String> {

    /**
     * Retrieves all role assignments for a given account.
     *
     * @param accountId the account ID to search for
     * @return a list of role assignments for the account
     */
    List<AccountRoleAssignment> findByAccountId(String accountId);

    /**
     * Retrieves active role codes for a given account, using a join fetch
     * to avoid N+1 queries on the role relationship.
     *
     * @param accountId the account ID to search for
     * @param now       the reference time for expiry checks
     * @return a list of active role codes assigned to the account
     */
    @Query("""
            select r.code
            from AccountRoleAssignment a
            join a.role r
            where a.account.id = :accountId
              and a.status = 'ACTIVE'
              and (a.expiresAt is null or a.expiresAt > :now)
            """)
    List<String> findActiveRoleCodesByAccountId(String accountId, LocalDateTime now);

    /**
     * Retrieves all role assignments for a given role.
     *
     * @param roleId the role ID to search for
     * @return a list of role assignments for the role
     */
    List<AccountRoleAssignment> findByRoleId(String roleId);
}
