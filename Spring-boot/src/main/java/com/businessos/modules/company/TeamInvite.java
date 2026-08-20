package com.businessos.modules.company;

import com.businessos.auth.role.enums.Role;
import com.businessos.core.base.BaseEntity;
import com.businessos.auth.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "team_invites")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeamInvite extends BaseEntity {


    @Column(nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id", nullable = false)
    private User invitedBy;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean accepted = false;
    private LocalDateTime acceptedAt;
}
