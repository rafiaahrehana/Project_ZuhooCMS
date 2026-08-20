package com.businessos.modules.hrm.leave;

import com.businessos.modules.hrm.leave.leavebalance.LeaveBalanceResponse;
import com.businessos.modules.hrm.leave.leaverequest.LeaveRequestDto;
import com.businessos.modules.hrm.leave.leaverequest.LeaveRequestResponse;
import com.businessos.enums.LeaveRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LeaveService {

    /** EMPLOYEE: apply for leave — validates balance and overlaps */
    LeaveRequestResponse apply(LeaveRequestDto request);

    /** ALL: get leave requeststatus by id */
    LeaveRequestResponse getById(Long id);

    /** ADMIN / OWNER: list all leave requests with optional requeststatus filter */
    Page<LeaveRequestResponse> listAll(LeaveRequestStatus status, Pageable pageable);

    /** EMPLOYEE: list own leave requests */
    Page<LeaveRequestResponse> listMyLeaves(Pageable pageable);

    /** ADMIN / OWNER: approve or reject a pending leave requeststatus */
    LeaveRequestResponse review(Long id, ReviewLeaveRequest request);

    /** EMPLOYEE: cancel a leave requeststatus that has not yet started */
    void cancel(Long id);

    /** EMPLOYEE: get own leave balances for a given year */
    List<LeaveBalanceResponse> getMyBalances(int year);

    /** ADMIN / OWNER: get leave balances for a specific employee */
    List<LeaveBalanceResponse> getBalancesForEmployee(Long employeeId, int year);

    /**
     * Chargeable APPROVED unpaid-leave days for one employee inside one
     * month, clipped to the month and skipping weekly off days and holidays
     * - what payroll deducts as leave-without-pay.
     */
    int unpaidLeaveDays(Long employeeId, int month, int year);

}
