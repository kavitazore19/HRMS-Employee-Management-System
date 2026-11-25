
package com.iceipt.hrms.repository;

import com.iceipt.hrms.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
