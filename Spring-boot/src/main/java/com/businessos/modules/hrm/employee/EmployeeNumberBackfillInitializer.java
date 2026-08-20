package com.businessos.modules.hrm.employee;

import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-time startup fix for employees created before employeeNumber became
 * server-generated (EmployeeNumberGenerator) - assigns each a sequential
 * EMP-NNNN number, per company, in id order. New employees already get one
 * at creation time so this only ever finds pre-existing gaps.
 */
@Component
@RequiredArgsConstructor
public class EmployeeNumberBackfillInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public void run(String... args) {
        for (Company company : companyRepository.findAll()) {
            List<Employee> missing = employeeRepository.findByCompanyIdWithBlankEmployeeNumber(company.getId());
            if (missing.isEmpty()) continue;

            for (Employee employee : missing) {
                employee.setEmployeeNumber(EmployeeNumberGenerator.next(employeeRepository, company.getId()));
                // saveAndFlush (not save) so the next iteration's MAX lookup sees this number.
                employeeRepository.saveAndFlush(employee);
            }
        }
    }
}
