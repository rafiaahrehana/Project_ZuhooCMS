package com.businessos.modules.itam.shared;

import com.businessos.auth.user.User;
import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.asset.Asset;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "asset_histories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    private LocalDate assignedAt;
    private LocalDate returnedAt;

    private String condition;
    private String conditionOnReturn;
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id")
    private User assignedBy;
}
