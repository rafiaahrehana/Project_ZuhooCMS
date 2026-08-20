package com.businessos.modules.hrm.designation;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.department.Department;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "designations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Designation extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 20)
    private String code;

    /** Numeric level used for hierarchy sorting (lower = more senior). */
    private Integer level;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default


    private boolean active = true;

    /** Free-form: FULL_TIME, PART_TIME, CONTRACT, TEMPORARY, INTERNSHIP, CONSULTANT. */
    @Column(length = 30)
    private String employmentCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}
