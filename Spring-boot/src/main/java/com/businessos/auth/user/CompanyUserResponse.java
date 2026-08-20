package com.businessos.auth.user;

import com.businessos.auth.role.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * A user as seen from inside a company's admin screens.
 *
 * Deliberately not {@link UserResponse}: this one adds how the user is attached
 * to the company ({@code membership}) and their custom role name, and it never
 * carries anything the owner has no business seeing.
 */
@Data
@Builder
public class CompanyUserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String image;
    private Role role;
    private boolean active;
    private boolean emailVerified;

    /** Name of the assigned custom role, when the user has one. */
    private String customRoleName;

    /** How this user belongs to the company: OWNER, EMPLOYEE or CLIENT. */
    private String membership;

    private LocalDateTime createdAt;
}
