
package com.iceipt.hrms.service;

import com.iceipt.hrms.dto.EmployeeRequest;
import com.iceipt.hrms.dto.EmployeeResponse;
import com.iceipt.hrms.entity.Employee;
import com.iceipt.hrms.entity.User;
import com.iceipt.hrms.entity.LeaveBalance;
import com.iceipt.hrms.entity.LeaveApplication.LeaveType;
import com.iceipt.hrms.repository.EmployeeRepository;
import com.iceipt.hrms.repository.UserRepository;
import com.iceipt.hrms.repository.LeaveBalanceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {

        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {

        if (employeeRepository.findByEmail(request.getEmail()).isPresent()
                || userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (employeeRepository.findByEmployeeCode(
                request.getEmployeeCode()).isPresent()) {
            throw new RuntimeException("Employee code already exists");
        }

        Employee employee = new Employee();

        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setFullName(request.getFullName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());
        employee.setDesignation(request.getDesignation());
        employee.setJoiningDate(request.getJoiningDate());
        employee.setSalary(request.getSalary());
        employee.setActive(true);

        Employee saved = employeeRepository.save(employee);

        User user = new User();
        user.setEmployee(saved);
        user.setEmail(saved.getEmail());
        user.setPassword(
                passwordEncoder.encode(request.getTemporaryPassword()));
        user.setRole(User.Role.EMPLOYEE);
        user.setEnabled(true);

        userRepository.save(user);

        createLeaveBalance(saved, LeaveType.SICK, 10);
        createLeaveBalance(saved, LeaveType.CASUAL, 12);
        createLeaveBalance(saved, LeaveType.EARNED, 15);

        auditLogService.logAction(
                "CREATE_EMPLOYEE",
                "EMPLOYEE",
                saved.getId(),
                "Created employee: " + saved.getFullName()
        );

        return toResponse(saved);
    }

    // Create login for an employee who already exists
    @Transactional
    public void createLoginForExistingEmployee(
            Long employeeId,
            String temporaryPassword) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found"));

        if (userRepository.existsByEmail(employee.getEmail())) {
            throw new RuntimeException("Login account already exists");
        }

        User user = new User();
        user.setEmployee(employee);
        user.setEmail(employee.getEmail());
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setRole(User.Role.EMPLOYEE);
        user.setEnabled(true);

        userRepository.save(user);

        auditLogService.logAction(
                "CREATE_EMPLOYEE_LOGIN",
                "EMPLOYEE",
                employee.getId(),
                "Created login for employee: " + employee.getFullName()
        );
    }

    private void createLeaveBalance(
            Employee employee,
            LeaveType leaveType,
            int totalDays) {

        LeaveBalance balance = new LeaveBalance();

        balance.setEmployee(employee);
        balance.setLeaveType(leaveType);
        balance.setTotalDays(totalDays);
        balance.setUsedDays(0);
        balance.setRemainingDays(totalDays);

        leaveBalanceRepository.save(balance);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {

        return employeeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found"));

        return toResponse(employee);
    }

    private EmployeeResponse toResponse(Employee employee) {

        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getDesignation(),
                employee.getJoiningDate(),
                employee.getSalary(),
                employee.getActive()
        );
    }
}
