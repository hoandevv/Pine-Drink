package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.CallbackLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CallbackLogRepository extends JpaRepository<CallbackLog, String> {

    @Query("""
            select callbackLog
            from CallbackLog callbackLog
            where callbackLog.provider = :provider
              and callbackLog.requestId = :requestId
            """)
    Optional<CallbackLog> findByProviderRequest(
            @Param("provider") String provider,
            @Param("requestId") String requestId
    );
}
