package com.iceipt.hrms.service;

import com.iceipt.hrms.dto.AuditLogResponse;
import com.iceipt.hrms.entity.AuditLog;
import com.iceipt.hrms.entity.User;
import com.iceipt.hrms.repository.AuditLogRepository;
import com.iceipt.hrms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void logAction(
            String action,
            String entityType,
            Long entityId,
            String details
    ) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User user = null;

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !authentication.getName().equals("anonymousUser")) {

            user = userRepository
                    .findByEmail(authentication.getName())
                    .orElse(null);
        }

        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);

        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllLogs() {

        return auditLogRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AuditLogResponse mapToResponse(AuditLog log) {

        return new AuditLogResponse(
                log.getId(),
                log.getUser() != null
                        ? log.getUser().getId() : null,
                log.getUser() != null
                        ? log.getUser().getEmail() : null,
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}