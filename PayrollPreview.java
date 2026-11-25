
package com.iceipt.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "payroll_previews",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "confirmation_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class PayrollPreview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "confirmation_id", nullable = false, unique = true)
    private String confirmationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(name = "payroll_month", nullable = false)
    private LocalDate payrollMonth;

    @Column(name = "employee_ids", columnDefinition = "JSON")
    private String employeeIds;

    @Column(name = "preview_data", columnDefinition = "JSON")
    private String previewData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreviewStatus status = PreviewStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum PreviewStatus {
        PENDING,
        CONFIRMED,
        EXPIRED
    }
}
