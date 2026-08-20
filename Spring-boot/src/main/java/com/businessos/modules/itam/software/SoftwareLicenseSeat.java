package com.businessos.modules.itam.software;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.employee.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDate;

/**
 * Tracks which employee currently holds a seat on a software license - the
 * license itself only stores an aggregate seat count, so without this there is
 * no way to answer "which licenses does this employee have?" (needed for
 * offboarding, and for showing seat holders on the license itself).
 * releasedAt == null means the seat is currently held.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "software_license_seats")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SoftwareLicenseSeat extends BaseEntity {

    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "license_id", nullable = false)
    private SoftwareLicense license;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private LocalDate assignedAt;
    private LocalDate releasedAt;
}
