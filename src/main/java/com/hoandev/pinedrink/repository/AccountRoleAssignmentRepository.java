package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.AccountRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRoleAssignmentRepository extends JpaRepository<AccountRoleAssignment, String> {
    List<AccountRoleAssignment> findByAccountId(String accountId);
    List<AccountRoleAssignment> findByRoleId(String roleId);
}
