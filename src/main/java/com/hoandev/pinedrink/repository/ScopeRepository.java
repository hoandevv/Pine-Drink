package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Scope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScopeRepository extends JpaRepository<Scope, String> {
    Optional<Scope> findByScopeTypeAndBranchId(String scopeType, String branchId);

    Optional<Scope> findByScopeTypeAndBranchIdIsNull(String scopeType);
}
