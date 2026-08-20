
package com.businessos.modules.crm.client;

import com.businessos.auth.role.enums.Role;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.auth.user.User;
import com.businessos.enums.ClientStatus;
import com.businessos.enums.CompanyStatus;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.auth.user.UserRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.email.EmailBranding;
import com.businessos.shared.email.EmailService;
import com.businessos.shared.notification.NotificationPreferenceService;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.crm.contact.ClientContact;
import com.businessos.modules.crm.contact.ClientContactRepository;
import com.businessos.auth.token.TokenType;
import com.businessos.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationPreferenceService notificationPreferenceService;
    private final SecurityUtil securityUtil;
    private final EmailService emailService;
    private final EmailBranding emailBranding;
    private final CompanyRepository companyRepository;
    private final AuthorizationService authorizationService;
    private final com.businessos.modules.crm.tag.TagRepository tagRepository;
    private final com.businessos.modules.crm.duplicate.DuplicateDetectionService duplicateDetectionService;
    // Portal invites: the contact holds the email, JwtService mints the one-time
    // set-password token.
    private final ClientContactRepository clientContactRepository;
    private final JwtService jwtService;
    private final com.businessos.modules.crm.opportunity.OpportunityRepository opportunityRepository;

    @Override
    @Transactional
    public ClientResponse create(CreateClientRequest request) {
        authorizationService.checkPermission(PermissionCode.CLIENT_CREATE);
        Long companyId = requireCompanyId();

        // Checked before saving, against existing clients only - the new row doesn't
        // exist yet, so this can't match itself.
        com.businessos.modules.crm.duplicate.DuplicateMatch possibleDuplicate = duplicateDetectionService
                .findPossibleDuplicateClient(request.getClientCompanyName(), request.getEmail(), request.getPhone())
                .orElse(null);

        boolean provisionLogin = Boolean.TRUE.equals(request.getProvisionPortalLogin());
        User user = null;

        if (provisionLogin) {
            if (request.getFirstName() == null || request.getFirstName().isBlank()
                    || request.getLastName() == null || request.getLastName().isBlank()
                    || request.getEmail() == null || request.getEmail().isBlank()
                    || request.getPassword() == null || request.getPassword().isBlank()) {
                throw new BadRequestException(
                        "First name, last name, email and password are required to provision a portal login");
            }
            String normalizedEmail = request.getEmail().toLowerCase().trim();
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new BadRequestException("An account with this email already exists");
            }

            user = User.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .phone(request.getPhone())
                    .role(Role.CLIENT)
                    .active(true)
                    .emailVerified(true)
                    .build();
            userRepository.save(user);
        }

        Client client = Client.builder()
                .user(user)
                .company(companyRef(companyId))   // was: full companyRepository.findById()
                .clientCompanyName(request.getClientCompanyName())
                .industry(request.getIndustry())
                .website(request.getWebsite())
                .taxId(request.getTaxId())
                .billingAddress(request.getBillingAddress())
                .shippingAddress(request.getShippingAddress())
                .tags(request.getTags())
                .employeeCount(request.getEmployeeCount())
                .annualRevenue(request.getAnnualRevenue())
                .status(ClientStatus.ACTIVE)
                .build();

        if (request.getAccountManagerId() != null) {
            Employee am = employeeRepository
                    .findByIdAndCompanyId(request.getAccountManagerId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Account manager not found: " + request.getAccountManagerId()));
            client.setAccountManager(am);
        }

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            client.setTagEntities(tagRepository.findByIdInAndCompanyId(request.getTagIds(), companyId));
        }

        clientRepository.save(client);

        if (user != null) {
            notificationPreferenceService.createDefaultsForUser(user.getId());
            try {
                Company fullCompany = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
                EmailBranding.Data branding = emailBranding.from(fullCompany);
                emailService.sendClientWelcomeEmail(user.getEmail(), user.getFirstName(), branding);
            } catch (Exception ex) {
                log.warn("Welcome email failed for client {}: {}", user.getEmail(), ex.getMessage());
            }
        }

        ClientResponse response = ClientMapper.toResponse(client);
        response.setPossibleDuplicate(possibleDuplicate);
        return response;
    }

    @Override
    @Transactional
    public ClientResponse registerPublic(PublicClientRegisterRequest request) {
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + request.getCompanyId()));
        if (company.getStatus() != CompanyStatus.ACTIVE && company.getStatus() != CompanyStatus.TRIAL) {
            throw new BadRequestException("This company is not currently accepting client registrations");
        }

        String normalizedEmail = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("An account with this email already exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.CLIENT)
                .active(true)
                .emailVerified(true)
                .build();
        userRepository.save(user);

        Client client = Client.builder()
                .user(user)
                .company(companyRef(company.getId()))
                .clientCompanyName(request.getClientCompanyName())
                .industry(request.getIndustry())
                .website(request.getWebsite())
                .status(ClientStatus.ACTIVE)
                .build();
        clientRepository.save(client);
        notificationPreferenceService.createDefaultsForUser(user.getId());

        try {
            EmailBranding.Data branding = emailBranding.from(company);
            emailService.sendClientWelcomeEmail(user.getEmail(), user.getFirstName(), branding);
        } catch (Exception ex) {
            log.warn("Welcome email failed for client {}: {}", user.getEmail(), ex.getMessage());
        }

        return ClientMapper.toResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getById(Long id) {
        Client client = findInTenant(id);
        return ClientMapper.toResponse(client, primaryContactEmail(client), primaryContactPhone(client));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getMyProfile() {
        User user = securityUtil.getCurrentUser();
        Client client = clientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client profile not found"));
        return ClientMapper.toResponse(client);
    }

    @Override
    @Transactional
    public ClientResponse updateMyProfile(UpdateMyClientProfileRequest request) {
        User user = securityUtil.getCurrentUser();
        Client client = clientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client profile not found"));

        if (request.getClientCompanyName() != null) client.setClientCompanyName(request.getClientCompanyName());
        if (request.getIndustry() != null) client.setIndustry(request.getIndustry());
        if (request.getWebsite() != null) client.setWebsite(request.getWebsite());
        if (request.getBillingAddress() != null) client.setBillingAddress(request.getBillingAddress());
        if (request.getShippingAddress() != null) client.setShippingAddress(request.getShippingAddress());

        clientRepository.save(client);
        return ClientMapper.toResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientResponse> listAll(ClientStatus status, Long tagId, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.CLIENT_VIEW);
        Long companyId = requireCompanyId();
        Page<Client> page;
        if (tagId != null) {
            page = clientRepository.findByCompanyIdAndTagEntitiesId(companyId, tagId, pageable);
        } else if (status != null) {
            page = clientRepository.findByCompanyIdAndStatus(companyId, status, pageable);
        } else {
            page = clientRepository.findByCompanyId(companyId, pageable);
        }
        return page.map(ClientMapper::toResponse);
    }

    // Deliberately NOT gated by CLIENT_VIEW: this is the active-client picker consumed
    // by Invoices, Payment Receipts, and the CRM Pipeline board when attaching a client
    // to an unrelated record - users with INVOICE_VIEW/PAYMENT_RECEIPT_VIEW/OPPORTUNITY_VIEW
    // but not CLIENT_VIEW still need it to populate that dropdown.
    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> listActive() {
        return clientRepository.findByCompanyIdAndStatus(requireCompanyId(), ClientStatus.ACTIVE)
                .stream().map(ClientMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ClientResponse update(Long id, UpdateClientRequest request) {
        authorizationService.checkPermission(PermissionCode.CLIENT_UPDATE);
        Long companyId = requireCompanyId();
        Client client = findInTenant(id);

        if (request.getClientCompanyName()!= null) client.setClientCompanyName(request.getClientCompanyName());
        if (request.getIndustry()!= null) client.setIndustry(request.getIndustry());
        if (request.getWebsite()!= null) client.setWebsite(request.getWebsite());
        if (request.getTaxId()!= null) client.setTaxId(request.getTaxId());
        if (request.getStatus()!= null) client.setStatus(request.getStatus());
        if (request.getPortalAccessEnabled()!= null) client.setPortalAccessEnabled(request.getPortalAccessEnabled());
        if (request.getBillingAddress() != null) client.setBillingAddress(request.getBillingAddress());
        if (request.getShippingAddress() != null) client.setShippingAddress(request.getShippingAddress());
        if (request.getTags() != null) client.setTags(request.getTags());
        if (request.getEmployeeCount() != null) client.setEmployeeCount(request.getEmployeeCount());
        if (request.getAnnualRevenue() != null) client.setAnnualRevenue(request.getAnnualRevenue());

        if (request.getAccountManagerId() != null) {
            Employee am = employeeRepository
                    .findByIdAndCompanyId(request.getAccountManagerId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Account manager not found: " + request.getAccountManagerId()));
            client.setAccountManager(am);
        }

        if (request.getTagIds() != null) {
            client.setTagEntities(request.getTagIds().isEmpty()
                    ? new java.util.ArrayList<>()
                    : tagRepository.findByIdInAndCompanyId(request.getTagIds(), companyId));
        }

        clientRepository.save(client);
        return ClientMapper.toResponse(client);
    }

    /**
     * Gives a client a portal login without anyone choosing a password for them.
     *
     * The user row is created with a long random password that is never shown to
     * anybody - the only way in is the emailed set-password link. That avoids the
     * usual failure mode where staff invent a password and send it over WhatsApp,
     * or worse, reuse one across every client.
     *
     * Re-invitable on purpose: calling it again for a client who already has a
     * login just issues a fresh link, which is what you want when the first one
     * expired or never arrived.
     */
    @Override
    @Transactional
    public ClientResponse inviteToPortal(Long id) {
        authorizationService.checkPermission(PermissionCode.CLIENT_UPDATE);
        Client client = findInTenant(id);
        Long companyId = requireCompanyId();

        User user = client.getUser();

        if (user == null) {
            // The address comes from the client's primary contact - the only place
            // an email is actually stored for a client.
            String email = primaryContactEmail(client);
            if (email == null) {
                throw new BadRequestException(
                    "This client has no contact email. Add a contact with an email address first.");
            }

            String normalized = email.trim().toLowerCase();
            user = userRepository.findByEmail(normalized).orElse(null);

            if (user == null) {
                String contactName = primaryContactName(client);
                user = User.builder()
                        .firstName(firstWord(contactName))
                        .lastName(remainingWords(contactName))
                        .email(normalized)
                        .password(passwordEncoder.encode(UUID.randomUUID() + "-" + UUID.randomUUID()))
                        .role(Role.CLIENT)
                        .active(true)
                        // Clicking the emailed link proves they control the address,
                        // so a separate verification step would be redundant.
                        .emailVerified(true)
                        .build();
                userRepository.save(user);
            } else if (user.getRole() != Role.CLIENT) {
                throw new BadRequestException(
                    "An account already exists for " + normalized + " with a different role.");
            }

            client.setUser(user);
        }

        client.setPortalAccessEnabled(true);
        clientRepository.save(client);

        // 7 days, not the 15-minute reset window: an invite often sits unopened
        // over a weekend, and a dead link means a support request.
        String token = jwtService.generateActionToken(
                user.getEmail(), TokenType.PASSWORD_RESET, 7L * 24 * 60 * 60 * 1000);

        // The login is provisioned regardless of whether the email gets through -
        // rolling it back would leave the client half-created, and a failed send is
        // recoverable by clicking invite again. But the caller is told which
        // happened, so "invite sent" never appears when nothing was delivered.
        boolean emailSent = false;
        String emailError = null;

        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            emailError = "Company record not found, so no invite could be sent.";
            log.warn("Portal invite for client {} sent no email - company {} not found",
                    client.getId(), companyId);
        } else {
            try {
                emailService.sendClientPortalInviteEmail(
                        user.getEmail(), user.getFirstName(),
                        token, emailBranding.from(company));
                emailSent = true;
            } catch (Exception ex) {
                // sendInternal already recorded a FAILED row in email_logs with the
                // reason; this only surfaces it to the caller.
                emailError = "The portal login was created, but the invite email could not be delivered.";
                log.warn("Portal invite email failed for client {} ({}): {}",
                        client.getId(), user.getEmail(), ex.getMessage());
            }
        }

        ClientResponse response = ClientMapper.toResponse(client);
        response.setInviteEmailSent(emailSent);
        response.setInviteEmailError(emailError);
        return response;
    }

    private String primaryContactEmail(Client client) {
        return clientContactRepository
                .findFirstByClientIdAndCompanyIdAndPrimaryContactTrueAndDeletedFalseOrderByIdAsc(client.getId(), client.getCompany().getId())
                .map(ClientContact::getEmail)
                .filter(e -> e != null && !e.isBlank())
                .orElse(null);
    }

    private String primaryContactPhone(Client client) {
        return clientContactRepository
                .findFirstByClientIdAndCompanyIdAndPrimaryContactTrueAndDeletedFalseOrderByIdAsc(client.getId(), client.getCompany().getId())
                .map(ClientContact::getPhone)
                .filter(p -> p != null && !p.isBlank())
                .orElse(null);
    }

    private String primaryContactName(Client client) {
        return clientContactRepository
                .findFirstByClientIdAndCompanyIdAndPrimaryContactTrueAndDeletedFalseOrderByIdAsc(client.getId(), client.getCompany().getId())
                .map(ClientContact::getFullName)
                .filter(n -> n != null && !n.isBlank())
                .orElse(client.getClientCompanyName());
    }

    private String firstWord(String full) {
        if (full == null || full.isBlank()) return "Client";
        return full.trim().split("\\s+")[0];
    }

    private String remainingWords(String full) {
        if (full == null || full.isBlank()) return "";
        String[] parts = full.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "";
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.CLIENT_DELETE);
        Client client = findInTenant(id);

        // The client vanishes from every lookup instantly (soft-delete), but any
        // still-open Opportunity pointing at it survives - a rep can no longer
        // open the client to see its contacts for that deal. Block instead of
        // silently orphaning it.
        long openOpportunities = opportunityRepository.countByCompanyIdAndClientIdAndStageNotIn(
                client.getCompany().getId(), client.getId(),
                List.of(com.businessos.modules.crm.opportunity.OpportunityStage.WON,
                        com.businessos.modules.crm.opportunity.OpportunityStage.LOST));
        if (openOpportunities > 0) {
            throw new BadRequestException(
                    "Cannot delete this client: it has " + openOpportunities
                            + " open opportunity/opportunities. Close or reassign them first.");
        }

        client.softDelete();
        clientRepository.save(client);

        User user = client.getUser();
        if (user != null) {
            user.setActive(false);
            user.softDelete();
            userRepository.save(user);
        }
        
    }

    @Override
    @Transactional(readOnly = true)
    public long getClientCount() {
        return clientRepository.countByCompanyId(requireCompanyId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isClient(Long userId) {
        return clientRepository.existsByUserIdAndCompanyId(userId, requireCompanyId());
    }

    private Client findInTenant(Long id) {
        return clientRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company();
        c.setId(companyId);
        return c;
    }
}