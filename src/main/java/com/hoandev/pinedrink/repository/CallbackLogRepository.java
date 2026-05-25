package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.CallbackLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CallbackLogRepository extends JpaRepository<CallbackLog, String> {
    Optional<CallbackLog> findByProviderAndRequestId(String provider, String requestId);
}
