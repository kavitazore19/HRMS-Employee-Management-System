
package com.iceipt.hrms.repository;

import com.iceipt.hrms.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployee_IdAndAttendanceDate(
            Long employeeId, LocalDate attendanceDate);

    List<Attendance> findByEmployee_IdAndAttendanceDateBetween(
            Long employeeId, LocalDate startDate, LocalDate endDate);

    List<Attendance> findByAttendanceDate(LocalDate attendanceDate);
}
