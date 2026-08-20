package com.businessos.modules.hrm.asset;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "asset_assignments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private LocalDate assignmentDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;

    private String conditionWhenAssigned;
    private String conditionOnReturn;

    private String assignedBy;
    private String notes;

    @Builder.Default
    private boolean active = true;

    public boolean isActive() {
        return active && actualReturnDate == null;
    }
}
