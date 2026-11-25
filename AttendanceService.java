
package com.iceipt.hrms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iceipt.hrms.dto.AttendanceResponse;
import com.iceipt.hrms.entity.Attendance;
import com.iceipt.hrms.entity.Employee;
import com.iceipt.hrms.entity.User;
import com.iceipt.hrms.repository.AttendanceRepository;
import com.iceipt.hrms.repository.EmployeeRepository;
import com.iceipt.hrms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    private User getLoggedInUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validateEmployeeAccess(Long employeeId) {

        User user = getLoggedInUser();

        if (user.getRole() == User.Role.ADMIN) {
            return;
        }

        if (user.getRole() == User.Role.EMPLOYEE) {

            if (user.getEmployee() == null) {
                throw new RuntimeException("Employee account not linked");
            }

            if (!user.getEmployee().getId().equals(employeeId)) {
                throw new RuntimeException(
                        "You can access only your own attendance"
                );
            }

            return;
        }

        throw new RuntimeException("Access denied");
    }

    @Transactional
    public AttendanceResponse punchIn(Long employeeId) {

        validateEmployeeAccess(employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found"));

        LocalDate today = LocalDate.now();

        if (attendanceRepository
                .findByEmployee_IdAndAttendanceDate(employeeId, today)
                .isPresent()) {
            throw new RuntimeException("Already punched in today");
        }

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(today);
        attendance.setPunchIn(LocalDateTime.now());
        attendance.setStatus(Attendance.AttendanceStatus.PRESENT);

        Attendance saved = attendanceRepository.save(attendance);

        auditLogService.logAction(
                "PUNCH_IN",
                "ATTENDANCE",
                saved.getId(),
                "Employee punched in: " + employee.getFullName()
        );

        return toResponse(saved);
    }

    @Transactional
    public AttendanceResponse punchOut(Long employeeId) {

        validateEmployeeAccess(employeeId);

        Attendance attendance = attendanceRepository
                .findByEmployee_IdAndAttendanceDate(
                        employeeId, LocalDate.now()
                )
                .orElseThrow(() ->
                        new RuntimeException("Please punch in first"));

        if (attendance.getPunchOut() != null) {
            throw new RuntimeException("Already punched out today");
        }

        attendance.setPunchOut(LocalDateTime.now());

        Attendance saved = attendanceRepository.save(attendance);

        auditLogService.logAction(
                "PUNCH_OUT",
                "ATTENDANCE",
                saved.getId(),
                "Employee punched out: "
                        + attendance.getEmployee().getFullName()
        );

        return toResponse(saved);
    }

    public AttendanceResponse getTodayAttendance(Long employeeId) {

        validateEmployeeAccess(employeeId);

        Attendance attendance = attendanceRepository
                .findByEmployee_IdAndAttendanceDate(
                        employeeId, LocalDate.now()
                )
                .orElseThrow(() ->
                        new RuntimeException("No attendance found today"));

        return toResponse(attendance);
    }

    public List<AttendanceResponse> getMonthlyAttendance(
            Long employeeId, int year, int month) {

        validateEmployeeAccess(employeeId);

        YearMonth yearMonth = YearMonth.of(year, month);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return attendanceRepository
                .findByEmployee_IdAndAttendanceDateBetween(
                        employeeId, startDate, endDate
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AttendanceResponse toResponse(Attendance attendance) {

        return new AttendanceResponse(
                attendance.getId(),
                attendance.getEmployee().getId(),
                attendance.getAttendanceDate(),
                attendance.getPunchIn(),
                attendance.getPunchOut(),
                attendance.getStatus().name()
        );
    }
}
