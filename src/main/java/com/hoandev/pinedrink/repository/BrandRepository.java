package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, String> {
    Optional<Brand> findByCode(String code);
    List<Brand> findByStatus(String status);
}
