
package com.iceipt.hrms.repository;

import com.iceipt.hrms.entity.LeaveBalance;
import com.iceipt.hrms.entity.LeaveApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository
        extends JpaRepository<LeaveBalance, Long> {

    List<LeaveBalance> findByEmployee_Id(Long employeeId);

    Optional<LeaveBalance> findByEmployee_IdAndLeaveType(
            Long employeeId,
            LeaveApplication.LeaveType leaveType
    );
}
