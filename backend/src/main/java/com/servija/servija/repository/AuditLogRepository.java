package com.servija.servija.repository;

import com.servija.servija.model.AuditLog;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from AuditLog log
            where log.timestamp < :cutoff
              and (log.action is null or log.action not in :nonInfoActions)
            """)
    int deleteInfoLogsOlderThan(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("nonInfoActions") Collection<String> nonInfoActions);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from AuditLog log
            where log.timestamp < :cutoff
              and log.action in :actions
            """)
    int deleteLogsOlderThanAndActionIn(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("actions") Collection<String> actions);
}
