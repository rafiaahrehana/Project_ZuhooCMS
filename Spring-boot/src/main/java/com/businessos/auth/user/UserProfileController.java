package com.businessos.auth.user;

import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.shared.address.Address;
import com.businessos.shared.address.AddressMapper;
import com.businessos.shared.address.AddressRequest;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.businessos.modules.hrm.employee.EmployeeRepository;
import java.util.List;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;
    private final AuthorizationService authorizationService;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * The current user's fully-resolved permission set (COMPANY_OWNER gets every
     * code, a custom-role employee gets their role's assigned codes). Drives
     * permission-based frontend UI (dashboard widgets, sidebar) - the frontend filter
     * is a UX convenience only, every endpoint still enforces its own permission
     * check server-side regardless of what this returns.
     */
    @GetMapping("/permissions")
    public ResponseEntity<List<String>> getMyPermissions() {
        return ResponseEntity.ok(authorizationService.getMyPermissionCodes());
    }

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public ResponseEntity<UserProfileResponse> getProfile() {
        User principal = securityUtil.getCurrentUser();
        if (principal == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ResponseEntity.ok(mapToResponse(user));
    }

    @PatchMapping("/profile")
    @Transactional
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UserProfileRequest request) {
        User principal = securityUtil.getCurrentUser();
        if (principal == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                    throw new BadRequestException("Enter your current password to change your email.");
                }
                if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                    throw new BadRequestException("Current password is incorrect.");
                }
                if (userRepository.existsByEmail(newEmail)) {
                    throw new BadRequestException("An account with this email already exists.");
                }
                user.setEmail(newEmail);
            }
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getImage() != null) {
            user.setImage(request.getImage().trim());
        }
        if (request.getLanguagePreference() != null) {
            user.setLanguagePreference(request.getLanguagePreference().trim());
        }

        // Handle Address update
        if (request.getLocation() != null) {
            AddressRequest locReq = request.getLocation();
            if (user.getLocation() == null) {
                Address newLoc = addressMapper.toEntity(locReq);
                user.setLocation(newLoc);
            } else {
                addressMapper.updateEntityFromRequest(user.getLocation(), locReq);
            }
        }

        // Handle Company Email update
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId != null && request.getCompanyEmail() != null) {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
            company.setCompanyEmail(request.getCompanyEmail().trim());
            companyRepository.save(company);
        }

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(mapToResponse(savedUser));
    }

    private UserProfileResponse mapToResponse(User user) {
        String companyEmail = null;
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId != null) {
            companyEmail = companyRepository.findById(companyId)
                    .map(Company::getCompanyEmail)
                    .orElse(null);
        }
        // Fallback: if the user's own image field is blank, try their employee profileImageUrl.
        // This handles employees created before the two fields were kept in sync.
        String imageUrl = user.getImage();
        if ((imageUrl == null || imageUrl.isBlank()) && companyId != null) {
            imageUrl = employeeRepository.findByUserId(user.getId())
                    .map(emp -> emp.getProfileImageUrl())
                    .orElse(null);
        }
        return UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .image(imageUrl)
                .role(user.getRole())
                .languagePreference(user.getLanguagePreference())
                .location(addressMapper.toResponse(user.getLocation()))
                .companyEmail(companyEmail)
                .build();
    }
}
