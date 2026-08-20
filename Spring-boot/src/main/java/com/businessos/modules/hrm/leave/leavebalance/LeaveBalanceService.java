package com.businessos.modules.hrm.leave.leavebalance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LeaveBalanceService {

    // ADMIN/OWNER: create a leave balance entry for an employee
    LeaveBalanceResponse create(LeaveBalanceRequest request);

    // ADMIN/OWNER: update an existing leave balance entry
    LeaveBalanceResponse update(Long id, LeaveBalanceRequest request);

    // ADMIN/OWNER: delete a leave balance entry
    void delete(Long id);

    // ADMIN/OWNER: list all leave balances for the company for a given year
    Page<LeaveBalanceResponse> listAll(int year, Pageable pageable);

    /**
     * The caller's OWN balances for a year.
     *
     * Needed because listAll() returns every employee's balances - fine for an
     * owner, but an employee viewing their dashboard must not be handed the
     * whole company's leave data just to render their own three bars.
     */
    java.util.List<LeaveBalanceResponse> listMine(int year);
}
