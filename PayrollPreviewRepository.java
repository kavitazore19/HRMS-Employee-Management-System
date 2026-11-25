package com.iceipt.hrms.repository;

import com.iceipt.hrms.entity.PayrollPreview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PayrollPreviewRepository
        extends JpaRepository<PayrollPreview, Long> {

    Optional<PayrollPreview> findByConfirmationId(
            String confirmationId
    );

    List<PayrollPreview> findByAdmin_Id(Long adminId);
}