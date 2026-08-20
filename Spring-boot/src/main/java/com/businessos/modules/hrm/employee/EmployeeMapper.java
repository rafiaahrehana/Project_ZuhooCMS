package com.businessos.modules.hrm.employee;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.attendance.shift.Shift;
import com.businessos.modules.hrm.department.Department;

import com.businessos.modules.hrm.designation.Designation;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    private final com.businessos.shared.address.AddressMapper addressMapper;

    public EmployeeMapper(com.businessos.shared.address.AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public EmployeeResponse toDTO(Employee e) {
        User u = e.getUser();
        Department d = e.getDepartment();
        Designation designation = e.getDesignation();
        Employee mgr = e.getReportingManager();
        Shift shift = e.getShift();
        User mgrU = mgr != null ? mgr.getUser() : null;
        EmployeeResponse r = new EmployeeResponse();
        r.setId(e.getId());
        r.setUserId(u != null ? u.getId() : null);
        r.setFirstName(u != null ? u.getFirstName() : null);
        r.setLastName(u != null ? u.getLastName() : null);
        r.setEmail(u != null ? u.getEmail() : null);
        r.setPhone(u != null ? u.getPhone() : null);
        r.setImage(u != null ? u.getImage() : null);
        r.setEmployeeNumber(e.getEmployeeNumber());
        r.setOfficialEmail(e.getOfficialEmail());
        r.setWorkPhone(e.getWorkPhone());
        r.setProfileImageUrl(e.getProfileImageUrl());
        r.setNationalId(e.getNationalId());
        r.setTaxId(e.getTaxId());
        r.setCostCenter(e.getCostCenter());
        r.setOfficeLocation(e.getOfficeLocation());
        r.setJobTitle(e.getJobTitle());
        r.setEmploymentType(e.getEmploymentType());
        r.setEmploymentStatus(e.getEmploymentStatus());
        r.setGender(e.getGender());
        r.setDateOfBirth(e.getDateOfBirth());
        r.setFatherName(e.getFatherName());
        r.setMotherName(e.getMotherName());
        r.setLocation(addressMapper.toResponse(e.getLocation()));
        r.setHireDate(e.getHireDate());
        r.setConfirmationDate(e.getConfirmationDate());
        r.setProbationEndDate(e.getProbationEndDate());
        r.setContractEndDate(e.getContractEndDate());
        r.setDepartmentId(d != null ? d.getId() : null);
        r.setDepartmentName(d != null ? d.getName() : null);
        r.setDesignationId(designation != null ? designation.getId() : null);
        r.setDesignationName(designation != null ? designation.getName() : null);
        r.setReportingManagerId(mgr != null ? mgr.getId() : null);
        r.setReportingManagerName(mgrU != null ? mgrU.getFullName() : null);
        r.setShiftId(shift != null ? shift.getId() : null);
        r.setShiftName(shift != null ? shift.getName() : null);
        r.setBasicSalary(e.getBasicSalary());
        r.setHouseRent(e.getHouseRent());
        r.setMedicalAllowance(e.getMedicalAllowance());
        r.setTransportAllowance(e.getTransportAllowance());
        r.setBillableRate(e.getBillableRate());
        r.setBankName(e.getBankName());
        // Needed by the payroll disbursement screen so HR can see, before running
        // the bank file, which employees still have incomplete bank details.
        r.setBankAccountNumber(e.getBankAccountNumber());
        r.setBankRoutingNumber(e.getBankRoutingNumber());
        r.setEmergencyContactName(e.getEmergencyContactName());
        r.setEmergencyContactPhone(e.getEmergencyContactPhone());
        r.setEmergencyContactRelation(e.getEmergencyContactRelation());
        r.setActive(e.isActive());
        r.setCreatedAt(e.getCreatedAt());
        com.businessos.auth.role.entity.CustomRole customRole = u != null ? u.getCustomRole() : null;
        r.setCustomRoleId(customRole != null ? customRole.getId() : null);
        r.setCustomRoleName(customRole != null ? customRole.getName() : null);
        return r;
    }
}
