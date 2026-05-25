package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, String> {
    Optional<Template> findByBrandIdAndTemplateCodeAndChannel(String brandId, String templateCode, String channel);
}
