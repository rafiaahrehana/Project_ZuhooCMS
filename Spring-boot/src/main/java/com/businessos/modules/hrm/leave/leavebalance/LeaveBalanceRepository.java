package com.businessos.modules.hrm.leave.leavebalance;

import com.businessos.enums.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    List<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, int year);

    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeAndYear(
        Long employeeId, LeaveType leaveType, int year);

    List<LeaveBalance> findByCompanyIdAndYear(Long companyId, int year);

    Page<LeaveBalance> findByCompanyIdAndYear(Long companyId, int year, Pageable pageable);

    Optional<LeaveBalance> findByIdAndCompanyId(Long id, Long companyId);
}
