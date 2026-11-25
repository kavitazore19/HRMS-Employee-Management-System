
package com.iceipt.hrms.service;

import com.iceipt.hrms.dto.PayrollPreviewResponse;
import com.iceipt.hrms.dto.PayrollSummaryResponse;
import com.iceipt.hrms.entity.Attendance;
import com.iceipt.hrms.entity.Employee;
import com.iceipt.hrms.entity.Payroll;
import com.iceipt.hrms.entity.PayrollPreview;
import com.iceipt.hrms.entity.User;
import com.iceipt.hrms.repository.AttendanceRepository;
import com.iceipt.hrms.repository.EmployeeRepository;
import com.iceipt.hrms.repository.PayrollPreviewRepository;
import com.iceipt.hrms.repository.PayrollRepository;
import com.iceipt.hrms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final PayrollRepository payrollRepository;
    private final PayrollPreviewRepository payrollPreviewRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    private static final int PREVIEW_EXPIRY_MINUTES = 15;

    private User getLoggedInAdmin() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new RuntimeException("Please login first.");
        }

        User user = userRepository.findByEmail(
                authentication.getName()
        ).orElseThrow(() ->
                new RuntimeException("User not found.")
        );

        if (user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Admin access required.");
        }

        return user;
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not save payroll preview data.", e
            );
        }
    }

    private PayrollPreviewResponse.EmployeePayrollPreview[]
    readPreviewData(String json) {

        try {
            return objectMapper.readValue(
                    json,
                    PayrollPreviewResponse.EmployeePayrollPreview[].class
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not read payroll preview data.", e
            );
        }
    }

    @Transactional
    public PayrollPreviewResponse preparePayrollPreview(
            LocalDate requestedMonth) {

        User admin = getLoggedInAdmin();

        if (requestedMonth == null) {
            throw new RuntimeException("Payroll month is required.");
        }

        YearMonth yearMonth = YearMonth.from(requestedMonth);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Employee> employees =
                employeeRepository.findByActiveTrue();

        if (employees.isEmpty()) {
            throw new RuntimeException("No active employees found.");
        }

        List<PayrollPreviewResponse.EmployeePayrollPreview> previews =
                new ArrayList<>();

        List<Long> employeeIds = new ArrayList<>();

        for (Employee employee : employees) {

            if (payrollRepository.existsByEmployee_IdAndPayrollMonth(
                    employee.getId(), startDate)) {

                throw new RuntimeException(
                        "Payroll already processed for employee: "
                                + employee.getFullName()
                );
            }

            List<Attendance> attendanceRecords =
                    attendanceRepository
                            .findByEmployee_IdAndAttendanceDateBetween(
                                    employee.getId(),
                                    startDate,
                                    endDate
                            );

            int presentDays = 0;
            int absentDays = 0;

            for (Attendance attendance : attendanceRecords) {

                if (attendance.getStatus()
                        == Attendance.AttendanceStatus.PRESENT ||
                    attendance.getStatus()
                        == Attendance.AttendanceStatus.LATE) {

                    presentDays++;

                } else if (attendance.getStatus()
                        == Attendance.AttendanceStatus.ABSENT) {

                    absentDays++;
                }
            }

            BigDecimal basicSalary = employee.getSalary();
            BigDecimal allowances = BigDecimal.ZERO;
            BigDecimal deductions = BigDecimal.ZERO;

            BigDecimal payableSalary = basicSalary
                    .add(allowances)
                    .subtract(deductions);

            PayrollPreviewResponse.EmployeePayrollPreview preview =
                    new PayrollPreviewResponse.EmployeePayrollPreview(
                            employee.getId(),
                            employee.getFullName(),
                            basicSalary,
                            allowances,
                            deductions,
                            payableSalary,
                            presentDays,
                            absentDays
                    );

            previews.add(preview);
            employeeIds.add(employee.getId());
        }

        String confirmationId = UUID.randomUUID().toString();

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusMinutes(PREVIEW_EXPIRY_MINUTES);

        PayrollPreview payrollPreview = new PayrollPreview();

        payrollPreview.setConfirmationId(confirmationId);
        payrollPreview.setAdmin(admin);
        payrollPreview.setPayrollMonth(startDate);
        payrollPreview.setEmployeeIds(toJson(employeeIds));
        payrollPreview.setPreviewData(toJson(previews));
        payrollPreview.setStatus(
                PayrollPreview.PreviewStatus.PENDING
        );
        payrollPreview.setExpiresAt(expiresAt);

        payrollPreviewRepository.save(payrollPreview);

        auditLogService.logAction(
                "PAYROLL_PREVIEW",
                "PAYROLL",
                null,
                "Payroll preview generated for "
                        + startDate + " for "
                        + employees.size() + " employees"
        );

        return new PayrollPreviewResponse(
                confirmationId,
                startDate,
                expiresAt,
                previews
        );
    }

    @Transactional
    public List<PayrollSummaryResponse> confirmPayroll(
            String confirmationId) {

        User admin = getLoggedInAdmin();

        PayrollPreview preview =
                payrollPreviewRepository
                        .findByConfirmationId(confirmationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payroll preview not found."
                                )
                        );

        if (!preview.getAdmin().getId().equals(admin.getId())) {
            throw new RuntimeException(
                    "You cannot confirm another admin's preview."
            );
        }

        if (preview.getStatus()
                != PayrollPreview.PreviewStatus.PENDING) {

            throw new RuntimeException(
                    "This payroll preview has already been processed."
            );
        }

        if (LocalDateTime.now().isAfter(preview.getExpiresAt())) {

            throw new RuntimeException(
                    "Payroll preview expired. Generate a new preview."
            );
        }

        PayrollPreviewResponse.EmployeePayrollPreview[] employees =
                readPreviewData(preview.getPreviewData());

        List<PayrollSummaryResponse> results = new ArrayList<>();

        for (PayrollPreviewResponse.EmployeePayrollPreview item
                : employees) {

            if (payrollRepository.existsByEmployee_IdAndPayrollMonth(
                    item.getEmployeeId(),
                    preview.getPayrollMonth())) {

                throw new RuntimeException(
                        "Payroll already exists for employee ID: "
                                + item.getEmployeeId()
                );
            }

            Employee employee = employeeRepository.findById(
                    item.getEmployeeId()
            ).orElseThrow(() ->
                    new RuntimeException("Employee not found.")
            );

            Payroll payroll = new Payroll();

            payroll.setEmployee(employee);
            payroll.setPayrollMonth(preview.getPayrollMonth());
            payroll.setBasicSalary(item.getBasicSalary());
            payroll.setAllowances(item.getAllowances());
            payroll.setDeductions(item.getDeductions());
            payroll.setPayableSalary(item.getPayableSalary());
            payroll.setPresentDays(item.getPresentDays());
            payroll.setAbsentDays(item.getAbsentDays());
            payroll.setProcessedBy(admin);
            payroll.setProcessedAt(LocalDateTime.now());

            Payroll savedPayroll = payrollRepository.save(payroll);

            results.add(toSummary(savedPayroll));
        }

        preview.setStatus(PayrollPreview.PreviewStatus.CONFIRMED);
        preview.setConfirmedAt(LocalDateTime.now());

        payrollPreviewRepository.save(preview);

        auditLogService.logAction(
                "PAYROLL_CONFIRMED",
                "PAYROLL",
                null,
                "Payroll confirmed for "
                        + preview.getPayrollMonth()
                        + " for " + results.size() + " employees"
        );

        return results;
    }

    @Transactional(readOnly = true)
    public List<PayrollSummaryResponse> getPayrollSummary(
            LocalDate requestedMonth) {

        getLoggedInAdmin();

        if (requestedMonth == null) {
            throw new RuntimeException("Payroll month is required.");
        }

        LocalDate month = YearMonth.from(requestedMonth).atDay(1);

        return payrollRepository.findByPayrollMonth(month)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PayrollSummaryResponse> getMyPayrollSummary() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new RuntimeException("Please login first.");
        }

        User user = userRepository.findByEmail(
                authentication.getName()
        ).orElseThrow(() ->
                new RuntimeException("User not found.")
        );

        if (user.getEmployee() == null) {
            throw new RuntimeException(
                    "No employee profile is linked to this account."
            );
        }

        return payrollRepository.findByEmployee_Id(
                user.getEmployee().getId()
        ).stream()
                .map(this::toSummary)
                .toList();
    }

    private PayrollSummaryResponse toSummary(Payroll payroll) {

        return new PayrollSummaryResponse(
                payroll.getId(),
                payroll.getEmployee().getId(),
                payroll.getEmployee().getFullName(),
                payroll.getPayrollMonth(),
                payroll.getBasicSalary(),
                payroll.getAllowances(),
                payroll.getDeductions(),
                payroll.getPayableSalary(),
                payroll.getPresentDays() == null
                        ? 0 : payroll.getPresentDays(),
                payroll.getAbsentDays() == null
                        ? 0 : payroll.getAbsentDays(),
                payroll.getStatus().name()
        );
    }
}
