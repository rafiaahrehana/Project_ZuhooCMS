package com.businessos.auth.user.config;

import com.businessos.auth.role.enums.Role;
import com.businessos.auth.user.User;
import com.businessos.auth.user.UserRepository;
import com.businessos.shared.exception.InternalServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.enums.CompanyStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User superAdmin = createAdminIfNotExists("superadmin@businessos.com", "Super", "Admin", Role.SUPER_ADMIN);
        createAdminIfNotExists("systemadmin@businessos.com", "System", "Admin", Role.SYSTEM_ADMIN);

        if (superAdmin != null) {
            createPlatformCompanyIfNotExists(superAdmin);
        }
    }

    private User createAdminIfNotExists(String email,
                                        String firstName,
                                        String lastName,
                                        Role role) {
        try {
            if (!userRepository.existsByEmail(email)) {
                User admin = User.builder()
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .password(passwordEncoder.encode("Password@123"))
                        .role(role)
                        .active(true)
                        .emailVerified(true)
                        .build();

                return userRepository.save(admin);
            }
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception ex) {
            throw new InternalServerException(
                    "Failed to initialize " + role + " account.",
                    ex
            );
        }
    }

    private void createPlatformCompanyIfNotExists(User owner) {
        try {
            if (!companyRepository.existsBySubdomain("admin")) {
                Company platformCompany = Company.builder()
                        .companyName("BusinessOS HQ")
                        .companyEmail("hq@businessos.com")
                        .subdomain("admin")
                        .website("businessos.com")
                        .owner(owner)
                        .status(CompanyStatus.ACTIVE)
                        .subscriptionPlan("ENTERPRISE")
                        .active(true)
                        .isPlatformTenant(true)
                        .build();

                companyRepository.save(platformCompany);
                log.info("Successfully seeded BusinessOS Platform Company!");
            }
        } catch (Exception ex) {
            log.error("Failed to seed platform company", ex);
        }
    }
}
