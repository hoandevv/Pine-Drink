package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, String> {
    List<RolePermission> findByRoleId(String roleId);
    List<RolePermission> findByPermissionId(String permissionId);
}
