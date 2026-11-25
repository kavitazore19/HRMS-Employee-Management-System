
package com.iceipt.hrms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iceipt.hrms.dto.LeaveRequest;
import com.iceipt.hrms.dto.LeaveResponse;
import com.iceipt.hrms.dto.LeaveBalanceResponse;
import com.iceipt.hrms.entity.Employee;
import com.iceipt.hrms.entity.User;
import com.iceipt.hrms.entity.LeaveApplication;
import com.iceipt.hrms.entity.LeaveBalance;
import com.iceipt.hrms.repository.UserRepository;
import com.iceipt.hrms.repository.LeaveApplicationRepository;
import com.iceipt.hrms.repository.LeaveBalanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final UserRepository userRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final AuditLogService auditLogService;

    private User getLoggedInUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public LeaveResponse applyLeave(LeaveRequest request) {

        User user = getLoggedInUser();
        Employee employee = user.getEmployee();

        if (employee == null) {
            throw new RuntimeException("Employee account not linked");
        }

        if (request.getStartDate() == null ||
            request.getEndDate() == null ||
            request.getLeaveType() == null ||
            request.getReason() == null ||
            request.getReason().isBlank()) {
            throw new RuntimeException("All leave details are required");
        }

        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Past dates are not allowed");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("Invalid leave dates");
        }

        int totalDays = (int) ChronoUnit.DAYS.between(
                request.getStartDate(),
                request.getEndDate()
        ) + 1;

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployee_IdAndLeaveType(
                        employee.getId(), request.getLeaveType()
                )
                .orElseThrow(() ->
                        new RuntimeException("Leave balance not configured"));

        if (balance.getRemainingDays() < totalDays) {
            throw new RuntimeException("Insufficient leave balance");
        }

        LeaveApplication application = new LeaveApplication();
        application.setEmployee(employee);
        application.setLeaveType(request.getLeaveType());
        application.setStartDate(request.getStartDate());
        application.setEndDate(request.getEndDate());
        application.setTotalDays(totalDays);
        application.setReason(request.getReason());
        application.setStatus(LeaveApplication.LeaveStatus.PENDING);

        LeaveApplication saved =
                leaveApplicationRepository.save(application);

        auditLogService.logAction(
                "APPLY_LEAVE",
                "LEAVE",
                saved.getId(),
                "Leave applied by " + employee.getFullName()
                        + " for " + totalDays + " days"
        );

        return toLeaveResponse(saved);
    }

    public List<LeaveResponse> getMyLeaves() {

        User user = getLoggedInUser();

        if (user.getEmployee() == null) {
            throw new RuntimeException("Employee account not linked");
        }

        return leaveApplicationRepository
                .findByEmployee_Id(user.getEmployee().getId())
                .stream()
                .map(this::toLeaveResponse)
                .toList();
    }

    public List<LeaveBalanceResponse> getMyLeaveBalances() {

        User user = getLoggedInUser();

        if (user.getEmployee() == null) {
            throw new RuntimeException("Employee account not linked");
        }

        return leaveBalanceRepository
                .findByEmployee_Id(user.getEmployee().getId())
                .stream()
                .map(balance -> new LeaveBalanceResponse(
                        balance.getId(),
                        balance.getEmployee().getId(),
                        balance.getLeaveType(),
                        balance.getTotalDays(),
                        balance.getUsedDays(),
                        balance.getRemainingDays()
                ))
                .toList();
    }

    public List<LeaveResponse> getPendingLeaves() {

        return leaveApplicationRepository
                .findByStatus(LeaveApplication.LeaveStatus.PENDING)
                .stream()
                .map(this::toLeaveResponse)
                .toList();
    }

    @Transactional
    public LeaveResponse reviewLeave(Long leaveId, boolean approve) {

        User admin = getLoggedInUser();

        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Admin access required");
        }

        LeaveApplication application = leaveApplicationRepository
                .findById(leaveId)
                .orElseThrow(() ->
                        new RuntimeException("Leave application not found"));

        if (application.getStatus() !=
                LeaveApplication.LeaveStatus.PENDING) {
            throw new RuntimeException("Leave already reviewed");
        }

        if (approve) {

            LeaveBalance balance = leaveBalanceRepository
                    .findByEmployee_IdAndLeaveType(
                            application.getEmployee().getId(),
                            application.getLeaveType()
                    )
                    .orElseThrow(() ->
                            new RuntimeException("Leave balance not found"));

            if (balance.getRemainingDays() < application.getTotalDays()) {
                throw new RuntimeException("Insufficient leave balance");
            }

            balance.setUsedDays(
                    balance.getUsedDays() + application.getTotalDays()
            );

            balance.setRemainingDays(
                    balance.getRemainingDays() - application.getTotalDays()
            );

            leaveBalanceRepository.save(balance);

            application.setStatus(
                    LeaveApplication.LeaveStatus.APPROVED
            );

        } else {

            application.setStatus(
                    LeaveApplication.LeaveStatus.REJECTED
            );
        }

        application.setReviewedBy(admin);
        application.setReviewedAt(LocalDateTime.now());

        LeaveApplication saved =
                leaveApplicationRepository.save(application);

        auditLogService.logAction(
                approve ? "APPROVE_LEAVE" : "REJECT_LEAVE",
                "LEAVE",
                saved.getId(),
                (approve ? "Approved" : "Rejected")
                        + " leave for "
                        + saved.getEmployee().getFullName()
        );

        return toLeaveResponse(saved);
    }

    private LeaveResponse toLeaveResponse(LeaveApplication application) {

        return new LeaveResponse(
                application.getId(),
                application.getEmployee().getId(),
                application.getLeaveType(),
                application.getStartDate(),
                application.getEndDate(),
                application.getTotalDays(),
                application.getReason(),
                application.getStatus(),
                application.getCreatedAt()
        );
    }
}
