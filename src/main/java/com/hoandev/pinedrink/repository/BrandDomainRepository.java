package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.BrandDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandDomainRepository extends JpaRepository<BrandDomain, String> {
    Optional<BrandDomain> findByDomain(String domain);
    Optional<BrandDomain> findByPublicKey(String publicKey);
}
