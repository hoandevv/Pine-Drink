package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
    Optional<Permission> findByCode(String code);
    List<Permission> findByModule(String module);
    /**
     * Finds all permissions with the specified status, ordered by module and code.
     *
     * @param status the status of the permissions to find
     * @return a list of permissions with the specified status
     */
    List<Permission> findByStatusOrderByModuleAscCodeAsc(String status);
    /**
     * Finds all permissions with the specified codes and status, ordered by module and code.
     *
     * @param codes  the codes of the permissions to find
     * @param status the status of the permissions to find
     * @return a list of permissions with the specified codes and status
     */
    List<Permission> findByCodeInAndStatus(List<String> codes, String status);
}
