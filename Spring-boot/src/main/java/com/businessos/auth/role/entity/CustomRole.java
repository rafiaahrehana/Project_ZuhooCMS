package com.businessos.auth.role.entity;

import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "custom_roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"company_id", "name"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomRole extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean systemRole = false;

    /**
     * Inverse relationship: Users assigned to this custom role
     * Cascade DELETE is NOT set - users should have customRole set to NULL when role is deleted
     * Use SET_NULL in User.customRole mapping to handle this
     */
    @OneToMany(mappedBy = "customRole", fetch = FetchType.LAZY)
    private List<User> users;

}
