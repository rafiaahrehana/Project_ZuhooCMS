package com.businessos.modules.hrm.employee;

import com.businessos.auth.user.User;
import com.businessos.enums.EmploymentStatus;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * One-time startup fix for companies registered before the owner's Employee
 * record was created automatically at registration (see AuthServiceImpl.register).
 * Every module that resolves "the current user's employee profile" (leads,
 * leaves, timesheets, expenses, payroll...) fails with "Employee profile not
 * found" for owners created before that fix - this backfills them.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class OwnerEmployeeBackfillInitializer implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        for (Company company : companyRepository.findAll()) {
            User owner = company.getOwner();
            if (owner == null || employeeRepository.findByUserId(owner.getId()).isPresent()) {
                continue;
            }

            Employee ownerEmployee = Employee.builder()
                .user(owner)
                .company(company)
                .employeeNumber(EmployeeNumberGenerator.next(employeeRepository, company.getId()))
                .jobTitle("Owner")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .hireDate(LocalDate.now())
                .active(true)
                .build();
            // saveAndFlush so the next iteration's MAX-based number lookup sees this row.
            employeeRepository.saveAndFlush(ownerEmployee);
        }
    }
}
