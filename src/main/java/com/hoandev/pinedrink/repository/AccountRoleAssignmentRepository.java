package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.AccountRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
     * Retrieves active assignments with role and scope loaded.
     */
    @Query("""
            select a
            from AccountRoleAssignment a
            join fetch a.role r
            join fetch a.scope s
            left join fetch s.brand b
            left join fetch s.branch br
            where a.account.id = :accountId
              and a.status = 'ACTIVE'
              and (a.expiresAt is null or a.expiresAt > :now)
            order by r.code asc, a.createdAt asc
            """)
    List<AccountRoleAssignment> findActiveAssignmentsByAccountId(String accountId, LocalDateTime now);

    /**
     * Retrieves active assignments for multiple accounts with role and scope loaded.
     */
    @Query("""
            select a
            from AccountRoleAssignment a
            join fetch a.account acc
            join fetch a.role r
            join fetch a.scope s
            left join fetch s.brand b
            left join fetch s.branch br
            where acc.id in :accountIds
              and a.status = 'ACTIVE'
              and (a.expiresAt is null or a.expiresAt > :now)
            order by acc.id asc, r.code asc, a.createdAt asc
            """)
    List<AccountRoleAssignment> findActiveAssignmentsByAccountIdIn(List<String> accountIds, LocalDateTime now);

    /**
     * Retrieves all assignments with role and scope loaded.
     */
    @Query("""
            select a
            from AccountRoleAssignment a
            join fetch a.role r
            join fetch a.scope s
            left join fetch s.brand b
            left join fetch s.branch br
            where a.account.id = :accountId
            order by a.status desc, r.code asc, a.createdAt asc
            """)
    List<AccountRoleAssignment> findDetailedByAccountId(String accountId);

    /**
     * Loads one assignment with role and scope.
     */
    @Query("""
            select a
            from AccountRoleAssignment a
            join fetch a.role r
            join fetch a.scope s
            left join fetch s.brand b
            left join fetch s.branch br
            where a.id = :assignmentId
            """)
    Optional<AccountRoleAssignment> findDetailedById(String assignmentId);

    /**
     * Checks whether an active, non-expired assignment already exists for the same account-role-scope.
     */
    @Query("""
            select case when count(a) > 0 then true else false end
            from AccountRoleAssignment a
            where a.account.id = :accountId
              and a.role.id = :roleId
              and a.scope.id = :scopeId
              and a.status = 'ACTIVE'
              and (a.expiresAt is null or a.expiresAt > :now)
            """)
    boolean existsActiveValidAssignment(String accountId, String roleId, String scopeId, LocalDateTime now);

    /**
     * Retrieves all role assignments for a given role.
     *
     * @param roleId the role ID to search for
     * @return a list of role assignments for the role
     */
    List<AccountRoleAssignment> findByRoleId(String roleId);

    @Query("""
            select distinct a.account.id
            from AccountRoleAssignment a
            where a.role.id = :roleId
              and a.status = 'ACTIVE'
              and (a.expiresAt is null or a.expiresAt > :now)
            """)
    List<String> findActiveAccountIdsByRoleId(String roleId, LocalDateTime now);
}
