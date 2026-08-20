package com.businessos.auth.authentication;

import com.businessos.auth.role.enums.Role;
import com.businessos.auth.token.TokenService;
import com.businessos.auth.token.TokenType;
import com.businessos.auth.token.UserToken;
import com.businessos.auth.token.TokenRepository;
import com.businessos.auth.user.User;
import com.businessos.auth.user.UserMapper;
import com.businessos.auth.user.UserRepository;
import com.businessos.auth.user.UserResponse;
import com.businessos.auth.password.ChangePasswordRequest;
import com.businessos.auth.password.ForgotPasswordRequest;
import com.businessos.auth.password.ResetPasswordRequest;
import com.businessos.auth.password.VerifyResetCodeRequest;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeNumberGenerator;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.enums.CompanyStatus;
import com.businessos.enums.EmploymentStatus;
import com.businessos.shared.audit.AuditService;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.company.RegisterRequest;
import com.businessos.shared.email.EmailService;
import com.businessos.security.JwtService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.exception.UnauthorizedException;
import com.businessos.auth.authentication.google.GoogleAuthRequest;
import com.businessos.auth.authentication.google.GoogleRegisterRequest;
import com.businessos.auth.authentication.google.GoogleSignInResponse;
import com.businessos.auth.authentication.google.GoogleTokenVerifier;
import com.businessos.modules.crm.client.ClientService;
import com.businessos.modules.crm.client.PublicClientRegisterRequest;
import com.businessos.shared.notification.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final long PASSWORD_RESET_MINS = 15;
    private static final int  EMAIL_VERIFY_CODE_MINUTES = 15;
    private static final java.security.SecureRandom CODE_RANDOM = new java.security.SecureRandom();

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;
    private final TokenRepository tokenRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final ClientService clientService;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final EmailService emailService;
    private final AuditService auditService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final SecurityUtil securityUtil;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Value("${app.trial-days:14}")
    private int trialDays;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with this email already exists");
        }
        if (companyRepository.existsBySubdomain(request.getSubdomain())) {
            throw new BadRequestException("This subdomain is already taken");
        }

        // Previously this set emailVerified(true) immediately, which made the
        // verifyEmail() guard below ("already verified") always fire and silently
        // skip the trial-activation/welcome-email flow it was meant to run - no
        // account ever actually completed verification through that path.
        String verificationCode = generateVerificationCode();
        User user = User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail().toLowerCase().trim())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(Role.COMPANY_OWNER)
            .active(true)
            .emailVerified(false)
            .emailVerificationCode(verificationCode)
            .emailVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(EMAIL_VERIFY_CODE_MINUTES))
            .build();
        userRepository.save(user);

        Company company = Company.builder()
            .companyName(request.getCompanyName())
            .subdomain(request.getSubdomain().toLowerCase().trim())
            .companyEmail(blankToNull(request.getCompanyEmail() != null ? request.getCompanyEmail().toLowerCase().trim() : null))
            .companyPhone(blankToNull(request.getCompanyPhone()))
            .locationDetail(request.getLocation())
            .subscriptionPlan("FREE")
            .status(CompanyStatus.TRIAL)
            .owner(user)
            .build();
        companyRepository.save(company);

        // Every module that scopes "my work" (leads, leaves, timesheets, expenses, payroll...)
        // looks up the current user's Employee record - without this, the owner immediately
        // hits "Employee profile not found" the moment they touch any of those screens.
        Employee ownerEmployee = Employee.builder()
            .user(user)
            .company(company)
            .employeeNumber(EmployeeNumberGenerator.next(employeeRepository, company.getId()))
            .jobTitle("Owner")
            .employmentStatus(EmploymentStatus.ACTIVE)
            .hireDate(LocalDate.now())
            .active(true)
            .build();
        employeeRepository.save(ownerEmployee);

        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), verificationCode);

        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long companyId = resolveCompanyId(user);

        if (user.isTenantUser() && companyId != null) {
            Company company = companyRepository.findById(companyId).orElse(null);
            if (company != null && (company.getStatus() == CompanyStatus.SUSPENDED
                    || company.getStatus() == CompanyStatus.DEACTIVATED)) {
                throw new UnauthorizedException(
                    "This company account has been " + company.getStatus().name().toLowerCase()
                        + ". Please contact support.");
            }
        }

        String accessToken  = jwtService.generateAccessToken(
            user.getEmail(), user.getRole().name(), companyId);
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        // Deliberately NOT revoking existing refresh tokens here - each login issues its
        // own independent refresh token so multiple concurrent sessions (a second tab,
        // another device) keep working. Only resetPassword()/changePassword() revoke
        // every refresh token, since those genuinely need to kill all other sessions.
        persistToken(user, refreshToken, TokenType.REFRESH,
            LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));

        // Extract IP synchronously on the main thread before passing to async audit
        auditService.logLogin(user, companyId, resolveClientIp());
        

        return new LoginResponse(user.getId(), user.getFirstName(), user.getEmail(),
            user.getRole(), companyId, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public JwtResponse refreshToken(RefreshTokenRequest request) {
        UserToken stored = tokenRepository
            .findByTokenAndType(request.getRefreshToken(), TokenType.REFRESH)
            .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new BadRequestException(
                "Refresh token has expired or been revoked. Please log in again.");
        }

        User user = stored.getUser();
        stored.setRevoked(true);

        Long companyId = resolveCompanyId(user);
        String newAccess  = jwtService.generateAccessToken(
            user.getEmail(), user.getRole().name(), companyId);
        String newRefresh = jwtService.generateRefreshToken(user.getEmail());

        persistToken(user, newRefresh, TokenType.REFRESH,
            LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));

        return new JwtResponse(newAccess, newRefresh);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        tokenRepository.findByTokenAndType(request.getRefreshToken(), TokenType.REFRESH)
            .ifPresent(token -> {
                Long companyId = resolveCompanyId(token.getUser());
                // Extract IP synchronously on the main thread before passing to async audit
                String clientIp = resolveClientIp();
                auditService.logLogout(token.getUser(), companyId, clientIp);
                token.setRevoked(true);
            });
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
            .orElseThrow(() -> new BadRequestException("Invalid or expired verification code"));

        if (user.isEmailVerified()) {
            throw new BadRequestException("This email is already verified");
        }

        if (user.getEmailVerificationCode() == null
                || !user.getEmailVerificationCode().equals(request.getCode().trim())
                || user.getEmailVerificationCodeExpiresAt() == null
                || user.getEmailVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired verification code");
        }

        user.setActive(true);
        user.setEmailVerified(true);
        user.setEmailVerificationCode(null);
        user.setEmailVerificationCodeExpiresAt(null);
        userRepository.save(user);

        companyRepository.findByOwnerId(user.getId()).ifPresent(company -> {
            company.setStatus(CompanyStatus.TRIAL);
            company.setSubscriptionStart(LocalDate.now());
            company.setSubscriptionEnd(LocalDate.now().plusDays(trialDays));
            companyRepository.save(company);
            notificationPreferenceService.createDefaultsForUser(user.getId());
            emailService.sendWelcomeCompanyEmail(user.getEmail(), user.getFirstName(), company.getCompanyName());
            
        });
    }

    @Override
    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        // Use ifPresent (not orElseThrow) to prevent user enumeration:
        // the response is always 200 OK regardless of whether the email exists.
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                String newCode = generateVerificationCode();
                user.setEmailVerificationCode(newCode);
                user.setEmailVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(EMAIL_VERIFY_CODE_MINUTES));
                userRepository.save(user);
                emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), newCode);
            }
        });
    }

    private String generateVerificationCode() {
        return String.format("%06d", CODE_RANDOM.nextInt(1_000_000));
    }

    // companyEmail/companyPhone are optional but @Column(unique = true) - an empty
    // string (the Angular form's default value when the field is left blank) is a
    // real value to a unique constraint, so every registration after the first one
    // that skips these fields would 409. Blank must become null.
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String resetCode = generateVerificationCode();
            user.setPasswordResetCode(resetCode);
            user.setPasswordResetCodeExpiresAt(LocalDateTime.now().plusMinutes(PASSWORD_RESET_MINS));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetCode);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyResetCode(VerifyResetCodeRequest request) {
        resolveUserFromResetCode(request.getEmail(), request.getCode());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        User user = (request.getToken() != null && !request.getToken().isBlank())
            ? resolveUserFromResetToken(request.getToken())
            : resolveUserFromResetCode(request.getEmail(), request.getCode());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetCode(null);
        user.setPasswordResetCodeExpiresAt(null);
        userRepository.save(user);

        revokeAllRefreshTokens(user);

    }

    // The long-lived JWT link path - only ClientServiceImpl.invite() still issues
    // these tokens (forgotPassword() below issues a code instead), for setting an
    // initial portal password from an emailed "Set Your Password" link.
    private User resolveUserFromResetToken(String tokenStr) {
        if (!jwtService.isTokenValid(tokenStr) || jwtService.extractActionType(tokenStr) != TokenType.PASSWORD_RESET) {
            throw new BadRequestException("Invalid or expired reset link");
        }
        String email = jwtService.extractEmail(tokenStr);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new BadRequestException("User not found for this token"));
    }

    // The numeric-code path used by the "forgot password" flow. Same deliberately
    // generic error whether the user doesn't exist, the code doesn't match, or it
    // expired - avoids leaking which emails are registered (mirrors verifyEmail()).
    private User resolveUserFromResetCode(String email, String code) {
        if (email == null || code == null) {
            throw new BadRequestException("Invalid or expired reset code");
        }
        User user = userRepository.findByEmail(email.toLowerCase().trim())
            .orElseThrow(() -> new BadRequestException("Invalid or expired reset code"));

        if (user.getPasswordResetCode() == null
                || !user.getPasswordResetCode().equals(code)
                || user.getPasswordResetCodeExpiresAt() == null
                || user.getPasswordResetCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired reset code");
        }
        return user;
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User principal = securityUtil.getCurrentUser();
        if (principal == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens so any other active session is logged out,
        // consistent with resetPassword. The current session's access token stays
        // valid until it expires; the frontend should redirect to login after success.
        revokeAllRefreshTokens(user);
    }


    private Long resolveCompanyId(User user) {
        return switch (user.getRole()) {
            // Platform staff have no home tenant - their token carries no companyId at all.
            // (Previously hardcoded to the seeded "BusinessOS HQ" company, which made every
            // platform login behave like a login into that tenant - see PLATFORM_ADMIN identity fix.)
            case SUPER_ADMIN, SYSTEM_ADMIN, SUPPORT_AGENT, SUPPORT_MANAGER, MARKETING_MANAGER, PLATFORM_ACCOUNTANT, SALES_MANAGER -> null;
            case COMPANY_OWNER -> companyRepository.findByOwnerId(user.getId())
                .map(Company::getId).orElse(null);
            case EMPLOYEE -> employeeRepository.findCompanyIdByUserId(user.getId())
                .orElse(null);
            case CLIENT -> clientRepository.findCompanyIdByUserId(user.getId())
                .orElse(null);
        };
    }


    // Resolves the client IP address synchronously on the main request thread.
    // Must NEVER be called from inside an @Async method — pass the result as a parameter instead.

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ── Google sign-in ────────────────────────────────────────────

    @Override
    @Transactional
    public GoogleSignInResponse googleSignIn(GoogleAuthRequest request) {

        GoogleTokenVerifier.GoogleIdentity identity = googleTokenVerifier.verify(request.getIdToken());

        User user = userRepository.findByEmail(identity.email()).orElse(null);

        // No account with this email. The app collects a company and calls googleRegister();
        // we cannot create the user here because a Google token carries no tenant.
        if (user == null) {
            return GoogleSignInResponse.needsRegistration(
                    identity.email(), identity.firstName(), identity.lastName());
        }

        return GoogleSignInResponse.loggedIn(issueSession(user));
    }

    @Override
    @Transactional
    public LoginResponse googleRegister(GoogleRegisterRequest request) {

        // Verified again on purpose — the earlier googleSignIn() call proves nothing about this
        // request, so the email is taken from this token, not carried over by the client.
        GoogleTokenVerifier.GoogleIdentity identity = googleTokenVerifier.verify(request.getIdToken());

        if (userRepository.existsByEmail(identity.email())) {
            throw new BadRequestException(
                    "An account with this email already exists. Sign in instead.");
        }

        // Reuse the ordinary public client registration so the company-active check, welcome
        // email and notification defaults all behave identically to an email/password signup.
        PublicClientRegisterRequest registration = new PublicClientRegisterRequest();
        registration.setFirstName(identity.firstName());
        registration.setLastName(identity.lastName());
        registration.setEmail(identity.email());
        // The account is signed into with Google, so this password is never used. A random one
        // is stored rather than leaving it null, and "forgot password" is the route to setting a
        // real one if they ever want to log in without Google.
        registration.setPassword(UUID.randomUUID() + "Aa1!");
        registration.setPhone(request.getPhone());
        registration.setCompanyId(request.getCompanyId());
        registration.setClientCompanyName(request.getClientCompanyName());
        registration.setIndustry(request.getIndustry());
        registration.setWebsite(request.getWebsite());

        clientService.registerPublic(registration);

        User user = userRepository.findByEmail(identity.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found after registration"));

        return issueSession(user);
    }

    /**
     * The tail end of login(): resolve the tenant, refuse suspended companies, mint and persist
     * the token pair. Shared so a Google sign-in produces exactly the same session as a password
     * one — same claims, same expiry, same refresh behaviour.
     */
    private LoginResponse issueSession(User user) {

        Long companyId = resolveCompanyId(user);

        if (user.isTenantUser() && companyId != null) {
            Company company = companyRepository.findById(companyId).orElse(null);
            if (company != null && (company.getStatus() == CompanyStatus.SUSPENDED
                    || company.getStatus() == CompanyStatus.DEACTIVATED)) {
                throw new UnauthorizedException(
                    "This company account has been " + company.getStatus().name().toLowerCase()
                        + ". Please contact support.");
            }
        }

        String accessToken  = jwtService.generateAccessToken(
            user.getEmail(), user.getRole().name(), companyId);
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        persistToken(user, refreshToken, TokenType.REFRESH,
            LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));

        auditService.logLogin(user, companyId, resolveClientIp());

        return new LoginResponse(user.getId(), user.getFirstName(), user.getEmail(),
            user.getRole(), companyId, accessToken, refreshToken);
    }

    private void persistToken(User user, String value, TokenType type, LocalDateTime expiresAt) {
        tokenRepository.save(UserToken.builder()
            .token(value)
            .tokenType(type)
            .user(user)
            .expiresAt(expiresAt)
            .build());
    }

    private void revokeAllRefreshTokens(User user) {
        tokenRepository.revokeAllByUserIdAndType(user.getId(), TokenType.REFRESH);
    }
}
