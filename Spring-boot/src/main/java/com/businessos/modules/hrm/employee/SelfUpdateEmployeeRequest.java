package com.businessos.modules.hrm.employee;

import com.businessos.enums.Gender;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Fields an employee may edit on their OWN Employee record via PATCH /api/employees/me -
 * deliberately a narrow subset of UpdateEmployeeRequest. Job title, department, designation,
 * employment type/status, salary, bank details, hire dates, and date of birth are HR/owner-
 * managed only (EMPLOYEE_UPDATE via PATCH /api/employees/{id}) - letting an employee self-edit
 * those would let them change their own pay or job standing. National ID and Tax ID are
 * self-reported personal identity fields (the employee is the authority on their own ID
 * numbers), so those stay self-editable alongside the rest of the personal-info fields.
 */
@Data
public class SelfUpdateEmployeeRequest {
    @Size(max = 30)
    private String workPhone;
    // Personal mobile number - lives on the linked User account (User.phone), distinct
    // from workPhone which lives on the Employee record.
    @Size(max = 30)
    private String phone;
    @Size(max = 500)
    private String profileImageUrl;
    private Gender gender;
    @Size(max = 100)
    private String fatherName;
    @Size(max = 100)
    private String motherName;
    @Size(max = 50)
    private String nationalId;
    @Size(max = 50)
    private String taxId;
    private com.businessos.shared.address.AddressRequest location;
    @Size(max = 100)
    private String emergencyContactName;
    @Size(max = 30)
    private String emergencyContactPhone;
    @Size(max = 50)
    private String emergencyContactRelation;
}
