package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Scope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScopeRepository extends JpaRepository<Scope, String> {
    Optional<Scope> findByScopeTypeAndBrandIdAndBranchId(String scopeType, String brandId, String branchId);

    Optional<Scope> findByScopeTypeAndBrandId(String scopeType, String brandId);

    Optional<Scope> findByScopeTypeAndBranchId(String scopeType, String branchId);
}
