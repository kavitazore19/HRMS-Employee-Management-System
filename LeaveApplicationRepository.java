
package com.iceipt.hrms.repository;

import com.iceipt.hrms.entity.LeaveApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveApplicationRepository
        extends JpaRepository<LeaveApplication, Long> {

    List<LeaveApplication> findByEmployee_Id(Long employeeId);

    List<LeaveApplication> findByEmployee_IdAndStatus(
            Long employeeId,
            LeaveApplication.LeaveStatus status
    );

    List<LeaveApplication> findByStatus(
            LeaveApplication.LeaveStatus status
    );

    Optional<LeaveApplication> findByIdAndEmployee_Id(
            Long id,
            Long employeeId
    );
}
