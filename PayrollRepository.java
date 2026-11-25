
package com.iceipt.hrms.repository;

import com.iceipt.hrms.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayrollRepository
        extends JpaRepository<Payroll, Long> {

    List<Payroll> findByPayrollMonth(LocalDate payrollMonth);

    List<Payroll> findByEmployee_Id(Long employeeId);

    Optional<Payroll> findByEmployee_IdAndPayrollMonth(
            Long employeeId,
            LocalDate payrollMonth
    );

    boolean existsByEmployee_IdAndPayrollMonth(
            Long employeeId,
            LocalDate payrollMonth
    );
}
