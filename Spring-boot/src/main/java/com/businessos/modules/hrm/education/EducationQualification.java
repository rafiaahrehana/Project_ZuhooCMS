package com.businessos.modules.hrm.education;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "education_qualifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EducationQualification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 100)
    private String degree;

    @Column(nullable = false, length = 200)
    private String institution;

    @Column(name = "field_of_study", length = 150)
    private String fieldOfStudy;

    @Column(name = "passing_year")
    private Integer passingYear;

    @Column(length = 50)
    private String result;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
