package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, String> {
    /**
     * Finds all role permissions for the specified role ID.
     *
     * @param roleId the ID of the role
     * @return a list of role permissions for the specified role ID
     */
    List<RolePermission> findByRoleId(String roleId);

    @Query("""
            select distinct p.code
            from AccountRoleAssignment ara
            join ara.role r
            join RolePermission rp on rp.role.id = r.id
            join rp.permission p
            where ara.account.id = :accountId
              and ara.status = 'ACTIVE'
              and (ara.expiresAt is null or ara.expiresAt > :now)
              and rp.status = 'ACTIVE'
              and p.status = 'ACTIVE'
            order by p.code asc
            """)
    List<String> findActivePermissionCodesByAccountId(String accountId, LocalDateTime now);

    @Query("""
            select rp
            from RolePermission rp
            join fetch rp.role r
            join fetch rp.permission p
            where rp.status = 'ACTIVE'
              and r.status = 'ACTIVE'
              and p.status = 'ACTIVE'
            order by r.code asc, p.code asc
            """)
    List<RolePermission> findActiveMatrixEntries();
}
