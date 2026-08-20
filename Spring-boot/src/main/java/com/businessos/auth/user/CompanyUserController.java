package com.businessos.auth.user;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The company's own user directory.
 *
 * <p>There is no {@code users.company_id} column - membership is derived, because
 * a User becomes part of a company in three different ways: they own it, they
 * have an Employee record in it, or they have a Client record in it. This
 * controller resolves those three sets and returns their union, which is why it
 * cannot simply page a repository query.
 *
 * <p>Consequence worth knowing: paging is applied in memory after the union.
 * That is fine at the scale of one company's staff and clients (hundreds), and
 * it is the honest trade for not denormalising a company id onto User. If a
 * tenant ever grows into the tens of thousands of users, this needs a real
 * projection query instead.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class CompanyUserController {

    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<CompanyUserResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String membership) {

        authorizationService.checkPermission(PermissionCode.USER_VIEW);

        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) {
            // Platform staff have no home tenant, so "this company's users" is
            // meaningless for them - they use the platform user screens instead.
            throw new UnauthorizedException("No company context for the current user");
        }

        // LinkedHashMap so a user who is both owner and employee is listed once,
        // keeping the first (strongest) membership rather than duplicating them.
        Map<Long, String> membershipByUserId = new LinkedHashMap<>();

        companyRepository.findById(companyId)
                .map(Company::getOwner)
                .filter(owner -> owner != null && !owner.isDeleted())
                .ifPresent(owner -> membershipByUserId.put(owner.getId(), "OWNER"));

        employeeRepository.findUserIdsByCompanyId(companyId)
                .forEach(id -> membershipByUserId.putIfAbsent(id, "EMPLOYEE"));

        clientRepository.findUserIdsByCompanyId(companyId)
                .forEach(id -> membershipByUserId.putIfAbsent(id, "CLIENT"));

        if (membershipByUserId.isEmpty()) {
            return ResponseEntity.ok(new PageImpl<>(List.of(), PageRequest.of(page, size), 0));
        }

        String needle = keyword == null ? null : keyword.trim().toLowerCase();

        List<CompanyUserResponse> all = new ArrayList<>();
        for (User user : userRepository.findAllById(membershipByUserId.keySet())) {
            if (user.isDeleted()) continue;

            String memberOf = membershipByUserId.get(user.getId());
            if (membership != null && !membership.isBlank() && !membership.equalsIgnoreCase(memberOf)) {
                continue;
            }
            if (needle != null && !needle.isEmpty() && !matches(user, needle)) {
                continue;
            }

            all.add(CompanyUserResponse.builder()
                    .id(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .image(user.getImage())
                    .role(user.getRole())
                    .active(user.isActive())
                    .emailVerified(user.isEmailVerified())
                    .customRoleName(user.getCustomRole() == null ? null : user.getCustomRole().getName())
                    .membership(memberOf)
                    .createdAt(user.getCreatedAt())
                    .build());
        }

        all.sort(Comparator
                .comparing((CompanyUserResponse u) -> membershipRank(u.getMembership()))
                .thenComparing(u -> (u.getFirstName() == null ? "" : u.getFirstName()).toLowerCase()));

        Pageable pageable = PageRequest.of(page, size);
        int from = Math.min((int) pageable.getOffset(), all.size());
        int to = Math.min(from + pageable.getPageSize(), all.size());

        return ResponseEntity.ok(new PageImpl<>(all.subList(from, to), pageable, all.size()));
    }

    private boolean matches(User user, String needle) {
        return contains(user.getFirstName(), needle)
                || contains(user.getLastName(), needle)
                || contains(user.getEmail(), needle)
                || contains(user.getPhone(), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    /** Owner first, then staff, then clients - the order an admin scans in. */
    private int membershipRank(String membership) {
        return switch (membership == null ? "" : membership) {
            case "OWNER" -> 0;
            case "EMPLOYEE" -> 1;
            default -> 2;
        };
    }
}
