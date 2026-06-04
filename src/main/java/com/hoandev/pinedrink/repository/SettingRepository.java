package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettingRepository extends JpaRepository<Setting, String> {
    Optional<Setting> findByConfigKey(String configKey);

    Optional<Setting> findByBranchIdAndConfigKey(String branchId, String configKey);
}
