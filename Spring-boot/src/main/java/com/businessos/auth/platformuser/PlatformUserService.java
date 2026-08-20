package com.businessos.auth.platformuser;

import com.businessos.auth.role.enums.Role;
import com.businessos.auth.user.User;
import com.businessos.auth.user.UserMapper;
import com.businessos.auth.user.UserRepository;
import com.businessos.auth.user.UserResponse;
import com.businessos.enums.AuditAction;
import com.businessos.enums.AuditEntityType;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.audit.AuditService;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PlatformUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SecurityUtil securityUtil;

    // Same set of roles as User.isPlatformUser() - kept here since PlatformUserService
    // is the only place that needs it as a query-able list rather than a boolean check.
    private static final List<Role> PLATFORM_ROLES = List.of(
        Role.SUPER_ADMIN, Role.SYSTEM_ADMIN, Role.SUPPORT_AGENT,
        Role.SUPPORT_MANAGER, Role.MARKETING_MANAGER,
        Role.PLATFORM_ACCOUNTANT, Role.SALES_MANAGER
    );

    public UserResponse createPlatformUser(PlatformUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        if (!PLATFORM_ROLES.contains(request.getRole())) {
            throw new BadRequestException("Invalid platform role selected");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        user.setActive(true);
        user.setEmailVerified(true);

        user = userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findByRoleIn(PLATFORM_ROLES, pageable).map(UserMapper::toResponse);
    }

    public UserResponse getById(Long id) {
        return UserMapper.toResponse(getPlatformUserOrThrow(id));
    }

    public UserResponse update(Long id, PlatformUserRequest request) {
        User user = getPlatformUserOrThrow(id);

        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new BadRequestException("First name is required");
        }
        if (request.getLastName() == null || request.getLastName().isBlank()) {
            throw new BadRequestException("Last name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (request.getRole() == null || !PLATFORM_ROLES.contains(request.getRole())) {
            throw new BadRequestException("Invalid platform role selected");
        }
        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        Role oldRole = user.getRole();
        boolean passwordReset = request.getPassword() != null && !request.getPassword().isBlank();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        if (passwordReset) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User saved = userRepository.save(user);

        // These two are among the most sensitive actions an admin can take on
        // another account - promoting someone to SUPER_ADMIN, or setting their
        // password without them asking - and neither ever left a trace before.
        User actor = securityUtil.getCurrentUser();
        if (oldRole != request.getRole()) {
            auditService.log(AuditEntityType.USER, saved.getId(), AuditAction.PERMISSION_CHANGE,
                    oldRole != null ? oldRole.name() : null, request.getRole().name(), actor, null, null);
        }
        if (passwordReset) {
            auditService.log(AuditEntityType.USER, saved.getId(), AuditAction.PASSWORD_CHANGE,
                    null, null, actor, null, null);
        }

        return UserMapper.toResponse(saved);
    }

    public void deactivate(Long id) {
        User user = getPlatformUserOrThrow(id);
        user.setActive(false);
        userRepository.save(user);
        auditService.log(AuditEntityType.USER, user.getId(), AuditAction.UPDATE,
                "active", "inactive", securityUtil.getCurrentUser(), null, null);
    }

    private User getPlatformUserOrThrow(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Platform user not found"));
        if (!user.isPlatformUser()) {
            throw new ResourceNotFoundException("Platform user not found");
        }
        return user;
    }
}
