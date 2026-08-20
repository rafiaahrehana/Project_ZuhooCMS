package com.businessos.modules.hrm.leave.companyleavePolicy;

import com.businessos.modules.company.CompanyLeavePolicy;
import com.businessos.enums.EmploymentType;
import com.businessos.enums.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyLeavePolicyRepository extends JpaRepository<CompanyLeavePolicy, Long> {

    Optional<CompanyLeavePolicy> findByIdAndCompanyId(Long id, Long companyId);

    List<CompanyLeavePolicy> findByCompanyIdAndActiveTrue(Long companyId);

    Page<CompanyLeavePolicy> findByCompanyId(Long companyId, Pageable pageable);

    @Query("""
        SELECT p FROM CompanyLeavePolicy p
        WHERE p.company.id = :companyId
          AND p.leaveType = :leaveType
          AND p.active = true
          AND (p.employmentType IS NULL OR p.employmentType = :empType)
          AND p.deleted = false
        ORDER BY p.employmentType NULLS LAST
        """)
    Optional<CompanyLeavePolicy> findApplicablePolicy(
        @Param("companyId") Long companyId,
        @Param("leaveType") LeaveType leaveType,
        @Param("empType") EmploymentType empType);
}
