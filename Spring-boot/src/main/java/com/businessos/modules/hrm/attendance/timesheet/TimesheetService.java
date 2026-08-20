package com.businessos.modules.hrm.attendance.timesheet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetService {
    TimesheetResponse log(TimesheetRequest request);
    TimesheetResponse getById(Long id);
    Page<TimesheetResponse> listMine(Pageable pageable);
    Page<TimesheetResponse> listForEmployee(Long employeeId, Pageable pageable);
    List<TimesheetResponse> listByDateRange(Long employeeId, LocalDate from, LocalDate to);
    TimesheetResponse update(Long id, TimesheetRequest request);
    /** EMPLOYEE (self-service): submits all of the caller's own not-yet-submitted, not-yet-approved entries for review. Returns how many were submitted. */
    int submitForReview();
    TimesheetResponse approve(Long id);
    void delete(Long id);
    /** AI micro-assist: turns rough notes into a draft task description + description. Nothing is saved. */
    TimesheetComposeResponse composeEntry(TimesheetComposeRequest request);
}
