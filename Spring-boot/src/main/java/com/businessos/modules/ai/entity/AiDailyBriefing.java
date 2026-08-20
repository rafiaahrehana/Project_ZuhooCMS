package com.businessos.modules.ai.entity;

import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDate;

/**
 * One cached daily briefing per (company, user, date) - lazily built the
 * first time the employee opens the assistant that day, so nothing pays for
 * an AI generation on a day the app is never opened.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "ai_daily_briefings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "user_id", "briefing_date"})
)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
public class AiDailyBriefing extends BaseEntity {

    @Column(name = "briefing_date", nullable = false)
    private LocalDate briefingDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
