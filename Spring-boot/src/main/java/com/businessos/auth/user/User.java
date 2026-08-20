package com.businessos.auth.user;

import com.businessos.auth.role.entity.CustomRole;
import com.businessos.auth.role.enums.Role;
import com.businessos.core.base.BaseEntity;
import com.businessos.shared.address.Address;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity implements UserDetails {

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address location;

    public boolean isPlatformUser() {
        return role == Role.SUPER_ADMIN || role == Role.SYSTEM_ADMIN || 
               role == Role.SUPPORT_AGENT || role == Role.SUPPORT_MANAGER || 
               role == Role.MARKETING_MANAGER || role == Role.PLATFORM_ACCOUNTANT || 
               role == Role.SALES_MANAGER;
    }

    public boolean isTenantUser() {
        return !isPlatformUser();
    }

    @Builder.Default
    private boolean active = true;

    private String image;

    @Builder.Default
    private boolean emailVerified = false;

    /** 6-digit code shown in the "Verify your email" message; cleared once used. */
    @Column(length = 6)
    private String emailVerificationCode;

    private java.time.LocalDateTime emailVerificationCodeExpiresAt;

    /** 6-digit code shown in the "Reset your password" email; cleared once used. */
    @Column(length = 6)
    private String passwordResetCode;

    private java.time.LocalDateTime passwordResetCodeExpiresAt;

    @Builder.Default
    private String languagePreference = "EN";

    /**
     * When a CustomRole is deleted, this field must be explicitly set to NULL
     * in the service layer before deletion. CascadeType.SET_NULL is not a valid
     * JPA type and has been removed intentionally.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_role_id")
    private CustomRole customRole;

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public @NonNull String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !isDeleted();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
