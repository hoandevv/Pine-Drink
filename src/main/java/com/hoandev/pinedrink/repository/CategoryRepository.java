package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    boolean existsByCode(String code);

    List<Category> findByStatusOrderByDisplayOrder(String status);

    Page<Category> findByStatus(String status, Pageable pageable);
}
