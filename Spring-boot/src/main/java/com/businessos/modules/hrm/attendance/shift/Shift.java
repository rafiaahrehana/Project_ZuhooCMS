package com.businessos.modules.hrm.attendance.shift;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.attendance.attendance.ShiftType;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalTime;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ShiftType shiftType;

    private LocalTime startTime;
    private LocalTime endTime;

    @Builder.Default
    private Integer gracePeriodMinutes = 10;

    @Builder.Default
    private String weeklyOffDays = "FRI,SAT";

    @Builder.Default
    private boolean flexible = false;
    @Builder.Default
    private boolean nightShift = false;
    @Builder.Default
    private boolean active = true;

    private long workingMinutes;
    private String description;
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}
