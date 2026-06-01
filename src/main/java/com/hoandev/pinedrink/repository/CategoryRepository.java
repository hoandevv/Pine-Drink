package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    List<Category> findByStatusOrderByDisplayOrder(String status);
}
