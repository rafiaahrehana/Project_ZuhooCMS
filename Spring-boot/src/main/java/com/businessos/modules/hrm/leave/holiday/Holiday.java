package com.businessos.modules.hrm.leave.holiday;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import com.businessos.enums.HolidayType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "holidays")
@Getter @Setter @NoArgsConstructor
public class Holiday extends BaseEntity {


    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HolidayType type = HolidayType.COMPANY;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;
}
