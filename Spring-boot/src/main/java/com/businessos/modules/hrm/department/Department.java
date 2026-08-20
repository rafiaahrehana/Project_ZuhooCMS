package com.businessos.modules.hrm.department;
import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "departments", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "code"}))
@Getter @Setter @NoArgsConstructor
@AllArgsConstructor @Builder
public class Department extends BaseEntity {

    @Column(length = 20)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal budget;
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Department head
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_department_id")
    private Department department;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Employee> employees = new ArrayList<>();
}
